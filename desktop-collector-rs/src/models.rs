//! 백엔드(api.lol.gijun.net) 응답 모델.
//!
//! 형태는 웹 프론트엔드(`frontend/src/lib/types/stats.ts`)와 실제 응답을 대조해
//! 맞췄다. Kotlin 판 모델은 여러 엔드포인트에서 필드명이 어긋나 값이 통째로
//! 0으로 나오고 있었다 — 옮기면서 바로잡은 것들이다.
//!
//! 백엔드가 경로/필드를 정리하는 중이라, 이름이 바뀔 만한 자리에는
//! `#[serde(alias = ...)]` 로 옛 이름도 같이 받아 둔다. 어느 쪽이 와도 파싱된다.
//! 모르는 필드는 무시하고, 빠진 필드는 기본값으로 채운다.

use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct ApiResponse<T> {
    pub data: Option<T>,
    #[serde(default)]
    pub message: Option<String>,
}

/// 표본 신뢰 등급. 내전은 표본이 금방 한 자릿수로 떨어져서, 비율 지표는
/// 이 등급을 같이 보여주지 않으면 거짓말이 된다.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Deserialize)]
pub enum SampleGrade {
    #[serde(rename = "HIGH")]
    High,
    #[serde(rename = "MEDIUM")]
    Medium,
    #[serde(rename = "LOW")]
    Low,
    #[serde(rename = "INSUFFICIENT")]
    Insufficient,
    #[serde(other)]
    #[default]
    Unknown,
}

impl SampleGrade {
    /// 표본이 미덥지 못할 때만 화면에 배지를 단다.
    pub fn warns(self) -> bool {
        matches!(self, SampleGrade::Low | SampleGrade::Insufficient)
    }

    pub fn label(self) -> &'static str {
        match self {
            SampleGrade::High => "표본 충분",
            SampleGrade::Medium => "표본 보통",
            SampleGrade::Low => "표본 적음",
            SampleGrade::Insufficient => "표본 부족",
            SampleGrade::Unknown => "",
        }
    }
}

// ── 전체 통계 목록 (/stats) ───────────────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct StatsListResult {
    pub stats: Vec<StatsListEntry>,
    pub match_count: i32,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct StatsListEntry {
    pub riot_id: String,
    pub games: i32,
    pub wins: i32,
    pub losses: i32,
    pub win_rate: f64,
    pub kda: f64,
}

// ── 플레이어 상세 (/stats/player/{riotId}) ────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct PlayerStats {
    pub riot_id: String,
    pub games: i32,
    pub wins: i32,
    pub losses: i32,
    pub win_rate: f64,
    pub avg_kills: f64,
    pub avg_deaths: f64,
    pub avg_assists: f64,
    pub kda: f64,
    pub avg_damage: f64,
    pub avg_cs: f64,
    pub avg_gold: f64,
    pub avg_vision_score: f64,
    pub elo: Option<f64>,
    pub elo_rank: Option<i32>,
    pub champion_stats: Vec<ChampionStat>,
    pub recent_matches: Vec<RecentMatch>,
}

impl PlayerStats {
    /// 유한한 Elo 만 돌려준다. 배치 중이면 0 이나 NaN 이 오기도 한다.
    pub fn finite_elo(&self) -> Option<f64> {
        self.elo.filter(|e| e.is_finite() && *e > 0.0)
    }
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct ChampionStat {
    pub champion: String,
    pub champion_id: i32,
    pub games: i32,
    pub wins: i32,
    pub win_rate: f64,
    pub avg_kills: f64,
    pub avg_deaths: f64,
    pub avg_assists: f64,
    pub kda: f64,
    pub avg_damage: f64,
    pub avg_cs: f64,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct RecentMatch {
    pub match_id: String,
    pub champion: String,
    pub champion_id: i32,
    pub win: bool,
    pub kills: i32,
    pub deaths: i32,
    pub assists: i32,
    pub damage: i32,
    pub cs: i32,
    pub gold: i32,
    pub game_creation: i64,
    pub game_duration: i32,
    pub queue_id: i32,
}

// ── 소환사 종합 (/summoner/{riotId}) ──────────────
// 프로필 · 연승 · 챔피언 · 최근 경기를 한 번에 준다. 예전처럼 다섯 번
// 나눠 부르지 않아도 된다.

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct SummonerResult {
    pub profile: SummonerProfile,
    pub streak: Option<Streak>,
    pub champion_stats: Vec<ChampionStat>,
    pub recent_matches: Vec<RecentMatch>,
    pub position_stats: Vec<PositionStat>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct SummonerProfile {
    pub riot_id: String,
    pub games: i32,
    pub wins: i32,
    pub losses: i32,
    pub win_rate: f64,
    pub adjusted_win_rate: f64,
    pub sample_grade: SampleGrade,
    pub kda: f64,
    pub avg_kills: f64,
    pub avg_deaths: f64,
    pub avg_assists: f64,
    pub avg_damage: f64,
    pub avg_cs: f64,
    pub avg_gold: f64,
    pub avg_vision_score: f64,
    pub elo: Option<f64>,
    pub elo_rank: Option<i32>,
    pub elo_ranked_total: Option<i32>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct Streak {
    /// 음수면 연패. `type` 이 "LOSS" 인 것과 같은 뜻이다.
    pub current: i32,
    #[serde(rename = "type")]
    pub kind: String,
    pub longest_win: i32,
    pub longest_loss: i32,
    /// 최신이 앞. `["W", "L", ...]`
    pub recent_form: Vec<String>,
}

impl Streak {
    pub fn is_win_streak(&self) -> bool {
        self.kind.eq_ignore_ascii_case("WIN") || self.current > 0
    }

    pub fn length(&self) -> i32 {
        self.current.abs()
    }

    /// 오래된 것부터 최신 순으로 뒤집어 준다. 화면은 왼쪽이 과거다.
    pub fn form_oldest_first(&self, take: usize) -> Vec<bool> {
        self.recent_form
            .iter()
            .take(take)
            .rev()
            .map(|s| s.eq_ignore_ascii_case("W"))
            .collect()
    }
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct PositionStat {
    pub position: String,
    pub games: i32,
    pub wins: i32,
    pub win_rate: f64,
    pub kda: f64,
}

// ── 매치 (/matches/page, /matches/{id}) ───────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default)]
pub struct MatchPageResult {
    pub matches: Vec<MatchDetail>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MatchDetail {
    pub match_id: String,
    pub queue_id: i32,
    pub game_creation: i64,
    pub game_duration: i32,
    pub participants: Vec<MatchParticipant>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MatchParticipant {
    pub riot_id: String,
    pub champion: String,
    pub champion_id: i32,
    pub team: String,
    pub win: bool,
    pub kills: i32,
    pub deaths: i32,
    pub assists: i32,
    pub damage: i32,
    pub cs: i32,
    pub gold: i32,
    pub vision_score: i32,
    pub assigned_position: String,
}

// ── 매치업 (/stats/matchup) ───────────────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MatchupResult {
    pub champion: String,
    pub champion_id: i32,
    pub matchups: Vec<MatchupStat>,
    pub lane_strength: Vec<LaneStrength>,
    pub min_games: i32,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MatchupStat {
    pub opponent: String,
    pub opponent_id: i32,
    pub position: String,
    pub games: i32,
    pub wins: i32,
    pub win_rate: f64,
    pub adjusted_win_rate: f64,
    pub sample_grade: SampleGrade,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct LaneStrength {
    pub champion: String,
    pub champion_id: i32,
    pub position: String,
    pub games: i32,
    pub win_rate: f64,
    pub adjusted_win_rate: f64,
    pub sample_grade: SampleGrade,
}

// ── 듀오 시너지 (/stats/duo) ──────────────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default)]
pub struct DuoSynergyResult {
    pub duos: Vec<DuoSynergy>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct DuoSynergy {
    pub player1: String,
    pub player2: String,
    pub games: i32,
    pub wins: i32,
    /// 관측 승률. 항상 games 와 같이 보여줄 것.
    pub win_rate: f64,
    /// 전체 평균 쪽으로 끌어당긴 승률. 정렬은 이 값으로 한다.
    pub adjusted_win_rate: f64,
    pub sample_grade: SampleGrade,
    pub kda: f64,
}

// ── 라이벌 (/stats/rival-matchup) ─────────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default)]
pub struct RivalMatchupResult {
    pub rivalries: Vec<RivalEntry>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct RivalEntry {
    pub player1: String,
    pub player2: String,
    pub games: i32,
    pub player1_wins: i32,
    pub player2_wins: i32,
    pub player1_win_rate: f64,
    pub sample_grade: SampleGrade,
}

// ── 챔피언 티어 (/stats/champion-tier) ────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct ChampionTierResult {
    pub tier_list: Vec<ChampionTierEntry>,
    pub total_matches: i32,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct ChampionTierEntry {
    pub champion: String,
    pub champion_id: i32,
    pub tier: String,
    pub tier_score: f64,
    pub games: i32,
    pub win_rate: f64,
    pub adjusted_win_rate: f64,
    pub sample_grade: SampleGrade,
    pub kda: f64,
    pub pick_rate: f64,
    pub avg_damage: f64,
}

// ── 밴 분석 (/stats/ban-analysis) ─────────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct BanAnalysisResult {
    pub top_banned: Vec<BanEntry>,
    pub total_games_analyzed: i32,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct BanEntry {
    pub champion: String,
    pub champion_id: i32,
    pub ban_count: i32,
    pub ban_rate: f64,
}

// ── Elo 리더보드 (/stats/elo) ─────────────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct EloLeaderboardResult {
    pub players: Vec<EloEntry>,
    pub min_games: i32,
    pub ranked_count: i32,
    pub placement_count: i32,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct EloEntry {
    /// 배치 중인 플레이어는 0. 순위를 매기지 않는다.
    pub rank: i32,
    pub riot_id: String,
    pub elo: f64,
    pub games: i32,
    pub wins: i32,
    pub losses: i32,
    pub win_rate: f64,
    pub win_streak: i32,
    pub loss_streak: i32,
    /// 최소 경기 수 미달. 목록에는 남기되 순위에서는 뺀다.
    pub placement: bool,
    pub sample_grade: SampleGrade,
}

// ── Elo 히스토리 (/stats/player/{id}/elo-history) ─

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct EloHistoryResult {
    pub riot_id: String,
    pub current_elo: f64,
    pub elo_rank: Option<i32>,
    pub history: Vec<EloHistoryEntry>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct EloHistoryEntry {
    pub match_id: String,
    pub elo_before: f64,
    pub elo_after: f64,
    pub delta: f64,
    pub win: bool,
    /// 같은 포지션 상대와 비교한 라인전 점수(0~1, 0.5가 호각).
    /// 같은 승리에서 Elo 변동폭이 갈리는 근거다.
    pub lane_performance: f64,
    pub game_creation: i64,
}

// ── 어워즈 (/stats/awards) ────────────────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct AwardsResult {
    pub penta_kill_hero: Option<AwardEntry>,
    pub highest_win_rate: Option<AwardEntry>,
    pub most_games_champion: Option<AwardEntry>,
    pub lone_hero: Option<AwardEntry>,
    pub most_deaths: Option<AwardEntry>,
    pub worst_kda: Option<AwardEntry>,
    pub high_gold_low_damage: Option<AwardEntry>,
    pub most_surrenders: Option<AwardEntry>,
}

impl AwardsResult {
    /// 화면에 뿌릴 순서. 칭찬이 앞, 놀림이 뒤.
    pub fn entries(&self) -> Vec<(&'static str, &AwardEntry)> {
        [
            ("펜타킬 영웅", &self.penta_kill_hero),
            ("최고 승률", &self.highest_win_rate),
            ("최다 판수 챔피언", &self.most_games_champion),
            ("고독한 영웅", &self.lone_hero),
            ("최다 데스", &self.most_deaths),
            ("최저 KDA", &self.worst_kda),
            ("돈은 쓰는데 딜은", &self.high_gold_low_damage),
            ("최다 서렌더", &self.most_surrenders),
        ]
        .into_iter()
        .filter_map(|(label, e)| e.as_ref().map(|e| (label, e)))
        .collect()
    }
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct AwardEntry {
    pub riot_id: String,
    pub display_value: String,
    pub games: i32,
}

// ── 멀티킬 (/stats/multikill-highlights) ──────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MultikillResult {
    pub penta_kill_events: Vec<MultikillEvent>,
    pub recent_highlights: Vec<MultikillEvent>,
    pub player_rankings: Vec<MultikillPlayer>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MultikillEvent {
    pub riot_id: String,
    pub champion: String,
    pub champion_id: i32,
    /// "PENTA" · "QUADRA" 등.
    pub multi_kill_type: String,
    pub match_id: String,
    pub game_creation: i64,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MultikillPlayer {
    pub riot_id: String,
    pub penta_kills: i32,
    pub quadra_kills: i32,
    pub triple_kills: i32,
    pub double_kills: i32,
    pub top_champion: Option<String>,
    pub top_champion_id: Option<i32>,
}

// ── MVP (/stats/mvp) ──────────────────────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MvpRankingResult {
    pub rankings: Vec<MvpEntry>,
    pub total_games: i32,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MvpEntry {
    pub riot_id: String,
    pub games: i32,
    pub mvp_count: i32,
    pub ace_count: i32,
    pub mvp_rate: f64,
    pub avg_mvp_score: f64,
    pub top_champion: Option<String>,
    pub top_champion_id: Option<i32>,
}

// ── 포지션별 챔피언 풀 (/stats/position-champion-pool)

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct PositionPoolResult {
    pub all_players: Vec<PositionPoolEntry>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct PositionPoolEntry {
    pub riot_id: String,
    pub position: String,
    pub games: i32,
    pub win_rate: f64,
    pub top_champion: Option<String>,
    pub top_champion_id: Option<i32>,
    pub champions: Vec<PositionPoolChampion>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct PositionPoolChampion {
    pub champion: String,
    pub champion_id: i32,
    pub games: i32,
    pub win_rate: f64,
    pub kda: f64,
}

// ── 플레이스타일 DNA (/stats/playstyle-dna) ───────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default)]
pub struct PlaystyleDnaResult {
    pub players: Vec<DnaEntry>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct DnaEntry {
    pub riot_id: String,
    pub games: i32,
    pub aggression: f64,
    pub durability: f64,
    pub team_play: f64,
    pub objective_focus: f64,
    pub economy: f64,
    pub vision_control: f64,
    pub style_tag: String,
}

impl DnaEntry {
    /// 화면에 뿌릴 (라벨, 0~100) 쌍.
    pub fn axes(&self) -> [(&'static str, f64); 6] {
        [
            ("공격성", self.aggression),
            ("생존력", self.durability),
            ("팀플레이", self.team_play),
            ("오브젝트", self.objective_focus),
            ("성장", self.economy),
            ("시야", self.vision_control),
        ]
    }
}

// ── 개요 (/stats/overview) ────────────────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct OverviewResult {
    pub match_count: i32,
    pub avg_game_minutes: f64,
    pub total_baron_kills: i64,
    pub total_dragon_kills: i64,
    pub total_tower_kills: i64,
    pub total_rift_herald_kills: i64,
    pub total_inhibitor_kills: i64,
    pub total_first_bloods: i64,
    pub total_cs: i64,
    pub top_picked_champions: Vec<ChampionPickStat>,
    pub top_win_rate_champions: Vec<ChampionPickStat>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct ChampionPickStat {
    pub champion: String,
    pub champion_id: i32,
    pub picks: i32,
    pub wins: i32,
    pub win_rate: f64,
    pub kda: f64,
    pub avg_damage: f64,
}

// ── 데미지 분석 (/stats/damage-analysis) ──────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default)]
pub struct DamageAnalysisResult {
    pub players: Vec<DamagePlayerEntry>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct DamagePlayerEntry {
    pub riot_id: String,
    pub games: i32,
    #[serde(alias = "avgTotalDamage")]
    pub avg_total: f64,
    #[serde(alias = "avgPhysicalDamage")]
    pub avg_physical: f64,
    #[serde(alias = "avgMagicDamage")]
    pub avg_magic: f64,
    #[serde(alias = "avgTrueDamage")]
    pub avg_true: f64,
    /// 0~1 비율. 퍼센트가 아니다.
    #[serde(alias = "physicalRatio")]
    pub physical_rate: f64,
    #[serde(alias = "magicRatio")]
    pub magic_rate: f64,
    #[serde(alias = "trueRatio")]
    pub true_rate: f64,
    #[serde(alias = "avgDamageTaken")]
    pub avg_mitigated: f64,
    #[serde(alias = "avgTurretDamage")]
    pub avg_turret_dmg: f64,
    pub damage_profile: String,
}

// ── 시야 분석 (/stats/vision-dominance) ───────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default)]
pub struct VisionDominanceResult {
    pub players: Vec<VisionPlayerEntry>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct VisionPlayerEntry {
    pub riot_id: String,
    pub games: i32,
    pub avg_vision_score: f64,
    pub avg_wards_placed: f64,
    pub avg_wards_killed: f64,
    #[serde(alias = "avgControlWards")]
    pub avg_control_wards_bought: f64,
    /// 0~1. 내가 지운 와드 / 상대가 박은 와드.
    pub ward_kill_rate: f64,
}

// ── 서렌더 분석 (/stats/surrender-analysis) ───────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct SurrenderAnalysisResult {
    pub total_games: i32,
    pub surrender_games: i32,
    pub early_surrender_games: i32,
    /// 0~1 비율. 퍼센트가 아니다.
    pub overall_surrender_rate: f64,
    pub overall_early_surrender_rate: f64,
    pub players: Vec<SurrenderPlayerEntry>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct SurrenderPlayerEntry {
    pub riot_id: String,
    pub games: i32,
    pub surrender_games: i32,
    pub early_surrender_games: i32,
    /// 0~1 비율.
    pub surrender_rate: f64,
}

// ── Riot API 프로필 (/riot/profile) ───────────────

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct RiotProfile {
    pub riot_id: String,
    pub puuid: Option<String>,
    pub summoner_level: Option<i64>,
    pub profile_icon_id: Option<i32>,
    pub solo_rank: Option<RankedInfo>,
    pub flex_rank: Option<RankedInfo>,
    pub top_mastery: Vec<MasteryInfo>,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct RankedInfo {
    pub tier: String,
    pub rank: String,
    pub lp: i32,
    pub wins: i32,
    pub losses: i32,
    pub win_rate: f64,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MasteryInfo {
    pub champion_id: i32,
    pub level: i32,
    pub points: i64,
}

// ── 헬퍼 ─────────────────────────────────────────

/// "이름#태그" 에서 이름만. 표에서는 태그를 떼고 보여준다.
pub fn short_name(riot_id: &str) -> &str {
    riot_id.split('#').next().unwrap_or(riot_id)
}

/// "이름#태그" 에서 태그만.
pub fn tag_of(riot_id: &str) -> &str {
    riot_id.split('#').nth(1).unwrap_or("")
}

/// 0~1 비율을 퍼센트로. 백엔드가 자리에 따라 0~1 과 0~100 을 섞어 쓴다.
/// 1 이하이면 비율로 보고 100을 곱한다.
pub fn as_percent(v: f64) -> f64 {
    if v <= 1.0 {
        v * 100.0
    } else {
        v
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ratio_and_percent_both_read_as_percent() {
        assert!((as_percent(0.09) - 9.0).abs() < 1e-9);
        assert_eq!(as_percent(59.0), 59.0);
        assert_eq!(as_percent(0.0), 0.0);
        // 100% 는 비율로도 1.0 이라 곱해야 맞다.
        assert_eq!(as_percent(1.0), 100.0);
    }

    #[test]
    fn streak_reads_direction_and_form() {
        let s = Streak {
            current: -3,
            kind: "LOSS".to_owned(),
            longest_win: 6,
            longest_loss: 3,
            recent_form: ["L", "L", "L", "W", "L"].map(str::to_owned).to_vec(),
        };
        assert!(!s.is_win_streak());
        assert_eq!(s.length(), 3);
        // 화면은 왼쪽이 과거. 최신이 앞인 배열을 뒤집는다.
        assert_eq!(s.form_oldest_first(5), vec![false, true, false, false, false]);
    }

    #[test]
    fn sample_grade_only_warns_when_thin() {
        assert!(!SampleGrade::High.warns());
        assert!(!SampleGrade::Medium.warns());
        assert!(SampleGrade::Low.warns());
        assert!(SampleGrade::Insufficient.warns());
    }

    #[test]
    fn unknown_sample_grade_does_not_break_parsing() {
        let g: SampleGrade = serde_json::from_str("\"WHATEVER\"").expect("모르는 값도 받아야 한다");
        assert_eq!(g, SampleGrade::Unknown);
    }

    #[test]
    fn damage_entry_accepts_both_field_spellings() {
        // 현재 백엔드
        let a: DamagePlayerEntry = serde_json::from_str(
            r#"{"riotId":"a#KR1","games":7,"avgPhysical":14659,"avgMagic":32762,"avgTrue":3130,
                "avgTotal":50553,"avgMitigated":24130,"avgTurretDmg":12458,
                "physicalRate":0.28,"magicRate":0.64,"trueRate":0.06,"damageProfile":"Hybrid"}"#,
        )
        .expect("현재 필드명 파싱 실패");
        assert_eq!(a.avg_physical, 14659.0);
        assert_eq!(a.physical_rate, 0.28);

        // 프론트엔드 타입 정의 쪽 이름
        let b: DamagePlayerEntry = serde_json::from_str(
            r#"{"riotId":"a#KR1","avgPhysicalDamage":100,"physicalRatio":0.5,
                "avgDamageTaken":10,"avgTurretDamage":20}"#,
        )
        .expect("대체 필드명 파싱 실패");
        assert_eq!(b.avg_physical, 100.0);
        assert_eq!(b.physical_rate, 0.5);
        assert_eq!(b.avg_mitigated, 10.0);
        assert_eq!(b.avg_turret_dmg, 20.0);
    }

    #[test]
    fn missing_fields_fall_back_to_defaults() {
        let p: PlayerStats = serde_json::from_str(r#"{"riotId":"a#KR1"}"#).expect("파싱 실패");
        assert_eq!(p.games, 0);
        assert!(p.champion_stats.is_empty());
        assert_eq!(p.finite_elo(), None);
    }

    #[test]
    fn placement_elo_is_not_treated_as_rating() {
        let p = PlayerStats {
            elo: Some(0.0),
            ..Default::default()
        };
        assert_eq!(p.finite_elo(), None);
    }

    #[test]
    fn short_name_and_tag_split_riot_id() {
        assert_eq!(short_name("화안시인#KR1"), "화안시인");
        assert_eq!(tag_of("화안시인#KR1"), "KR1");
        assert_eq!(short_name("태그없음"), "태그없음");
        assert_eq!(tag_of("태그없음"), "");
    }
}
