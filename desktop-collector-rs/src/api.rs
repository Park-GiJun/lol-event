//! 백엔드 API 클라이언트.
//!
//! 경로는 웹 프론트엔드(`frontend/src/hooks/*`)가 부르는 것과 같게 맞췄다.
//! Kotlin 판이 쓰던 경로 중 여럿은 이미 서버에서 사라졌거나 이름이 바뀌어
//! 조용히 빈 화면을 만들고 있었다. 웹이 정본이다.

use serde::de::DeserializeOwned;
use serde_json::Value;

use crate::models::*;
use crate::net::http;

pub const BASE_URL: &str = "https://api.lol.gijun.net/api";

/// URL 경로/쿼리에 넣을 문자열을 퍼센트 인코딩한다.
/// riotId 에 `#` 과 한글이 들어가므로 반드시 거쳐야 한다.
pub fn encode(s: &str) -> String {
    let mut out = String::with_capacity(s.len() * 3);
    for b in s.as_bytes() {
        match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                out.push(*b as char)
            }
            _ => out.push_str(&format!("%{b:02X}")),
        }
    }
    out
}

/// `{ "data": ..., "message": ... }` 봉투를 벗겨 안쪽만 돌려준다.
async fn get_data<T: DeserializeOwned>(path: String) -> Result<T, String> {
    let url = format!("{BASE_URL}{path}");
    let resp = http()
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("요청 실패: {e}"))?;
    let status = resp.status();
    let body = resp.text().await.map_err(|e| format!("본문 읽기 실패: {e}"))?;

    let wrapper: ApiResponse<T> = serde_json::from_str(&body).map_err(|e| {
        if status.is_success() {
            format!("응답 해석 실패: {e}")
        } else {
            format!("서버 응답 {}", status.as_u16())
        }
    })?;
    wrapper.data.ok_or_else(|| {
        wrapper
            .message
            .unwrap_or_else(|| format!("서버 응답 {}", status.as_u16()))
    })
}

/// 데이터가 비어 있는 것과 서버가 죽은 것은 다르다. 전자는 빈 화면,
/// 후자는 에러 메시지를 띄워야 하므로 성공 응답의 null 만 기본값으로 바꾼다.
async fn get_or_default<T: DeserializeOwned + Default>(path: String) -> Result<T, String> {
    match get_data::<Option<T>>(path).await {
        Ok(v) => Ok(v.unwrap_or_default()),
        Err(e) => Err(e),
    }
}

// ── 목록 · 플레이어 ───────────────────────────────

pub async fn stats_list(mode: &str) -> Result<StatsListResult, String> {
    get_or_default(format!("/stats?mode={mode}")).await
}

pub async fn player_stats(riot_id: String, mode: &str) -> Result<PlayerStats, String> {
    get_data(format!("/stats/player/{}?mode={mode}", encode(&riot_id))).await
}

/// 프로필 · 연승 · 챔피언 · 최근 경기를 한 번에. 소환사 화면이 쓴다.
pub async fn summoner(riot_id: String, mode: &str) -> Result<SummonerResult, String> {
    get_data(format!("/summoner/{}?mode={mode}", encode(&riot_id))).await
}

pub async fn elo_history(riot_id: String, limit: i32) -> Result<EloHistoryResult, String> {
    get_or_default(format!(
        "/stats/player/{}/elo-history?limit={limit}",
        encode(&riot_id)
    ))
    .await
}

// ── 경기 ─────────────────────────────────────────

pub async fn recent_matches(mode: &str, size: i32) -> Result<MatchPageResult, String> {
    get_or_default(format!("/matches/page?mode={mode}&page=0&size={size}")).await
}

pub async fn match_detail(match_id: String) -> Result<MatchDetail, String> {
    get_data(format!("/matches/{}", encode(&match_id))).await
}

// ── 통계 ─────────────────────────────────────────

pub async fn overview(mode: &str) -> Result<OverviewResult, String> {
    get_or_default(format!("/stats/overview?mode={mode}")).await
}

pub async fn elo_leaderboard() -> Result<EloLeaderboardResult, String> {
    get_or_default("/stats/elo".to_owned()).await
}

pub async fn awards(mode: &str) -> Result<AwardsResult, String> {
    get_or_default(format!("/stats/awards?mode={mode}")).await
}

pub async fn multikill(mode: &str) -> Result<MultikillResult, String> {
    get_or_default(format!("/stats/multikill-highlights?mode={mode}")).await
}

pub async fn mvp_ranking(mode: &str) -> Result<MvpRankingResult, String> {
    get_or_default(format!("/stats/mvp?mode={mode}")).await
}

pub async fn ban_analysis(mode: &str) -> Result<BanAnalysisResult, String> {
    get_or_default(format!("/stats/ban-analysis?mode={mode}")).await
}

pub async fn champion_tier(mode: &str, min_games: i32) -> Result<ChampionTierResult, String> {
    get_or_default(format!(
        "/stats/champion-tier?mode={mode}&minGames={min_games}"
    ))
    .await
}

pub async fn duo_synergy(mode: &str, min_games: i32) -> Result<DuoSynergyResult, String> {
    get_or_default(format!("/stats/duo?mode={mode}&minGames={min_games}")).await
}

pub async fn rival_matchup(mode: &str) -> Result<RivalMatchupResult, String> {
    get_or_default(format!("/stats/rival-matchup?mode={mode}")).await
}

pub async fn damage_analysis(mode: &str) -> Result<DamageAnalysisResult, String> {
    get_or_default(format!("/stats/damage-analysis?mode={mode}")).await
}

pub async fn vision_dominance(mode: &str) -> Result<VisionDominanceResult, String> {
    get_or_default(format!("/stats/vision-dominance?mode={mode}")).await
}

pub async fn surrender_analysis(mode: &str) -> Result<SurrenderAnalysisResult, String> {
    get_or_default(format!("/stats/surrender-analysis?mode={mode}")).await
}

pub async fn playstyle_dna(mode: &str) -> Result<PlaystyleDnaResult, String> {
    get_or_default(format!("/stats/playstyle-dna?mode={mode}")).await
}

pub async fn position_pool(mode: &str) -> Result<PositionPoolResult, String> {
    get_or_default(format!("/stats/position-champion-pool?mode={mode}")).await
}

pub async fn matchup(champion: String, mode: &str) -> Result<MatchupResult, String> {
    get_or_default(format!(
        "/stats/matchup?vsChampion={}&mode={mode}",
        encode(&champion)
    ))
    .await
}

// ── Riot 프로필 ──────────────────────────────────

pub async fn riot_profile(riot_id: String) -> Result<RiotProfile, String> {
    get_data(format!("/riot/profile/{}", encode(&riot_id))).await
}

pub async fn riot_profiles(
    riot_ids: Vec<String>,
) -> Result<std::collections::HashMap<String, RiotProfile>, String> {
    let body = serde_json::json!({ "riotIds": riot_ids });
    let resp = http()
        .post(format!("{BASE_URL}/riot/profiles/bulk"))
        .json(&body)
        .send()
        .await
        .map_err(|e| format!("요청 실패: {e}"))?;
    if !resp.status().is_success() {
        return Err(format!("서버 응답 {}", resp.status().as_u16()));
    }
    let text = resp.text().await.map_err(|e| e.to_string())?;
    let wrapper: ApiResponse<std::collections::HashMap<String, RiotProfile>> =
        serde_json::from_str(&text).map_err(|e| format!("응답 해석 실패: {e}"))?;
    Ok(wrapper.data.unwrap_or_default())
}

// ── 수집 업로드 ──────────────────────────────────

/// 수집한 매치를 서버로 올린다. `(저장, 중복스킵)` 을 돌려준다.
pub async fn post_matches(matches: Vec<Value>) -> Result<(i64, i64), String> {
    let body = serde_json::json!({ "matches": matches });
    let resp = http()
        .post(format!("{BASE_URL}/matches/bulk"))
        .json(&body)
        .send()
        .await
        .map_err(|e| format!("전송 실패: {e}"))?;
    let status = resp.status();
    let text = resp.text().await.map_err(|e| e.to_string())?;
    if !status.is_success() {
        return Err(format!("서버 응답 {}", status.as_u16()));
    }
    let v: Value = serde_json::from_str(&text).map_err(|e| format!("응답 해석 실패: {e}"))?;
    let data = &v["data"];
    Ok((
        data["saved"].as_i64().unwrap_or(0),
        data["skipped"].as_i64().unwrap_or(0),
    ))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn encodes_riot_id_for_url_path() {
        assert_eq!(encode("Dokyon"), "Dokyon");
        // '#' 과 공백, 한글은 전부 인코딩돼야 한다.
        assert_eq!(encode("a b#KR1"), "a%20b%23KR1");
        assert_eq!(encode("가"), "%EA%B0%80");
        // 비예약 문자는 그대로 둔다.
        assert_eq!(encode("a-b_c.d~e"), "a-b_c.d~e");
    }
}
