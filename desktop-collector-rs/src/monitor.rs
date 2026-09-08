//! 게임 페이즈 감시.
//!
//! 두 개의 루프를 돈다.
//! - 5초: 닷지 감지 + 챔피언 선택 팀 구성 기록
//! - 30초: 로비 캐시 채우기 + 게임 종료 후 자동 수집
//!
//! 자동 수집을 게임 종료 직후가 아니라 30초 뒤에 거는 이유는, 라이엇이 매치
//! 히스토리에 경기를 반영하는 데 시간이 걸리기 때문이다.

use std::time::Duration;

use crate::collect::{self, LogKind, LogLine};
use crate::lcu::{self, LcuStatus};
use crate::models::{DuoSynergy, PlayerStats, RivalEntry};
use crate::net::{spawn, Shared};

/// 게임 종료를 감지하고 실제 수집을 시작하기까지의 유예.
const AUTO_COLLECT_DELAY: Duration = Duration::from_secs(30);

const LOG_LIMIT: usize = 500;

#[derive(Debug, Clone, Default)]
pub struct CachedPlayer {
    pub riot_id: String,
    pub summoner_name: String,
    pub is_me: bool,
}

/// 로비에서 본 팀 구성을 들고 있는다.
///
/// 챔피언 선택에 들어가면 LCU 가 상대팀 `summonerId` 를 가려 버려서 누가
/// 상대인지 알 수 없다. 로비 단계에서 받아 둔 명단을 그때 대신 쓴다.
#[derive(Debug, Clone, Default)]
pub struct LobbyCache {
    pub blue_team: Vec<CachedPlayer>,
    pub red_team: Vec<CachedPlayer>,
    pub player_stats: std::collections::HashMap<String, PlayerStats>,
    pub duo_synergies: Vec<DuoSynergy>,
    pub rival_matchups: Vec<RivalEntry>,
}

impl LobbyCache {
    pub fn is_valid(&self) -> bool {
        !self.blue_team.is_empty() || !self.red_team.is_empty()
    }

    pub fn i_am_blue(&self) -> bool {
        // 내가 블루에 없으면 레드로 본다. 양쪽 다 없으면 블루 기준.
        self.blue_team.iter().any(|p| p.is_me) || !self.red_team.iter().any(|p| p.is_me)
    }

    pub fn enemy_team(&self) -> &[CachedPlayer] {
        if self.i_am_blue() {
            &self.red_team
        } else {
            &self.blue_team
        }
    }

    pub fn update_from_lobby(&mut self, blue: &[lcu::TeamMemberInfo], red: &[lcu::TeamMemberInfo]) {
        let map = |m: &lcu::TeamMemberInfo| CachedPlayer {
            riot_id: m.riot_id.clone(),
            summoner_name: m.summoner_name.clone(),
            is_me: m.is_me,
        };
        self.blue_team = blue.iter().map(map).collect();
        self.red_team = red.iter().map(map).collect();
    }

    pub fn clear(&mut self) {
        *self = Self::default();
    }
}

/// 백그라운드 루프들이 UI 와 나눠 쓰는 상태.
#[derive(Clone)]
pub struct Signals {
    pub lcu_status: Shared<LcuStatus>,
    pub game_phase: Shared<String>,
    pub logs: Shared<Vec<LogLine>>,
    pub auto_status: Shared<String>,
    pub dodge_count: Shared<i32>,
    pub lobby: Shared<LobbyCache>,
    /// 게임이 막 시작되면 실시간 탭으로 한 번 넘겨 준다. UI 가 소비하고 되돌린다.
    pub jump_to_live: Shared<bool>,
}

impl Default for Signals {
    fn default() -> Self {
        Self {
            lcu_status: Shared::new(LcuStatus::default()),
            game_phase: Shared::new(String::new()),
            logs: Shared::new(Vec::new()),
            auto_status: Shared::new(String::new()),
            dodge_count: Shared::new(0),
            lobby: Shared::new(LobbyCache::default()),
            jump_to_live: Shared::new(false),
        }
    }
}

impl Signals {
    pub fn log(&self, kind: LogKind, message: String) {
        let mut logs = self.logs.lock();
        if logs.len() >= LOG_LIMIT {
            logs.remove(0);
        }
        logs.push(LogLine { kind, message });
    }
}

fn hhmmss() -> String {
    use chrono::{FixedOffset, Utc};
    let kst = FixedOffset::east_opt(9 * 3600).expect("고정 오프셋");
    Utc::now().with_timezone(&kst).format("%H:%M:%S").to_string()
}

/// 모든 감시 루프를 띄운다. 앱이 살아 있는 동안 계속 돈다.
pub fn start(signals: Signals, ctx: egui::Context) {
    spawn_status_loop(signals.clone(), ctx.clone());
    spawn_dodge_loop(signals.clone(), ctx.clone());
    spawn_auto_collect_loop(signals, ctx);
}

/// LCU 접속 상태 — 타이틀바 표시에 쓴다.
fn spawn_status_loop(signals: Signals, ctx: egui::Context) {
    spawn(async move {
        loop {
            let status = lcu::status().await;
            let changed = {
                let prev = signals.lcu_status.lock();
                prev.connected != status.connected
                    || prev.display_name() != status.display_name()
            };
            signals.lcu_status.set(status);
            if changed {
                ctx.request_repaint();
            }
            tokio::time::sleep(Duration::from_secs(5)).await;
        }
    });
}

/// 닷지 감지 + 게임 시작 감지. 5초 간격.
fn spawn_dodge_loop(signals: Signals, ctx: egui::Context) {
    spawn(async move {
        let mut last_phase = String::new();
        let mut auto_switched_this_game = false;
        loop {
            let phase = lcu::game_phase().await.unwrap_or_default();

            // 챔피언 선택 → None/Lobby 로 튕기면 누군가 닷지한 것이다.
            if last_phase == "ChampSelect" && (phase == "None" || phase == "Lobby") {
                let count = {
                    let mut c = signals.dodge_count.lock();
                    *c += 1;
                    *c
                };
                let ts = hhmmss();
                signals.log(LogKind::Warn, format!("닷지 감지 #{count} ({ts})"));
                signals.auto_status.set(format!("닷지 감지 — 총 {count}회"));
                ctx.request_repaint();
            }

            // 게임이 막 시작됐으면 실시간 탭으로 한 번 넘긴다.
            if phase == "InProgress" && last_phase != "InProgress" && !auto_switched_this_game {
                signals.jump_to_live.set(true);
                auto_switched_this_game = true;
                ctx.request_repaint();
            }
            if phase != "InProgress" && phase != "GameStart" {
                auto_switched_this_game = false;
            }

            if *signals.game_phase.lock() != phase {
                signals.game_phase.set(phase.clone());
                ctx.request_repaint();
            }
            last_phase = phase;
            tokio::time::sleep(Duration::from_secs(5)).await;
        }
    });
}

/// 로비 캐시 + 게임 종료 후 자동 수집. 30초 간격.
fn spawn_auto_collect_loop(signals: Signals, ctx: egui::Context) {
    spawn(async move {
        let mut last_phase = String::new();
        let mut scheduled = false;
        loop {
            let phase = lcu::game_phase().await.unwrap_or_default();

            if phase == "Lobby" {
                lcu::cache_lobby_members().await;

                // 새 로비에 들어왔으면 이전 판의 캐시를 버린다.
                if matches!(last_phase.as_str(), "None" | "EndOfGame" | "WaitingForStats" | "") {
                    signals.lobby.lock().clear();
                    signals.log(LogKind::Info, "로비 캐시 초기화 — 새로운 로비 진입".to_owned());
                }

                if let Some(teams) = lcu::custom_teams().await {
                    if !teams.blue_team.is_empty() || !teams.red_team.is_empty() {
                        signals
                            .lobby
                            .lock()
                            .update_from_lobby(&teams.blue_team, &teams.red_team);
                        let (b, r) = (teams.blue_team.len(), teams.red_team.len());
                        signals.log(
                            LogKind::Info,
                            format!("로비 캐시 업데이트: 블루 {b}명, 레드 {r}명"),
                        );
                        ctx.request_repaint();
                    }
                }
            }

            let game_just_ended = last_phase == "InProgress"
                && matches!(
                    phase.as_str(),
                    "EndOfGame" | "WaitingForStats" | "None" | "Lobby"
                );

            if game_just_ended && !scheduled {
                scheduled = true;
                signals
                    .auto_status
                    .set("게임 종료 감지 — 30초 후 자동 수집 시작".to_owned());
                ctx.request_repaint();

                let s = signals.clone();
                let c = ctx.clone();
                spawn(async move {
                    tokio::time::sleep(AUTO_COLLECT_DELAY).await;
                    run_collect(s, c).await;
                });
            }
            if !game_just_ended && phase != "InProgress" {
                scheduled = false;
            }

            last_phase = phase;
            tokio::time::sleep(Duration::from_secs(30)).await;
        }
    });
}

/// 수집을 돌리며 로그와 자동 상태 문구를 갱신한다. 수동 버튼도 이걸 쓴다.
pub async fn run_collect(signals: Signals, ctx: egui::Context) {
    let s = signals.clone();
    let c = ctx.clone();
    collect::run(move |kind, message| {
        s.log(kind, message.clone());
        match kind {
            LogKind::Done => s.auto_status.set(format!("자동 수집 완료 — {message}")),
            LogKind::Error => s.auto_status.set(format!("자동 수집 실패 — {message}")),
            _ => {}
        }
        c.request_repaint();
    })
    .await;
}

#[cfg(test)]
mod tests {
    use super::*;

    fn member(riot_id: &str, is_me: bool) -> lcu::TeamMemberInfo {
        lcu::TeamMemberInfo {
            summoner_name: riot_id.split('#').next().unwrap_or("").to_owned(),
            riot_id: riot_id.to_owned(),
            is_me,
        }
    }

    #[test]
    fn enemy_team_flips_with_my_side() {
        let mut cache = LobbyCache::default();
        cache.update_from_lobby(
            &[member("나#KR1", true), member("친구#KR1", false)],
            &[member("적1#KR1", false), member("적2#KR1", false)],
        );
        assert!(cache.i_am_blue());
        let enemies: Vec<&str> = cache.enemy_team().iter().map(|p| p.riot_id.as_str()).collect();
        assert_eq!(enemies, vec!["적1#KR1", "적2#KR1"]);

        cache.update_from_lobby(
            &[member("적1#KR1", false)],
            &[member("나#KR1", true), member("친구#KR1", false)],
        );
        assert!(!cache.i_am_blue());
        let enemies: Vec<&str> = cache.enemy_team().iter().map(|p| p.riot_id.as_str()).collect();
        assert_eq!(enemies, vec!["적1#KR1"]);
    }

    #[test]
    fn cache_is_invalid_until_filled_and_after_clear() {
        let mut cache = LobbyCache::default();
        assert!(!cache.is_valid());
        cache.update_from_lobby(&[member("나#KR1", true)], &[]);
        assert!(cache.is_valid());
        cache.clear();
        assert!(!cache.is_valid());
    }

    #[test]
    fn log_buffer_is_bounded() {
        let signals = Signals::default();
        for i in 0..(LOG_LIMIT + 50) {
            signals.log(LogKind::Info, format!("{i}"));
        }
        let logs = signals.logs.lock();
        assert_eq!(logs.len(), LOG_LIMIT);
        // 오래된 쪽부터 잘려 나간다.
        assert_eq!(logs.first().unwrap().message, "50");
        assert_eq!(logs.last().unwrap().message, (LOG_LIMIT + 49).to_string());
    }
}
