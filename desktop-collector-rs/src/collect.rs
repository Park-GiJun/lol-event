//! LCU 매치 히스토리에서 내전을 긁어 백엔드로 보낸다.
//!
//! 참가자 JSON 의 필드 구성은 백엔드 `/api/matches/bulk` 가 기대하는 그대로다.
//! 필드를 빼면 조용히 통계가 비므로 함부로 줄이지 않는다.

use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::OnceLock;
use std::time::Duration;

use serde_json::{json, Map, Value};

use crate::lcu;
use crate::net::http;

const MAX_GAMES: i32 = 500;
const PAGE: i32 = 20;
/// 한 번에 올릴 매치 수. 타임라인 원본까지 실어 보내므로 한 묶음이 1~2MB 를 넘지 않게 잡는다.
const UPLOAD_BATCH: usize = 20;
const SLEEP: Duration = Duration::from_millis(200);

/// 커스텀(0) · 5v5 내전(3130) · 칼바람 내전(3270).
const CUSTOM_QUEUE_IDS: [i64; 3] = [0, 3130, 3270];

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LogKind {
    Info,
    Progress,
    Warn,
    Error,
    Done,
}

#[derive(Debug, Clone)]
pub struct LogLine {
    pub kind: LogKind,
    pub message: String,
}

fn collecting() -> &'static AtomicBool {
    static B: OnceLock<AtomicBool> = OnceLock::new();
    B.get_or_init(|| AtomicBool::new(false))
}

pub fn is_collecting() -> bool {
    collecting().load(Ordering::SeqCst)
}

fn n(v: &Value, key: &str) -> i64 {
    v.get(key).and_then(Value::as_i64).unwrap_or(0)
}

fn b(v: &Value, key: &str) -> bool {
    v.get(key).and_then(Value::as_bool).unwrap_or(false)
}

fn s(v: &Value, key: &str) -> Option<String> {
    v.get(key).and_then(Value::as_str).map(str::to_owned)
}

/// championId → 챔피언 영문 키. Data Dragon 에서 한 번만 받아 캐시한다.
async fn champion_map() -> Result<std::collections::HashMap<String, String>, String> {
    static CACHE: OnceLock<std::sync::Mutex<Option<std::collections::HashMap<String, String>>>> =
        OnceLock::new();
    let cache = CACHE.get_or_init(|| std::sync::Mutex::new(None));
    if let Some(m) = cache.lock().unwrap_or_else(|e| e.into_inner()).clone() {
        return Ok(m);
    }

    let versions: Vec<String> = http()
        .get("https://ddragon.leagueoflegends.com/api/versions.json")
        .send()
        .await
        .map_err(|e| e.to_string())?
        .json()
        .await
        .map_err(|e| e.to_string())?;
    let latest = versions.first().ok_or("버전 목록이 비어 있습니다")?;

    let champs: Value = http()
        .get(format!(
            "https://ddragon.leagueoflegends.com/cdn/{latest}/data/ko_KR/champion.json"
        ))
        .send()
        .await
        .map_err(|e| e.to_string())?
        .json()
        .await
        .map_err(|e| e.to_string())?;

    let mut map = std::collections::HashMap::new();
    if let Some(data) = champs["data"].as_object() {
        for champ in data.values() {
            if let (Some(key), Some(id)) = (champ["key"].as_str(), champ["id"].as_str()) {
                map.insert(key.to_owned(), id.to_owned());
            }
        }
    }
    *cache.lock().unwrap_or_else(|e| e.into_inner()) = Some(map.clone());
    Ok(map)
}

fn champ_name(map: &std::collections::HashMap<String, String>, champion_id: i64) -> String {
    map.get(&champion_id.to_string())
        .cloned()
        .unwrap_or_else(|| format!("Champion_{champion_id}"))
}

/// epoch millis → `YYYY-MM-DD` (Asia/Seoul).
fn seoul_date(millis: i64) -> String {
    use chrono::{FixedOffset, TimeZone};
    let kst = FixedOffset::east_opt(9 * 3600).expect("고정 오프셋");
    match kst.timestamp_millis_opt(millis).single() {
        Some(dt) => dt.format("%Y-%m-%d").to_string(),
        None => String::new(),
    }
}

fn participant_json(
    p: &Value,
    identity: &Value,
    champs: &std::collections::HashMap<String, String>,
) -> Value {
    let stats = p.get("stats").cloned().unwrap_or(Value::Null);
    let timeline = p.get("timeline").cloned().unwrap_or(Value::Null);
    let champion_id = n(p, "championId");

    let game_name = identity.get("gameName").and_then(Value::as_str);
    let riot_id = match game_name {
        Some(g) => format!(
            "{g}#{}",
            identity.get("tagLine").and_then(Value::as_str).unwrap_or("")
        ),
        None => identity
            .get("summonerName")
            .and_then(Value::as_str)
            .unwrap_or("???")
            .to_owned(),
    };

    let mut o = Map::new();
    // 타임라인 participantFrames 의 키. 이게 없으면 프레임과 사람을 이을 수 없어서
    // 백엔드가 15분 기준 라인 판정을 포기하고 경기 종료 시점 누적값으로 내려간다.
    o.insert("participantId".into(), json!(n(p, "participantId")));
    o.insert(
        "puuid".into(),
        json!(identity.get("puuid").and_then(Value::as_str).unwrap_or("")),
    );
    o.insert("riotId".into(), json!(riot_id));
    o.insert("champion".into(), json!(champ_name(champs, champion_id)));
    o.insert("championId".into(), json!(champion_id));
    o.insert(
        "team".into(),
        json!(if n(p, "teamId") == 100 { "blue" } else { "red" }),
    );
    o.insert("teamId".into(), json!(n(p, "teamId")));
    o.insert("spell1Id".into(), json!(n(p, "spell1Id")));
    o.insert("spell2Id".into(), json!(n(p, "spell2Id")));
    o.insert("win".into(), json!(b(&stats, "win")));
    o.insert("kills".into(), json!(n(&stats, "kills")));
    o.insert("deaths".into(), json!(n(&stats, "deaths")));
    o.insert("assists".into(), json!(n(&stats, "assists")));
    o.insert(
        "damage".into(),
        json!(n(&stats, "totalDamageDealtToChampions")),
    );
    o.insert(
        "cs".into(),
        json!(n(&stats, "totalMinionsKilled") + n(&stats, "neutralMinionsKilled")),
    );
    o.insert("gold".into(), json!(n(&stats, "goldEarned")));

    for key in [
        "visionScore",
        "champLevel",
        "doubleKills",
        "tripleKills",
        "quadraKills",
        "pentaKills",
        "unrealKills",
        "killingSprees",
        "largestKillingSpree",
        "largestMultiKill",
        "largestCriticalStrike",
        "longestTimeSpentLiving",
        "inhibitorKills",
        "turretKills",
        "wardsKilled",
        "wardsPlaced",
        "sightWardsBoughtInGame",
        "visionWardsBoughtInGame",
        "perkPrimaryStyle",
        "perkSubStyle",
        "magicDamageDealt",
        "magicDamageDealtToChampions",
        "magicalDamageTaken",
        "physicalDamageDealt",
        "physicalDamageDealtToChampions",
        "physicalDamageTaken",
        "trueDamageDealt",
        "trueDamageDealtToChampions",
        "trueDamageTaken",
        "totalDamageDealt",
        "totalDamageDealtToChampions",
        "totalDamageTaken",
        "damageDealtToObjectives",
        "damageDealtToTurrets",
        "damageSelfMitigated",
        "totalHeal",
        "totalUnitsHealed",
        "timeCCingOthers",
        "totalTimeCrowdControlDealt",
        "neutralMinionsKilled",
        "neutralMinionsKilledTeamJungle",
        "neutralMinionsKilledEnemyJungle",
        "combatPlayerScore",
        "objectivePlayerScore",
        "totalPlayerScore",
        "totalScoreRank",
        "playerSubteamId",
        "subteamPlacement",
        "roleBoundItem",
    ] {
        o.insert(key.into(), json!(n(&stats, key)));
    }

    for key in [
        "firstBloodKill",
        "firstBloodAssist",
        "firstTowerKill",
        "firstTowerAssist",
        "firstInhibitorKill",
        "firstInhibitorAssist",
        "gameEndedInSurrender",
        "gameEndedInEarlySurrender",
        "causedEarlySurrender",
        "earlySurrenderAccomplice",
        "teamEarlySurrendered",
    ] {
        o.insert(key.into(), json!(b(&stats, key)));
    }

    for i in 0..=6 {
        o.insert(format!("item{i}"), json!(n(&stats, &format!("item{i}"))));
    }
    for i in 0..=5 {
        o.insert(format!("perk{i}"), json!(n(&stats, &format!("perk{i}"))));
        for v in 1..=3 {
            let key = format!("perk{i}Var{v}");
            o.insert(key.clone(), json!(n(&stats, &key)));
        }
    }
    for i in 1..=6 {
        let key = format!("playerAugment{i}");
        o.insert(key.clone(), json!(n(&stats, &key)));
    }

    o.insert(
        "lane".into(),
        s(&timeline, "lane").map_or(Value::Null, Value::String),
    );
    o.insert(
        "role".into(),
        s(&timeline, "role").map_or(Value::Null, Value::String),
    );

    Value::Object(o)
}

fn team_json(t: &Value, champs: &std::collections::HashMap<String, String>) -> Value {
    let bans: Vec<Value> = t["bans"]
        .as_array()
        .map(|arr| {
            arr.iter()
                .filter(|ban| n(ban, "championId") > 0)
                .map(|ban| {
                    let cid = n(ban, "championId");
                    json!({
                        "championId": cid,
                        "championName": champ_name(champs, cid),
                        "pickTurn": n(ban, "pickTurn"),
                    })
                })
                .collect()
        })
        .unwrap_or_default();

    json!({
        "teamId": n(t, "teamId"),
        "win": t.get("win").and_then(Value::as_str) == Some("Win"),
        "baronKills": n(t, "baronKills"),
        "dragonKills": n(t, "dragonKills"),
        "towerKills": n(t, "towerKills"),
        "inhibitorKills": n(t, "inhibitorKills"),
        "riftHeraldKills": n(t, "riftHeraldKills"),
        "hordeKills": n(t, "hordeKills"),
        "firstBlood": b(t, "firstBlood"),
        "firstTower": b(t, "firstTower"),
        "firstBaron": b(t, "firstBaron"),
        "firstInhibitor": b(t, "firstInhibitor"),
        // LCU 가 "firstDargon" 으로 보낸다. 라이엇 쪽 오탈자를 그대로 받아야 한다.
        "firstDragon": b(t, "firstDargon"),
        "bans": bans,
    })
}

/// 수집을 한 번 돌린다. 진행 상황은 `log` 로 흘려보낸다.
pub async fn run<F>(log: F)
where
    F: Fn(LogKind, String) + Send + Sync + 'static,
{
    if collecting().swap(true, Ordering::SeqCst) {
        log(LogKind::Error, "이미 수집 중입니다".to_owned());
        return;
    }
    let result = run_inner(&log).await;
    if let Err(e) = result {
        log(LogKind::Error, e);
    }
    collecting().store(false, Ordering::SeqCst);
}

async fn run_inner<F>(log: &F) -> Result<(), String>
where
    F: Fn(LogKind, String) + Send + Sync + 'static,
{
    let creds = lcu::credentials().ok_or("lockfile 없음 — 롤 클라이언트를 실행해주세요")?;

    let summoner = lcu::get(&creds, "/lol-summoner/v1/current-summoner")
        .await
        .map_err(|e| format!("LCU 연결 실패 — port:{} {e}", creds.port))?;
    log(
        LogKind::Info,
        format!(
            "클라이언트 연결 — {}#{}",
            summoner["gameName"].as_str().unwrap_or("?"),
            summoner["tagLine"].as_str().unwrap_or("?")
        ),
    );

    let champs = champion_map().await?;
    let mut new_matches: Vec<Value> = Vec::new();
    let mut seen_game_ids: std::collections::HashSet<i64> = std::collections::HashSet::new();
    let mut beg_index = 0;

    while beg_index < MAX_GAMES {
        tokio::time::sleep(SLEEP).await;
        let endpoint = format!(
            "/lol-match-history/v1/products/lol/current-summoner/matches?begIndex={beg_index}&endIndex={}",
            beg_index + PAGE - 1
        );
        let data = match lcu::get(&creds, &endpoint).await {
            Ok(v) => v,
            Err(e) => {
                log(LogKind::Warn, format!("페이지 {beg_index} 실패: {e}"));
                break;
            }
        };
        let Some(games) = data["games"]["games"].as_array() else {
            break;
        };
        if games.is_empty() {
            break;
        }

        // 히스토리 끝에 도달하면 라이엇이 같은 페이지를 계속 돌려준다.
        let first_id = n(&games[0], "gameId");
        if seen_game_ids.contains(&first_id) {
            log(
                LogKind::Info,
                format!("매치 히스토리 끝 ({beg_index}번째에서 중복 감지)"),
            );
            break;
        }
        for g in games {
            seen_game_ids.insert(n(g, "gameId"));
        }
        log(
            LogKind::Progress,
            format!(
                "{beg_index}~{}번 조회 — {}건",
                beg_index + games.len() as i32 - 1,
                games.len()
            ),
        );

        for game in games {
            let queue_id = n(game, "queueId");
            if !CUSTOM_QUEUE_IDS.contains(&queue_id) {
                continue;
            }
            let game_id = n(game, "gameId");
            let match_id = format!("KR_{game_id}");
            tokio::time::sleep(SLEEP).await;

            let detail = match lcu::get(&creds, &format!("/lol-match-history/v1/games/{game_id}"))
                .await
            {
                Ok(v) => v,
                Err(e) => {
                    log(LogKind::Warn, format!("{match_id} 상세 조회 실패 — {e}"));
                    continue;
                }
            };

            let mut identities: std::collections::HashMap<i64, Value> =
                std::collections::HashMap::new();
            if let Some(arr) = detail["participantIdentities"].as_array() {
                for id in arr {
                    identities.insert(
                        n(id, "participantId"),
                        id.get("player").cloned().unwrap_or(Value::Null),
                    );
                }
            }

            let participants: Vec<Value> = detail["participants"]
                .as_array()
                .map(|arr| {
                    arr.iter()
                        .map(|p| {
                            let identity = identities
                                .get(&n(p, "participantId"))
                                .cloned()
                                .unwrap_or(Value::Null);
                            participant_json(p, &identity, &champs)
                        })
                        .collect()
                })
                .unwrap_or_default();

            let teams: Vec<Value> = detail["teams"]
                .as_array()
                .map(|arr| arr.iter().map(|t| team_json(t, &champs)).collect())
                .unwrap_or_default();

            // 타임라인은 best-effort 다. 실패해도 경기 저장은 그대로 진행한다 —
            // 없으면 백엔드가 라인 판정을 경기 종료 시점 기준으로 내린다.
            //
            // 과거 경기 백필은 하지 않는다. LCU 매치 히스토리는 최근 경기만 보관하므로
            // 오래된 경기에는 애초에 타임라인이 없다.
            tokio::time::sleep(SLEEP).await;
            let timeline = match lcu::get(
                &creds,
                &format!("/lol-match-history/v1/game-timelines/{game_id}"),
            )
            .await
            {
                Ok(v) => v,
                Err(e) => {
                    log(
                        LogKind::Warn,
                        format!("{match_id} 타임라인 조회 실패 — {e} (경기는 그대로 저장)"),
                    );
                    Value::Null
                }
            };

            let game_creation = n(game, "gameCreation");
            new_matches.push(json!({
                "matchId": match_id,
                "queueId": queue_id,
                "gameCreation": game_creation,
                "gameDuration": n(game, "gameDuration"),
                "gameMode": s(&detail, "gameMode").map_or(Value::Null, Value::String),
                "gameType": s(&detail, "gameType").map_or(Value::Null, Value::String),
                "gameVersion": s(&detail, "gameVersion").map_or(Value::Null, Value::String),
                "mapId": n(&detail, "mapId"),
                "seasonId": n(&detail, "seasonId"),
                "platformId": s(&detail, "platformId").map_or(Value::Null, Value::String),
                "participants": participants,
                "teams": teams,
                // 원본 그대로 보낸다. 가공은 백엔드 몫이다.
                // 객체가 아니라 문자열로 싣는다 — 백엔드가 받은 바이트를 그대로 jsonb 에 넣는다.
                "timelineRaw": if timeline.is_null() { Value::Null } else { json!(timeline.to_string()) },
            }));

            log(
                LogKind::Info,
                format!("{match_id} 저장 ({})", seoul_date(game_creation)),
            );
        }

        if (games.len() as i32) < PAGE {
            break;
        }
        beg_index += PAGE;
    }

    if new_matches.is_empty() {
        log(LogKind::Done, "수집 완료 — 내전 0건".to_owned());
        return Ok(());
    }

    log(
        LogKind::Info,
        format!("서버 전송 중 ({}건)...", new_matches.len()),
    );

    // 나눠서 보낸다. 타임라인 원본이 경기당 60KB 급이라 500경기를 한 요청에 담으면 30MB 를 넘고,
    // 앞단 nginx 의 client_max_body_size 에 걸려 통째로 실패한다. 나눠 두면 한 묶음이 실패해도
    // 나머지는 들어가고, 재수집 때 중복 스킵으로 넘어간다.
    let total = new_matches.len();
    let mut saved_total = 0i64;
    let mut skipped_total = 0i64;
    let mut failed = 0usize;
    for batch in new_matches.chunks(UPLOAD_BATCH) {
        match crate::api::post_matches(batch.to_vec()).await {
            Ok((saved, skipped)) => {
                saved_total += saved;
                skipped_total += skipped;
            }
            Err(e) => {
                failed += batch.len();
                log(LogKind::Error, format!("서버 전송 실패 ({}건) — {e}", batch.len()));
            }
        }
    }

    let summary = format!("완료 — {saved_total}건 저장, {skipped_total}건 중복 스킵");
    if failed > 0 {
        log(
            LogKind::Error,
            format!("{summary} · {failed}건 전송 실패 (전체 {total}건)"),
        );
    } else {
        log(LogKind::Done, summary);
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn seoul_date_uses_kst() {
        // 2024-01-01T00:30:00Z -> KST 로는 09:30, 같은 날.
        assert_eq!(seoul_date(1_704_069_000_000), "2024-01-01");
        // 2023-12-31T16:00:00Z -> KST 01:00, 하루 넘어간다.
        assert_eq!(seoul_date(1_704_038_400_000), "2024-01-01");
        // 2023-12-31T14:00:00Z -> KST 23:00, 아직 전날.
        assert_eq!(seoul_date(1_704_031_200_000), "2023-12-31");
    }

    #[test]
    fn participant_keeps_riot_id_and_derived_fields() {
        let mut champs = std::collections::HashMap::new();
        champs.insert("266".to_owned(), "Aatrox".to_owned());

        let p = json!({
            "championId": 266,
            "teamId": 100,
            "participantId": 1,
            "stats": {
                "win": true,
                "kills": 7, "deaths": 2, "assists": 5,
                "totalDamageDealtToChampions": 24000,
                "totalMinionsKilled": 180, "neutralMinionsKilled": 20,
                "goldEarned": 13000,
            },
            "timeline": { "lane": "TOP", "role": "SOLO" },
        });
        let identity = json!({ "gameName": "아트록스장인", "tagLine": "KR1", "puuid": "abc" });

        let out = participant_json(&p, &identity, &champs);
        assert_eq!(out["riotId"], "아트록스장인#KR1");
        assert_eq!(out["champion"], "Aatrox");
        assert_eq!(out["team"], "blue");
        assert_eq!(out["cs"], 200);
        assert_eq!(out["win"], true);
        assert_eq!(out["lane"], "TOP");
        // 없는 통계는 0으로 채운다. null 이면 백엔드가 거절한다.
        assert_eq!(out["visionScore"], 0);
        assert_eq!(out["item0"], 0);
        assert_eq!(out["perk0Var3"], 0);
    }

    #[test]
    fn participant_falls_back_to_summoner_name() {
        let champs = std::collections::HashMap::new();
        let p = json!({ "championId": 999, "teamId": 200, "participantId": 6 });
        let identity = json!({ "summonerName": "옛날닉" });
        let out = participant_json(&p, &identity, &champs);
        assert_eq!(out["riotId"], "옛날닉");
        assert_eq!(out["champion"], "Champion_999");
        assert_eq!(out["team"], "red");
    }

    #[test]
    fn team_json_maps_riot_typo_for_first_dragon() {
        let champs = std::collections::HashMap::new();
        let t = json!({
            "teamId": 100,
            "win": "Win",
            "firstDargon": true,
            "bans": [ { "championId": 0, "pickTurn": 1 }, { "championId": 12, "pickTurn": 2 } ],
        });
        let out = team_json(&t, &champs);
        assert_eq!(out["win"], true);
        assert_eq!(out["firstDragon"], true);
        // championId 0 (밴 없음) 은 걸러진다.
        assert_eq!(out["bans"].as_array().unwrap().len(), 1);
        assert_eq!(out["bans"][0]["championId"], 12);
    }
}
