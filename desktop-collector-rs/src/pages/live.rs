//! 실시간 분석 — 진행 중인 게임의 스코어 · 오브젝트 타이머 · 골드 차이 추이.

use std::collections::HashMap;
use std::time::{Duration, Instant};

use crate::chart;
use crate::icons::champion_icon;
use crate::lcu::{LiveClientData, LiveClientEvent, LiveClientPlayer, LiveParticipant};
use crate::models::PlayerStats;
use crate::monitor::Signals;
use crate::net::Remote;
use crate::theme::*;
use crate::ui;

use super::MODE;

const REFRESH: Duration = Duration::from_secs(5);

/// 히스토리가 이만큼 쌓이면 절반으로 솎아 낸다. 5초 간격이니 넉넉히 두 시간치다.
const MAX_SNAPSHOTS: usize = 1500;

/// 라이브 클라이언트 API 는 적 골드를 주지 않는다. CS · 킬 · 어시 · 시간으로 추정한다.
/// 양 팀에 같은 공식을 쓰므로 절대값은 부정확해도 "차이"는 의미가 있다.
///  - CS 21g · 킬 300g · 어시 150g · 패시브 2.04g/s (1:50부터)
fn estimate_gold(p: &LiveClientPlayer, game_time: f64) -> f64 {
    let passive = (game_time - 110.0).max(0.0) * 2.04;
    p.creep_score as f64 * 21.0 + p.kills as f64 * 300.0 + p.assists as f64 * 150.0 + passive
}

fn team_gold(players: &[&LiveClientPlayer], game_time: f64) -> f64 {
    players.iter().map(|p| estimate_gold(p, game_time)).sum()
}

/// 마지막 처치 시각 + 쿨다운. 한 번도 안 잡혔으면 첫 스폰 시각.
fn next_respawn(
    events: &[LiveClientEvent],
    name: &str,
    first_spawn: f64,
    cooldown: f64,
    game_time: f64,
) -> f64 {
    match events
        .iter()
        .filter(|e| e.event_name == name)
        .map(|e| e.event_time)
        .fold(None, |acc: Option<f64>, t| Some(acc.map_or(t, |a| a.max(t))))
    {
        Some(last) => last + cooldown,
        None => first_spawn.max(game_time),
    }
}

fn countdown(remaining: f64) -> String {
    if remaining <= 0.0 {
        return "지금".to_owned();
    }
    ui::mmss(remaining as i64)
}

#[derive(Clone, Copy)]
struct Snapshot {
    game_time: f32,
    gold_diff: f32,
}

#[derive(Clone, Default)]
struct TeamsData {
    phase: String,
    participants: Vec<LiveParticipant>,
    /// 소환사 이름 → 내전 통계. 라이브 API 는 riotId 를 안 주므로 이름으로 잇는다.
    stats: HashMap<String, PlayerStats>,
}

pub struct LivePage {
    teams: Remote<TeamsData>,
    live: Remote<Option<LiveClientData>>,
    snapshots: Vec<Snapshot>,
    last_refresh: Instant,
}

impl Default for LivePage {
    fn default() -> Self {
        Self {
            teams: Remote::new(),
            live: Remote::new(),
            snapshots: Vec::new(),
            last_refresh: Instant::now() - REFRESH,
        }
    }
}

impl LivePage {
    pub fn ui(&mut self, ui: &mut egui::Ui, _signals: &Signals) {
        let ctx = ui.ctx().clone();
        if self.last_refresh.elapsed() >= REFRESH {
            self.last_refresh = Instant::now();
            self.teams.reload(&ctx, load_teams());
            self.live
                .reload(&ctx, async { Ok(crate::lcu::live_client_data().await) });
        }
        ctx.request_repaint_after(Duration::from_secs(1));

        self.record_snapshot();

        let teams = self.teams.lock().value.clone().unwrap_or_default();
        let live = self.live.lock().value.clone().flatten();

        ui::page(
            ui,
            "실시간 분석",
            "진행 중인 게임의 팀 구성과 내전 통계",
            |ui| {
                ui::card(ui, |ui| {
                    ui.horizontal(|ui| {
                        let in_game = teams.phase == "InProgress";
                        ui.label(ui::txt(
                            if in_game { "게임 진행 중" } else { "현재 게임 없음" },
                            16.0,
                            ui::W::Bold,
                            if in_game { GRAY_900 } else { GRAY_600 },
                        ));
                        if !teams.phase.is_empty() && !in_game {
                            ui::chip_neutral(ui, &teams.phase);
                        }
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            if ui::secondary_button(ui, "새로고침", true).clicked() {
                                self.last_refresh = Instant::now();
                                self.teams.reload(&ctx, load_teams());
                                self.live
                                    .reload(&ctx, async { Ok(crate::lcu::live_client_data().await) });
                            }
                            ui::muted(ui, "5초마다 자동 갱신");
                        });
                    });
                });

                if teams.phase != "InProgress" {
                    ui::card(ui, |ui| {
                        ui::empty(ui, "게임이 시작되면 실시간 정보가 표시됩니다")
                    });
                    return;
                }

                for (team_id, title, color) in
                    [(100, "블루팀", TEAM_BLUE), (200, "레드팀", TEAM_RED)]
                {
                    let players: Vec<&LiveParticipant> = teams
                        .participants
                        .iter()
                        .filter(|p| p.team_id == team_id)
                        .collect();
                    if players.is_empty() {
                        continue;
                    }
                    team_card(ui, title, color, &players, &teams.stats);
                }

                let Some(ld) = live.as_ref() else {
                    ui::card(ui, |ui| {
                        ui::empty(ui, "실시간 데이터(포트 2999)를 기다리는 중입니다")
                    });
                    return;
                };

                diff_card(ui, ld);
                objective_card(ui, ld);
                if self.snapshots.len() >= 2 {
                    self.gold_chart(ui, ld);
                }
                score_card(ui, ld);
                events_card(ui, ld);
            },
        );
    }

    /// 골드 차이 히스토리를 한 점씩 쌓는다. 메모리에만 있고 게임이 바뀌면 버린다.
    fn record_snapshot(&mut self) {
        let Some(Some(ld)) = self.live.lock().value.clone() else {
            return;
        };
        let (blue, red) = split_teams(&ld);
        let diff = (team_gold(&blue, ld.game_time) - team_gold(&red, ld.game_time)) as f32;
        let snap = Snapshot {
            game_time: ld.game_time as f32,
            gold_diff: diff,
        };

        match self.snapshots.last() {
            // 시간이 되감겼으면 새 게임이다.
            Some(last) if snap.game_time < last.game_time - 30.0 => self.snapshots.clear(),
            // 같은 시각을 두 번 담지 않는다.
            Some(last) if snap.game_time - last.game_time < 1.0 => return,
            _ => {}
        }
        self.snapshots.push(snap);

        if self.snapshots.len() > MAX_SNAPSHOTS {
            let thinned: Vec<Snapshot> = self
                .snapshots
                .iter()
                .step_by(2)
                .copied()
                .collect();
            self.snapshots = thinned;
        }
    }

    fn gold_chart(&self, ui: &mut egui::Ui, ld: &LiveClientData) {
        let (blue, red) = split_teams(ld);
        let diff = team_gold(&blue, ld.game_time) - team_gold(&red, ld.game_time);
        let leading = if diff >= 0.0 { "블루" } else { "레드" };

        ui::card_with_head(
            ui,
            "골드 차이 추이",
            |ui| {
                ui.label(ui::txt(
                    format!("{leading} +{}", ui::short_num(diff.abs())),
                    14.0,
                    ui::W::Semibold,
                    if diff >= 0.0 { TEAM_BLUE } else { TEAM_RED },
                ));
            },
            |ui| {
                let points: Vec<(f32, f32)> = self
                    .snapshots
                    .iter()
                    .map(|s| (s.game_time, s.gold_diff))
                    .collect();
                chart::signed_line(ui, &points, 120.0);
                ui.horizontal(|ui| {
                    ui::muted(ui, ui::mmss(points[0].0 as i64));
                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        ui::muted(ui, ui::mmss(points[points.len() - 1].0 as i64));
                    });
                });
            },
        );
    }
}

fn split_teams(ld: &LiveClientData) -> (Vec<&LiveClientPlayer>, Vec<&LiveClientPlayer>) {
    (
        ld.players.iter().filter(|p| p.team == "ORDER").collect(),
        ld.players.iter().filter(|p| p.team == "CHAOS").collect(),
    )
}

async fn load_teams() -> Result<TeamsData, String> {
    let Some((phase, participants)) = crate::lcu::live_teams().await else {
        return Ok(TeamsData::default());
    };
    if phase != "InProgress" {
        return Ok(TeamsData {
            phase,
            ..Default::default()
        });
    }

    // gameflow 는 소환사 이름만 준다. riotId 는 팀 조회로 이어 붙인다.
    let mut stats = HashMap::new();
    if let Some(teams) = crate::lcu::custom_teams().await {
        for m in teams.blue_team.iter().chain(teams.red_team.iter()) {
            if m.summoner_name.is_empty() || m.riot_id.is_empty() {
                continue;
            }
            if let Ok(s) = crate::api::player_stats(m.riot_id.clone(), MODE).await {
                stats.insert(m.summoner_name.clone(), s);
            }
        }
    }

    Ok(TeamsData {
        phase,
        participants,
        stats,
    })
}

fn team_card(
    ui: &mut egui::Ui,
    title: &str,
    color: egui::Color32,
    players: &[&LiveParticipant],
    stats: &HashMap<String, PlayerStats>,
) {
    ui::card_with_head(
        ui,
        title,
        |ui| ui::muted(ui, format!("{}명", players.len())),
        |ui| {
            for p in players {
                let s = stats.get(&p.summoner_name);
                ui::table_row(ui, |ui| {
                    ui::cell(ui, 40.0, |ui| {
                        champion_icon(ui, p.champion_id, 32.0);
                    });
                    ui::cell(ui, 150.0, |ui| {
                        ui.label(ui::txt(&p.summoner_name, 14.0, ui::W::Semibold, color))
                    });
                    ui::cell(ui, 96.0, |ui| match s.and_then(|s| s.finite_elo()) {
                        Some(elo) => ui::num(ui, format!("Elo {elo:.0}"), elo_color(Some(elo))),
                        None => ui::muted(ui, "내전 기록 없음"),
                    });
                    ui::cell(ui, 110.0, |ui| {
                        if let Some(s) = s {
                            ui::muted(ui, format!("{}판 {}", s.games, ui::pct(s.win_rate)));
                        }
                    });
                    if let Some(s) = s {
                        for c in s.champion_stats.iter().take(3) {
                            champion_icon(ui, c.champion_id, 22.0);
                            ui.label(ui::txt(
                                ui::pct(c.win_rate),
                                11.0,
                                ui::W::Regular,
                                win_rate_color(c.win_rate),
                            ));
                        }
                    }
                });
            }
        },
    );
}

fn diff_card(ui: &mut egui::Ui, ld: &LiveClientData) {
    let (blue, red) = split_teams(ld);
    let kill_diff: i32 =
        blue.iter().map(|p| p.kills).sum::<i32>() - red.iter().map(|p| p.kills).sum::<i32>();
    let cs_diff: i32 = blue.iter().map(|p| p.creep_score).sum::<i32>()
        - red.iter().map(|p| p.creep_score).sum::<i32>();
    let gold_diff = team_gold(&blue, ld.game_time) - team_gold(&red, ld.game_time);

    ui::card_with_head(
        ui,
        "팀 차이",
        |ui| ui::muted(ui, "블루 기준"),
        |ui| {
            ui.horizontal(|ui| {
                ui.spacing_mut().item_spacing.x = 40.0;
                diff_stat(ui, "킬", kill_diff as f64, kill_diff.to_string());
                diff_stat(ui, "CS", cs_diff as f64, cs_diff.abs().to_string());
                diff_stat(ui, "골드(추정)", gold_diff, ui::short_num(gold_diff.abs()));
            });
        },
    );
}

fn diff_stat(ui: &mut egui::Ui, label: &str, signed: f64, magnitude: String) {
    let (color, prefix) = if signed > 0.0 {
        (TEAM_BLUE, "+")
    } else if signed < 0.0 {
        (TEAM_RED, "-")
    } else {
        (GRAY_600, "±")
    };
    let magnitude = magnitude.trim_start_matches('-').to_owned();
    ui::stat(ui, label, &format!("{prefix}{magnitude}"), color);
}

fn objective_card(ui: &mut egui::Ui, ld: &LiveClientData) {
    let dragon = next_respawn(&ld.events, "DragonKill", 300.0, 300.0, ld.game_time);
    let baron = next_respawn(&ld.events, "BaronKill", 1500.0, 360.0, ld.game_time);
    let herald = next_respawn(&ld.events, "HeraldKill", 840.0, 360.0, ld.game_time);

    ui::card_with_head(
        ui,
        "오브젝트 리스폰",
        |ui| ui::muted(ui, "마지막 처치 기준 추정"),
        |ui| {
            ui.horizontal(|ui| {
                ui.spacing_mut().item_spacing.x = 40.0;
                ui::stat(ui, "드래곤", &countdown(dragon - ld.game_time), GRAY_900);
                ui::stat(ui, "바론", &countdown(baron - ld.game_time), GRAY_900);
                ui::stat(ui, "전령", &countdown(herald - ld.game_time), GRAY_900);
            });
        },
    );
}

fn score_card(ui: &mut egui::Ui, ld: &LiveClientData) {
    let (blue, red) = split_teams(ld);
    let blue_kills: i32 = blue.iter().map(|p| p.kills).sum();
    let red_kills: i32 = red.iter().map(|p| p.kills).sum();

    ui::card_with_head(
        ui,
        "실시간 스코어",
        |ui| {
            ui.label(ui::txt(
                ui::mmss(ld.game_time as i64),
                15.0,
                ui::W::Bold,
                GRAY_900,
            ));
        },
        |ui| {
            ui.horizontal(|ui| {
                ui::stat(ui, "블루팀", &blue_kills.to_string(), TEAM_BLUE);
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    ui::stat(ui, "레드팀", &red_kills.to_string(), TEAM_RED);
                });
            });
            ui.add_space(10.0);

            for p in &ld.players {
                let color = if p.team == "ORDER" { TEAM_BLUE } else { TEAM_RED };
                ui::table_row(ui, |ui| {
                    ui::cell(ui, 100.0, |ui| {
                        ui.label(ui::txt(&p.champion_name, 14.0, ui::W::Semibold, color))
                    });
                    ui::cell(ui, 160.0, |ui| ui::td(ui, p.summoner_name.clone()));
                    ui::cell(ui, 44.0, |ui| ui::muted(ui, format!("Lv{}", p.level)));
                    ui::cell(ui, 80.0, |ui| {
                        ui::num(ui, format!("{}/{}/{}", p.kills, p.deaths, p.assists), GRAY_900)
                    });
                    ui::cell(ui, 60.0, |ui| ui::muted(ui, format!("{}CS", p.creep_score)));
                });
            }
        },
    );
}

fn events_card(ui: &mut egui::Ui, ld: &LiveClientData) {
    const INTERESTING: [&str; 10] = [
        "ChampionKill",
        "DragonKill",
        "BaronKill",
        "HeraldKill",
        "TurretKilled",
        "InhibKilled",
        "FirstBlood",
        "Ace",
        "Multikill",
        "FirstBrick",
    ];

    let events: Vec<&LiveClientEvent> = ld
        .events
        .iter()
        .filter(|e| INTERESTING.contains(&e.event_name.as_str()))
        .rev()
        .take(15)
        .collect();
    if events.is_empty() {
        return;
    }

    ui::card_with_head(
        ui,
        "게임 이벤트",
        |ui| ui::muted(ui, "최근 15건"),
        |ui| {
            for e in events {
                let (label, color) = match e.event_name.as_str() {
                    "ChampionKill" => ("킬", LOSS),
                    "DragonKill" => ("드래곤", WARN_FG),
                    "BaronKill" => ("바론", GRAY_800),
                    "HeraldKill" => ("전령", BLUE_500),
                    "TurretKilled" => ("포탑 파괴", GRAY_600),
                    "InhibKilled" => ("억제기 파괴", BLUE_600),
                    "FirstBlood" => ("퍼스트 블러드", LOSS),
                    "Ace" => ("에이스", BLUE_600),
                    "FirstBrick" => ("첫 포탑", BLUE_500),
                    other => (other, GRAY_600),
                };
                ui::table_row(ui, |ui| {
                    ui::cell(ui, 50.0, |ui| ui::muted(ui, ui::mmss(e.event_time as i64)));
                    ui::cell(ui, 100.0, |ui| {
                        ui.label(ui::txt(label, 13.0, ui::W::Semibold, color))
                    });
                    let mut who = e.killer_name.clone().unwrap_or_default();
                    if !e.assisters.is_empty() {
                        who.push_str(&format!(" (+{})", e.assisters.join(", ")));
                    }
                    ui::td(ui, who);
                });
            }
        },
    );
}

#[cfg(test)]
mod tests {
    use super::*;

    fn player(cs: i32, kills: i32, assists: i32) -> LiveClientPlayer {
        LiveClientPlayer {
            creep_score: cs,
            kills,
            assists,
            ..Default::default()
        }
    }

    #[test]
    fn gold_estimate_includes_passive_only_after_first_wave() {
        // 1:50 이전에는 패시브 골드가 붙지 않는다.
        let p = player(0, 0, 0);
        assert_eq!(estimate_gold(&p, 60.0), 0.0);
        assert!((estimate_gold(&p, 210.0) - 100.0 * 2.04).abs() < 1e-6);
    }

    #[test]
    fn gold_estimate_weights_cs_kills_assists() {
        let p = player(100, 2, 3);
        // 100*21 + 2*300 + 3*150 = 2100 + 600 + 450
        assert!((estimate_gold(&p, 110.0) - 3150.0).abs() < 1e-6);
    }

    #[test]
    fn respawn_uses_last_kill_then_cooldown() {
        let events = vec![
            LiveClientEvent {
                event_name: "DragonKill".to_owned(),
                event_time: 400.0,
                ..Default::default()
            },
            LiveClientEvent {
                event_name: "DragonKill".to_owned(),
                event_time: 700.0,
                ..Default::default()
            },
        ];
        assert_eq!(next_respawn(&events, "DragonKill", 300.0, 300.0, 800.0), 1000.0);
        // 한 번도 안 잡혔으면 첫 스폰 시각. 이미 지났으면 "지금".
        assert_eq!(next_respawn(&[], "BaronKill", 1500.0, 360.0, 600.0), 1500.0);
        assert_eq!(next_respawn(&[], "BaronKill", 1500.0, 360.0, 1600.0), 1600.0);
    }

    #[test]
    fn countdown_reads_as_clock() {
        assert_eq!(countdown(0.0), "지금");
        assert_eq!(countdown(-3.0), "지금");
        assert_eq!(countdown(125.0), "2:05");
    }
}
