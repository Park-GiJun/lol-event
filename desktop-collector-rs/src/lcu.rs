//! 라이엇 클라이언트(LCU) 연동.
//!
//! lockfile 에서 포트와 비밀번호를 읽어 `https://127.0.0.1:{port}` 로 붙는다.
//! 인증서가 자기서명이라 검증을 끄는데, 상대가 항상 로컬호스트이고 자격증명도
//! 로컬 파일에서 직접 읽으므로 중간자가 낄 자리가 없다.

use std::collections::HashMap;
use std::sync::{Mutex, OnceLock};

use base64::Engine;
use serde_json::Value;

use crate::net::lcu_http;

/// 라이브 클라이언트 데이터 API. 게임 중에만 열린다.
const LIVE_CLIENT_BASE: &str = "https://127.0.0.1:2999";

#[derive(Debug, Clone)]
pub struct Credentials {
    pub port: String,
    pub password: String,
}

#[derive(Debug, Clone, Default)]
pub struct LcuStatus {
    pub connected: bool,
    pub game_name: Option<String>,
    pub tag_line: Option<String>,
    pub puuid: Option<String>,
    pub reason: Option<String>,
}

impl LcuStatus {
    pub fn display_name(&self) -> String {
        match (&self.game_name, &self.tag_line) {
            (Some(g), Some(t)) if !t.is_empty() => format!("{g}#{t}"),
            (Some(g), _) => g.clone(),
            _ => "—".to_owned(),
        }
    }
}

#[derive(Debug, Clone, Default)]
pub struct ChampSelectSlot {
    pub cell_id: i64,
    pub summoner_id: Option<i64>,
    pub champion_id: i32,
    pub assigned_position: String,
    pub riot_id: String,
    pub summoner_name: String,
    pub is_me: bool,
}

#[derive(Debug, Clone, Default)]
pub struct BannedChamp {
    pub champion_id: i32,
    pub team: &'static str,
}

#[derive(Debug, Clone, Default)]
pub struct ChampSelectFull {
    pub my_team: Vec<ChampSelectSlot>,
    pub their_team: Vec<ChampSelectSlot>,
    pub bans: Vec<BannedChamp>,
    pub phase: String,
    pub timer: i64,
}

#[derive(Debug, Clone, Default)]
pub struct TeamMemberInfo {
    pub summoner_name: String,
    pub riot_id: String,
    pub is_me: bool,
}

#[derive(Debug, Clone, Default)]
pub struct CustomTeams {
    pub phase: String,
    pub blue_team: Vec<TeamMemberInfo>,
    pub red_team: Vec<TeamMemberInfo>,
}

#[derive(Debug, Clone, Default)]
pub struct RunePage {
    pub id: i64,
    pub name: String,
    pub primary_style_id: i32,
    pub sub_style_id: i32,
    pub selected_perk_ids: Vec<i32>,
    pub current: bool,
}

#[derive(Debug, Clone, Default)]
pub struct LiveClientPlayer {
    pub summoner_name: String,
    pub champion_name: String,
    pub team: String,
    pub level: i32,
    pub kills: i32,
    pub deaths: i32,
    pub assists: i32,
    pub creep_score: i32,
}

#[derive(Debug, Clone, Default)]
pub struct LiveClientEvent {
    pub event_name: String,
    pub event_time: f64,
    pub killer_name: Option<String>,
    pub assisters: Vec<String>,
}

#[derive(Debug, Clone, Default)]
pub struct LiveClientData {
    pub players: Vec<LiveClientPlayer>,
    pub events: Vec<LiveClientEvent>,
    pub game_time: f64,
}

/// 게임 중 팀 구성 (gameflow session 기준).
#[derive(Debug, Clone, Default)]
pub struct LiveParticipant {
    pub summoner_name: String,
    pub champion_id: i32,
    pub champion_name: Option<String>,
    pub team_id: i32,
}

fn lockfile_candidates() -> Vec<String> {
    let mut v = vec![
        "C:/Riot Games/League of Legends/lockfile".to_owned(),
        "C:/Program Files/Riot Games/League of Legends/lockfile".to_owned(),
        "C:/Program Files (x86)/Riot Games/League of Legends/lockfile".to_owned(),
    ];
    if let Ok(local) = std::env::var("LOCALAPPDATA") {
        v.push(format!("{local}/Riot Games/League of Legends/lockfile"));
    }
    v
}

pub fn find_lockfile() -> Option<String> {
    lockfile_candidates()
        .into_iter()
        .find(|p| std::path::Path::new(p).exists())
}

/// lockfile 형식: `name:pid:port:password:protocol`
pub fn parse_lockfile(path: &str) -> Option<Credentials> {
    let text = std::fs::read_to_string(path).ok()?;
    let parts: Vec<&str> = text.trim().split(':').collect();
    if parts.len() < 4 {
        return None;
    }
    Some(Credentials {
        port: parts[2].trim().to_owned(),
        password: parts[3].trim().to_owned(),
    })
}

pub fn credentials() -> Option<Credentials> {
    parse_lockfile(&find_lockfile()?)
}

fn auth_header(password: &str) -> String {
    let token = base64::engine::general_purpose::STANDARD.encode(format!("riot:{password}"));
    format!("Basic {token}")
}

pub async fn get(creds: &Credentials, endpoint: &str) -> Result<Value, String> {
    let url = format!("https://127.0.0.1:{}{endpoint}", creds.port);
    let resp = lcu_http()
        .get(&url)
        .header("Authorization", auth_header(&creds.password))
        .send()
        .await
        .map_err(|e| format!("LCU 요청 실패: {e}"))?;
    let text = resp.text().await.map_err(|e| e.to_string())?;
    serde_json::from_str(&text).map_err(|e| format!("LCU 응답 해석 실패: {e}"))
}

async fn post(creds: &Credentials, endpoint: &str, body: &Value) -> Result<bool, String> {
    let url = format!("https://127.0.0.1:{}{endpoint}", creds.port);
    let resp = lcu_http()
        .post(&url)
        .header("Authorization", auth_header(&creds.password))
        .json(body)
        .send()
        .await
        .map_err(|e| format!("LCU 요청 실패: {e}"))?;
    Ok(resp.status().is_success())
}

async fn delete(creds: &Credentials, endpoint: &str) -> Result<bool, String> {
    let url = format!("https://127.0.0.1:{}{endpoint}", creds.port);
    let resp = lcu_http()
        .delete(&url)
        .header("Authorization", auth_header(&creds.password))
        .send()
        .await
        .map_err(|e| format!("LCU 요청 실패: {e}"))?;
    Ok(resp.status().is_success())
}

// ── 소환사 정보 캐시 ─────────────────────────────
// summonerId 로 riotId 를 매번 조회하면 챔피언 선택 화면에서만 초당 열 번씩
// LCU 를 두드리게 된다. 한 번 받은 건 프로세스가 살아 있는 동안 재사용한다.

fn summoner_cache() -> &'static Mutex<HashMap<i64, (String, String)>> {
    static C: OnceLock<Mutex<HashMap<i64, (String, String)>>> = OnceLock::new();
    C.get_or_init(|| Mutex::new(HashMap::new()))
}

/// `(riotId, summonerName)`.
async fn summoner_info(creds: &Credentials, summoner_id: i64) -> Option<(String, String)> {
    let cached = summoner_cache()
        .lock()
        .ok()
        .and_then(|m| m.get(&summoner_id).cloned());

    match get(creds, &format!("/lol-summoner/v1/summoners/{summoner_id}")).await {
        Ok(v) => {
            let game_name = v["gameName"].as_str().unwrap_or("").to_owned();
            let tag_line = v["tagLine"].as_str().unwrap_or("").to_owned();
            let display_name = v["displayName"].as_str().unwrap_or(&game_name).to_owned();
            let riot_id = if game_name.is_empty() {
                display_name.clone()
            } else {
                format!("{game_name}#{tag_line}")
            };
            let name = if display_name.is_empty() {
                game_name
            } else {
                display_name
            };
            if riot_id.is_empty() {
                return cached;
            }
            let pair = (riot_id, name);
            if let Ok(mut m) = summoner_cache().lock() {
                m.insert(summoner_id, pair.clone());
            }
            Some(pair)
        }
        Err(_) => cached,
    }
}

// ── 상태 ────────────────────────────────────────

pub async fn status() -> LcuStatus {
    let Some(creds) = credentials() else {
        return LcuStatus {
            connected: false,
            reason: Some("롤 클라이언트를 실행해주세요".to_owned()),
            ..Default::default()
        };
    };
    match get(&creds, "/lol-summoner/v1/current-summoner").await {
        Ok(v) if v.get("puuid").is_some() || v.get("gameName").is_some() => LcuStatus {
            connected: true,
            game_name: v["gameName"]
                .as_str()
                .or_else(|| v["displayName"].as_str())
                .map(str::to_owned),
            tag_line: v["tagLine"].as_str().map(str::to_owned),
            puuid: v["puuid"].as_str().map(str::to_owned),
            reason: None,
        },
        _ => LcuStatus {
            connected: false,
            reason: Some("클라이언트 응답 없음 — 로그인 확인".to_owned()),
            ..Default::default()
        },
    }
}

pub async fn game_phase() -> Option<String> {
    let creds = credentials()?;
    let v = get(&creds, "/lol-gameflow/v1/gameflow-phase").await.ok()?;
    Some(v.as_str()?.trim().to_owned())
}

// ── 챔피언 선택 ──────────────────────────────────

pub async fn champ_select_full() -> Option<ChampSelectFull> {
    let creds = credentials()?;
    let session = get(&creds, "/lol-champ-select/v1/session").await.ok()?;
    if session.get("myTeam").is_none() {
        return None;
    }

    let local_cell = session["localPlayerCellId"].as_i64();
    let phase = session["timer"]["phase"].as_str().unwrap_or("").to_owned();
    let remaining = session["timer"]["adjustedTimeLeftInPhase"]
        .as_i64()
        .unwrap_or(0);

    let mut my_team = Vec::new();
    let mut their_team = Vec::new();
    for (key, target) in [("myTeam", &mut my_team), ("theirTeam", &mut their_team)] {
        let Some(arr) = session[key].as_array() else {
            continue;
        };
        for slot in arr {
            let summoner_id = slot["summonerId"].as_i64().filter(|v| *v > 0);
            let cell_id = slot["cellId"].as_i64().unwrap_or(0);
            let (riot_id, summoner_name) = match summoner_id {
                Some(id) => summoner_info(&creds, id).await.unwrap_or_default(),
                None => (String::new(), String::new()),
            };
            target.push(ChampSelectSlot {
                cell_id,
                summoner_id,
                champion_id: slot["championId"].as_i64().unwrap_or(0) as i32,
                assigned_position: slot["assignedPosition"].as_str().unwrap_or("").to_owned(),
                riot_id,
                summoner_name,
                is_me: Some(cell_id) == local_cell,
            });
        }
    }

    // 밴은 액션 순서대로 쌓인다. 앞 다섯 개가 블루, 나머지가 레드.
    let mut bans = Vec::new();
    if let Some(groups) = session["actions"].as_array() {
        for group in groups {
            let Some(actions) = group.as_array() else {
                continue;
            };
            for action in actions {
                if action["type"].as_str() == Some("ban")
                    && action["completed"].as_bool() == Some(true)
                {
                    let champ_id = action["championId"].as_i64().unwrap_or(0) as i32;
                    if champ_id > 0 {
                        let team = if bans.len() < 5 { "blue" } else { "red" };
                        bans.push(BannedChamp {
                            champion_id: champ_id,
                            team,
                        });
                    }
                }
            }
        }
    }

    Some(ChampSelectFull {
        my_team,
        their_team,
        bans,
        phase,
        timer: remaining / 1000,
    })
}

// ── 내전 팀 구성 ─────────────────────────────────

/// 로비 · 챔피언 선택 · 게임 중 어느 단계든 블루/레드 팀 명단을 뽑아낸다.
pub async fn custom_teams() -> Option<CustomTeams> {
    let creds = credentials()?;
    let phase = game_phase().await?;

    match phase.as_str() {
        "Lobby" => {
            let lobby = get(&creds, "/lol-lobby/v2/lobby").await.ok()?;
            let my_id = lobby["localMember"]["summonerId"].as_i64();
            let mut blue = Vec::new();
            let mut red = Vec::new();
            for (key, target) in [
                ("customTeam100", &mut blue),
                ("customTeam200", &mut red),
            ] {
                if let Some(arr) = lobby["gameConfig"][key].as_array() {
                    for m in arr {
                        let Some(sid) = m["summonerId"].as_i64() else {
                            continue;
                        };
                        if let Some((riot_id, name)) = summoner_info(&creds, sid).await {
                            target.push(TeamMemberInfo {
                                summoner_name: name,
                                riot_id,
                                is_me: Some(sid) == my_id,
                            });
                        }
                    }
                }
            }
            Some(CustomTeams {
                phase,
                blue_team: blue,
                red_team: red,
            })
        }
        "ChampSelect" => {
            let session = get(&creds, "/lol-gameflow/v1/session").await.ok()?;
            let queue_id = session["gameData"]["queue"]["id"].as_i64();
            // 내전(큐 0)이 아니면 팀 분석을 하지 않는다.
            if queue_id.is_some_and(|q| q != 0) {
                return Some(CustomTeams {
                    phase,
                    ..Default::default()
                });
            }
            let cs = get(&creds, "/lol-champ-select/v1/session").await.ok()?;
            let local_cell = cs["localPlayerCellId"].as_i64();
            let i_am_blue = local_cell.map(|c| c < 5).unwrap_or(true);

            let mut mine = Vec::new();
            let mut theirs = Vec::new();
            for (key, target) in [("myTeam", &mut mine), ("theirTeam", &mut theirs)] {
                if let Some(arr) = cs[key].as_array() {
                    for s in arr {
                        let Some(sid) = s["summonerId"].as_i64().filter(|v| *v > 0) else {
                            continue;
                        };
                        if let Some((riot_id, name)) = summoner_info(&creds, sid).await {
                            target.push(TeamMemberInfo {
                                summoner_name: name,
                                riot_id,
                                is_me: s["cellId"].as_i64() == local_cell,
                            });
                        }
                    }
                }
            }
            let (blue, red) = if i_am_blue {
                (mine, theirs)
            } else {
                (theirs, mine)
            };
            Some(CustomTeams {
                phase,
                blue_team: blue,
                red_team: red,
            })
        }
        "InProgress" => {
            let session = get(&creds, "/lol-gameflow/v1/session").await.ok()?;
            let queue_id = session["gameData"]["queue"]["id"].as_i64();
            if queue_id.is_some_and(|q| q != 0) {
                return Some(CustomTeams {
                    phase,
                    ..Default::default()
                });
            }
            let my_id = session["localPlayer"]["summonerId"].as_i64();
            let mut blue = Vec::new();
            let mut red = Vec::new();
            for (key, target) in [("teamOne", &mut blue), ("teamTwo", &mut red)] {
                if let Some(arr) = session["gameData"][key].as_array() {
                    for p in arr {
                        let Some(sid) = p["summonerId"].as_i64().filter(|v| *v > 0) else {
                            continue;
                        };
                        if let Some((riot_id, name)) = summoner_info(&creds, sid).await {
                            target.push(TeamMemberInfo {
                                summoner_name: name,
                                riot_id,
                                is_me: Some(sid) == my_id,
                            });
                        }
                    }
                }
            }
            Some(CustomTeams {
                phase,
                blue_team: blue,
                red_team: red,
            })
        }
        _ => Some(CustomTeams {
            phase,
            ..Default::default()
        }),
    }
}

/// 로비 멤버를 미리 조회해 캐시에 채운다.
/// 챔피언 선택에 들어가면 상대팀 summonerId 가 가려지므로, 그 전에 받아 둬야 한다.
pub async fn cache_lobby_members() {
    let Some(creds) = credentials() else { return };
    let Ok(lobby) = get(&creds, "/lol-lobby/v2/lobby").await else {
        return;
    };
    for key in ["customTeam100", "customTeam200"] {
        if let Some(arr) = lobby["gameConfig"][key].as_array() {
            for m in arr {
                if let Some(sid) = m["summonerId"].as_i64() {
                    let _ = summoner_info(&creds, sid).await;
                }
            }
        }
    }
}

// ── 게임 중 팀 ───────────────────────────────────

pub async fn live_teams() -> Option<(String, Vec<LiveParticipant>)> {
    let creds = credentials()?;
    let phase = game_phase().await?;
    if phase != "InProgress" {
        return Some((phase, Vec::new()));
    }
    let session = get(&creds, "/lol-gameflow/v1/session").await.ok()?;
    let mut out = Vec::new();
    for (key, team_id) in [("teamOne", 100), ("teamTwo", 200)] {
        if let Some(arr) = session["gameData"][key].as_array() {
            for p in arr {
                out.push(LiveParticipant {
                    summoner_name: p["summonerName"]
                        .as_str()
                        .or_else(|| p["gameName"].as_str())
                        .unwrap_or("???")
                        .to_owned(),
                    champion_id: p["championId"].as_i64().unwrap_or(0) as i32,
                    champion_name: p["championName"].as_str().map(str::to_owned),
                    team_id,
                });
            }
        }
    }
    Some((phase, out))
}

// ── 룬 ──────────────────────────────────────────

fn parse_rune_page(v: &Value) -> Option<RunePage> {
    Some(RunePage {
        id: v["id"].as_i64()?,
        name: v["name"].as_str().unwrap_or("").to_owned(),
        primary_style_id: v["primaryStyleId"].as_i64().unwrap_or(0) as i32,
        sub_style_id: v["subStyleId"].as_i64().unwrap_or(0) as i32,
        selected_perk_ids: v["selectedPerkIds"]
            .as_array()
            .map(|a| a.iter().filter_map(|x| x.as_i64()).map(|x| x as i32).collect())
            .unwrap_or_default(),
        current: v["current"].as_bool().unwrap_or(false),
    })
}

pub async fn rune_pages() -> Vec<RunePage> {
    let Some(creds) = credentials() else {
        return Vec::new();
    };
    let Ok(v) = get(&creds, "/lol-perks/v1/pages").await else {
        return Vec::new();
    };
    v.as_array()
        .map(|a| a.iter().filter_map(parse_rune_page).collect())
        .unwrap_or_default()
}

/// 편집 가능한 페이지 하나를 지우고 새 페이지를 만든다.
/// 라이엇은 페이지 수 상한이 있어서, 자리를 비우지 않으면 생성이 거절된다.
pub async fn apply_rune_page(
    name: String,
    primary_style_id: i32,
    sub_style_id: i32,
    perk_ids: Vec<i32>,
) -> Result<(), String> {
    let creds = credentials().ok_or("롤 클라이언트가 실행 중이 아닙니다")?;

    let pages = rune_pages().await;
    // `*` 로 시작하는 건 라이엇 기본 프리셋이라 지울 수 없다.
    if let Some(editable) = pages
        .iter()
        .filter(|p| !p.name.starts_with('*') && p.id > 0)
        .next_back()
    {
        let _ = delete(&creds, &format!("/lol-perks/v1/pages/{}", editable.id)).await;
    }

    let body = serde_json::json!({
        "name": name,
        "primaryStyleId": primary_style_id,
        "subStyleId": sub_style_id,
        "selectedPerkIds": perk_ids,
    });
    if post(&creds, "/lol-perks/v1/pages", &body).await? {
        Ok(())
    } else {
        Err("룬 페이지 생성을 클라이언트가 거절했습니다".to_owned())
    }
}

// ── Live Client Data API (포트 2999) ─────────────

pub async fn live_client_data() -> Option<LiveClientData> {
    let resp = lcu_http()
        .get(format!("{LIVE_CLIENT_BASE}/liveclientdata/allgamedata"))
        .send()
        .await
        .ok()?;
    let text = resp.text().await.ok()?;
    let root: Value = serde_json::from_str(&text).ok()?;

    let players = root["allPlayers"]
        .as_array()
        .map(|arr| {
            arr.iter()
                .map(|p| LiveClientPlayer {
                    summoner_name: p["riotIdGameName"]
                        .as_str()
                        .or_else(|| p["summonerName"].as_str())
                        .unwrap_or("???")
                        .to_owned(),
                    champion_name: p["championName"].as_str().unwrap_or("").to_owned(),
                    team: p["team"].as_str().unwrap_or("").to_owned(),
                    level: p["level"].as_i64().unwrap_or(0) as i32,
                    kills: p["scores"]["kills"].as_i64().unwrap_or(0) as i32,
                    deaths: p["scores"]["deaths"].as_i64().unwrap_or(0) as i32,
                    assists: p["scores"]["assists"].as_i64().unwrap_or(0) as i32,
                    creep_score: p["scores"]["creepScore"].as_i64().unwrap_or(0) as i32,
                })
                .collect()
        })
        .unwrap_or_default();

    let events = root["events"]["Events"]
        .as_array()
        .map(|arr| {
            arr.iter()
                .map(|e| LiveClientEvent {
                    event_name: e["EventName"].as_str().unwrap_or("").to_owned(),
                    event_time: e["EventTime"].as_f64().unwrap_or(0.0),
                    killer_name: e["KillerName"].as_str().map(str::to_owned),
                    assisters: e["Assisters"]
                        .as_array()
                        .map(|a| {
                            a.iter()
                                .filter_map(|x| x.as_str())
                                .map(str::to_owned)
                                .collect()
                        })
                        .unwrap_or_default(),
                })
                .collect()
        })
        .unwrap_or_default();

    Some(LiveClientData {
        players,
        events,
        game_time: root["gameData"]["gameTime"].as_f64().unwrap_or(0.0),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_lockfile_format() {
        let dir = std::env::temp_dir().join("lol-collector-lockfile-test");
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("lockfile");
        std::fs::write(&path, "LeagueClient:12345:52123:aBcD-1234:https").unwrap();

        let creds = parse_lockfile(path.to_str().unwrap()).expect("파싱 실패");
        assert_eq!(creds.port, "52123");
        assert_eq!(creds.password, "aBcD-1234");

        std::fs::remove_dir_all(&dir).ok();
    }

    #[test]
    fn rejects_truncated_lockfile() {
        let dir = std::env::temp_dir().join("lol-collector-lockfile-bad");
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("lockfile");
        std::fs::write(&path, "LeagueClient:12345").unwrap();
        assert!(parse_lockfile(path.to_str().unwrap()).is_none());
        std::fs::remove_dir_all(&dir).ok();
    }

    #[test]
    fn auth_header_is_basic_riot() {
        // riot:secret -> cmlvdDpzZWNyZXQ=
        assert_eq!(auth_header("secret"), "Basic cmlvdDpzZWNyZXQ=");
    }
}
