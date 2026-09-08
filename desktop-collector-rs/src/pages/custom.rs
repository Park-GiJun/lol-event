//! 내전 분석 — 블루/레드 팀 구성과 팀 전력 비교.
//!
//! 로비 · 챔피언 선택 · 게임 중 어느 단계에서도 동작한다. 여기서 받아 둔 팀
//! 정보는 [`crate::monitor::LobbyCache`] 에도 넣어 둔다. 챔피언 선택에 들어가면
//! LCU 가 상대팀 정보를 가려 버리기 때문이다.

use std::collections::HashMap;
use std::time::{Duration, Instant};

use crate::icons::champion_icon;
use crate::models::*;
use crate::monitor::Signals;
use crate::net::Remote;
use crate::theme::*;
use crate::ui;

use super::MODE;

/// 자동 새로고침 주기. 로비 인원이 들락날락하므로 짧게 잡는다.
const REFRESH: Duration = Duration::from_secs(10);

#[derive(Clone, Default)]
pub struct PlayerData {
    pub summoner_name: String,
    pub riot_id: String,
    pub is_me: bool,
    pub stats: Option<PlayerStats>,
    pub profile: Option<RiotProfile>,
}

impl PlayerData {
    fn display(&self) -> &str {
        if self.summoner_name.is_empty() {
            short_name(&self.riot_id)
        } else {
            &self.summoner_name
        }
    }

    fn elo(&self) -> Option<f64> {
        self.stats.as_ref().and_then(PlayerStats::finite_elo)
    }
}

#[derive(Clone, Default)]
pub struct CustomData {
    pub phase: String,
    pub blue: Vec<PlayerData>,
    pub red: Vec<PlayerData>,
    pub duos: Vec<DuoSynergy>,
    pub rivals: Vec<RivalEntry>,
}

pub struct CustomPage {
    data: Remote<CustomData>,
    last_refresh: Instant,
}

impl Default for CustomPage {
    fn default() -> Self {
        Self {
            data: Remote::new(),
            // 처음 한 번은 바로 받도록 과거 시각으로 둔다.
            last_refresh: Instant::now() - REFRESH,
        }
    }
}

impl CustomPage {
    pub fn ui(&mut self, ui: &mut egui::Ui, signals: &Signals) {
        let ctx = ui.ctx().clone();
        if self.last_refresh.elapsed() >= REFRESH {
            self.last_refresh = Instant::now();
            self.data.reload(&ctx, load(signals.clone()));
        }
        // 로비 상태는 계속 바뀐다. 화면을 열어 둔 동안 주기적으로 다시 그린다.
        ctx.request_repaint_after(Duration::from_secs(1));

        ui::page(ui, "내전 분석", "블루팀 / 레드팀 구성과 전력 비교", |ui| {
            let (loading, error, data) = {
                let slot = self.data.lock();
                (slot.loading, slot.error.clone(), slot.value.clone())
            };

            ui::card(ui, |ui| {
                ui.horizontal(|ui| {
                    let phase = data.as_ref().map(|d| d.phase.as_str()).unwrap_or("");
                    ui.label(ui::txt(phase_label(phase), 16.0, ui::W::Bold, GRAY_900));
                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        if ui::secondary_button(
                            ui,
                            if loading { "불러오는 중..." } else { "새로고침" },
                            !loading,
                        )
                        .clicked()
                        {
                            self.last_refresh = Instant::now();
                            self.data.reload(&ctx, load(signals.clone()));
                        }
                        ui::muted(ui, "10초마다 자동 갱신");
                    });
                });
                if let Some(e) = &error {
                    ui.add_space(6.0);
                    ui::error(ui, e);
                }
            });

            let Some(d) = data else {
                ui::card(ui, |ui| ui::empty(ui, "롤 클라이언트에 연결하는 중..."));
                return;
            };
            if d.blue.is_empty() && d.red.is_empty() {
                ui::card(ui, |ui| {
                    ui::empty(ui, "내전 로비에 들어가면 팀 구성이 표시됩니다")
                });
                return;
            }

            ui::two_columns(
                ui,
                |ui| team_card(ui, "블루팀", TEAM_BLUE, &d.blue),
                |ui| team_card(ui, "레드팀", TEAM_RED, &d.red),
            );

            elo_card(ui, &d);
            ranking_card(ui, &d);

            if !d.duos.is_empty() {
                let blue_ids: Vec<String> = d.blue.iter().map(|p| p.riot_id.clone()).collect();
                let red_ids: Vec<String> = d.red.iter().map(|p| p.riot_id.clone()).collect();
                ui::two_columns(
                    ui,
                    |ui| duo_card(ui, "블루팀 듀오 시너지", TEAM_BLUE, &d.duos, &blue_ids),
                    |ui| duo_card(ui, "레드팀 듀오 시너지", TEAM_RED, &d.duos, &red_ids),
                );
            }
            if !d.rivals.is_empty() {
                rival_card(ui, &d.rivals);
            }
            top_champs_card(ui, &d);
        });
    }
}

fn phase_label(phase: &str) -> &str {
    match phase {
        "Lobby" => "대기방",
        "ChampSelect" => "챔피언 선택",
        "InProgress" => "게임 중",
        "None" | "" => "대기 중",
        other => other,
    }
}

async fn load(signals: Signals) -> Result<CustomData, String> {
    let teams = crate::lcu::custom_teams()
        .await
        .ok_or("롤 클라이언트에 연결할 수 없습니다")?;

    // 팀 명단은 다음 단계(챔피언 선택)에서 필요하므로 먼저 캐시에 넣는다.
    signals
        .lobby
        .lock()
        .update_from_lobby(&teams.blue_team, &teams.red_team);

    let all_ids: Vec<String> = teams
        .blue_team
        .iter()
        .chain(teams.red_team.iter())
        .map(|m| m.riot_id.clone())
        .filter(|id| !id.is_empty())
        .collect();

    // 랭크 정보는 한 번에 몰아 받는다. 실패해도 내전 통계는 보여 준다.
    let profiles = crate::api::riot_profiles(all_ids.clone())
        .await
        .unwrap_or_default();

    let mut stats_by_id: HashMap<String, PlayerStats> = HashMap::new();
    for id in &all_ids {
        if let Ok(s) = crate::api::player_stats(id.clone(), MODE).await {
            stats_by_id.insert(id.clone(), s);
        }
    }

    let build = |members: &[crate::lcu::TeamMemberInfo]| -> Vec<PlayerData> {
        members
            .iter()
            .map(|m| PlayerData {
                summoner_name: m.summoner_name.clone(),
                riot_id: m.riot_id.clone(),
                is_me: m.is_me,
                stats: stats_by_id.get(&m.riot_id).cloned(),
                profile: profiles.get(&m.riot_id).cloned(),
            })
            .collect()
    };
    let blue = build(&teams.blue_team);
    let red = build(&teams.red_team);

    {
        let mut cache = signals.lobby.lock();
        for p in blue.iter().chain(red.iter()) {
            if let Some(s) = &p.stats {
                cache.player_stats.insert(p.riot_id.clone(), s.clone());
            }
        }
    }

    let blue_ids: Vec<&String> = blue.iter().map(|p| &p.riot_id).collect();
    let red_ids: Vec<&String> = red.iter().map(|p| &p.riot_id).collect();

    let duos = crate::api::duo_synergy(MODE, 2)
        .await
        .map(|r| {
            r.duos
                .into_iter()
                .filter(|d| all_ids.contains(&d.player1) && all_ids.contains(&d.player2))
                .collect::<Vec<_>>()
        })
        .unwrap_or_default();

    let rivals = crate::api::rival_matchup(MODE)
        .await
        .map(|r| {
            r.rivalries
                .into_iter()
                .filter(|v| {
                    (blue_ids.contains(&&v.player1) && red_ids.contains(&&v.player2))
                        || (blue_ids.contains(&&v.player2) && red_ids.contains(&&v.player1))
                })
                .collect::<Vec<_>>()
        })
        .unwrap_or_default();

    {
        let mut cache = signals.lobby.lock();
        cache.duo_synergies = duos.clone();
        cache.rival_matchups = rivals.clone();
    }

    Ok(CustomData {
        phase: teams.phase,
        blue,
        red,
        duos,
        rivals,
    })
}

fn team_card(ui: &mut egui::Ui, title: &str, color: egui::Color32, players: &[PlayerData]) {
    ui::card_with_head(
        ui,
        title,
        |ui| ui::muted(ui, format!("{}명", players.len())),
        |ui| {
            // 제목 아래 팀 색 띠. 카드 두 장을 한눈에 구분하려는 것.
            let (rect, _) = ui.allocate_exact_size(
                egui::vec2(ui.available_width(), 2.0),
                egui::Sense::hover(),
            );
            ui.painter().rect_filled(rect, cr(1), color);
            ui.add_space(8.0);

            if players.is_empty() {
                ui::empty(ui, "플레이어 없음");
                return;
            }
            for p in players {
                ui::card_sunken(ui, |ui| {
                    ui.horizontal(|ui| {
                        if p.is_me {
                            ui::chip(ui, "나", BLUE_50, BLUE_600);
                        }
                        ui.label(ui::txt(p.display(), 14.0, ui::W::Bold, color));
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            if let Some(elo) = p.elo() {
                                ui.label(ui::txt(
                                    format!("Elo {elo:.0}"),
                                    13.0,
                                    ui::W::Semibold,
                                    elo_color(Some(elo)),
                                ));
                            }
                        });
                    });
                    ui.add_space(6.0);
                    match p.stats.as_ref().filter(|s| !s.champion_stats.is_empty()) {
                        None => ui::muted(ui, "내전 기록 없음"),
                        Some(s) => {
                            ui.horizontal(|ui| {
                                for c in s.champion_stats.iter().take(6) {
                                    ui.vertical(|ui| {
                                        ui.spacing_mut().item_spacing.y = 2.0;
                                        champion_icon(ui, c.champion_id, 32.0);
                                        ui.label(ui::txt(
                                            ui::pct(c.win_rate),
                                            11.0,
                                            ui::W::Semibold,
                                            win_rate_color(c.win_rate),
                                        ));
                                        ui::muted(ui, format!("{}판", c.games));
                                    });
                                }
                            });
                        }
                    }
                });
            }
        },
    );
}

/// Elo 차이를 승률로 바꾼다 (표준 Elo 기대 승률).
fn win_probability(mine: f64, theirs: f64) -> f64 {
    1.0 / (1.0 + 10f64.powf(-(mine - theirs) / 400.0))
}

fn average_elo(players: &[PlayerData]) -> (f64, usize) {
    let elos: Vec<f64> = players.iter().filter_map(PlayerData::elo).collect();
    if elos.is_empty() {
        // 표본이 없으면 기준점(1000)으로 둔다. 그래야 승률이 50%로 나온다.
        (1000.0, 0)
    } else {
        (elos.iter().sum::<f64>() / elos.len() as f64, elos.len())
    }
}

fn elo_card(ui: &mut egui::Ui, d: &CustomData) {
    let (blue_avg, blue_n) = average_elo(&d.blue);
    let (red_avg, red_n) = average_elo(&d.red);
    if blue_n == 0 && red_n == 0 {
        return;
    }
    let prob = win_probability(blue_avg, red_avg);
    let blue_pct = (prob * 100.0).round() as i32;
    let diff = (blue_avg - red_avg).round() as i32;

    ui::card_with_head(
        ui,
        "팀 전력 비교",
        |ui| ui::muted(ui, "내전 Elo 기준 기대 승률"),
        |ui| {
            ui.horizontal(|ui| {
                ui::stat(ui, &format!("블루팀 ({blue_n}명)"), &format!("{blue_avg:.0}"), TEAM_BLUE);
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    ui::stat(ui, &format!("레드팀 ({red_n}명)"), &format!("{red_avg:.0}"), TEAM_RED);
                });
            });
            ui.add_space(10.0);
            ui.vertical_centered(|ui| {
                ui.label(ui::txt(
                    format!("{blue_pct}% : {}%", 100 - blue_pct),
                    28.0,
                    ui::W::Bold,
                    GRAY_900,
                ));
                ui::muted(
                    ui,
                    format!("Elo 차이 {}{diff}", if diff >= 0 { "+" } else { "" }),
                );
            });
            ui.add_space(8.0);
            ui::split_bar(ui, prob as f32);
        },
    );
}

fn ranking_card(ui: &mut egui::Ui, d: &CustomData) {
    let mut all: Vec<&PlayerData> = d.blue.iter().chain(d.red.iter()).collect();
    all.retain(|p| p.stats.is_some());
    if all.is_empty() {
        return;
    }
    all.sort_by(|a, b| {
        b.elo()
            .unwrap_or(0.0)
            .partial_cmp(&a.elo().unwrap_or(0.0))
            .unwrap_or(std::cmp::Ordering::Equal)
    });

    ui::card_with_head(
        ui,
        "플레이어 Elo 랭킹",
        |ui| ui::muted(ui, "내전 Elo · 승률 · KDA"),
        |ui| {
            ui::table_row(ui, |ui| {
                ui::cell(ui, 30.0, |ui| ui::th(ui, "#"));
                ui::cell(ui, 140.0, |ui| ui::th(ui, "소환사"));
                ui::cell(ui, 110.0, |ui| ui::th(ui, "솔랭"));
                ui::cell(ui, 60.0, |ui| ui::th(ui, "Elo"));
                ui::cell(ui, 78.0, |ui| ui::th(ui, "전적"));
                ui::cell(ui, 56.0, |ui| ui::th(ui, "승률"));
                ui::cell(ui, 50.0, |ui| ui::th(ui, "KDA"));
            });
            for (i, p) in all.iter().enumerate() {
                let s = p.stats.as_ref().expect("위에서 걸렀다");
                ui::table_row(ui, |ui| {
                    ui::rank_cell(ui, i + 1);
                    ui::cell(ui, 140.0, |ui| {
                        ui.label(ui::txt(
                            p.display(),
                            14.0,
                            if p.is_me { ui::W::Bold } else { ui::W::Semibold },
                            if p.is_me { BLUE_600 } else { GRAY_900 },
                        ))
                    });
                    ui::cell(ui, 110.0, |ui| {
                        match p.profile.as_ref().and_then(|pr| pr.solo_rank.as_ref()) {
                            Some(r) => ui::td(ui, format!("{} {} · {}LP", r.tier, r.rank, r.lp)),
                            None => ui::muted(ui, "—"),
                        }
                    });
                    ui::cell(ui, 60.0, |ui| {
                        ui::num(
                            ui,
                            p.elo().map(|e| format!("{e:.0}")).unwrap_or_else(|| "—".into()),
                            elo_color(p.elo()),
                        )
                    });
                    ui::cell(ui, 78.0, |ui| {
                        ui::td(ui, format!("{}승 {}패", s.wins, s.losses))
                    });
                    ui::cell(ui, 56.0, |ui| {
                        ui::num(ui, ui::pct(s.win_rate), win_rate_color(s.win_rate))
                    });
                    ui::cell(ui, 50.0, |ui| ui::td(ui, format!("{:.2}", s.kda)));
                });
            }
        },
    );
}

fn duo_card(
    ui: &mut egui::Ui,
    title: &str,
    color: egui::Color32,
    all_duos: &[DuoSynergy],
    team_ids: &[String],
) {
    let mut duos: Vec<&DuoSynergy> = all_duos
        .iter()
        .filter(|d| team_ids.contains(&d.player1) && team_ids.contains(&d.player2))
        .collect();
    if duos.is_empty() {
        return;
    }
    duos.sort_by(|a, b| {
        b.win_rate
            .partial_cmp(&a.win_rate)
            .unwrap_or(std::cmp::Ordering::Equal)
    });

    ui::card(ui, |ui| {
        ui.label(ui::txt(title, 16.0, ui::W::Bold, color));
        ui::muted(ui, "함께 높은 승률을 기록한 조합");
        ui.add_space(8.0);
        for duo in duos.iter().take(5) {
            ui::table_row(ui, |ui| {
                ui::cell(ui, 170.0, |ui| {
                    ui::td(
                        ui,
                        format!(
                            "{} + {}",
                            short_name(&duo.player1),
                            short_name(&duo.player2)
                        ),
                    )
                });
                ui::cell(ui, 56.0, |ui| {
                    ui::num(ui, ui::pct(duo.win_rate), win_rate_color(duo.win_rate))
                });
                ui::cell(ui, 48.0, |ui| ui::muted(ui, format!("{}판", duo.games)));
            });
        }
    });
}

fn rival_card(ui: &mut egui::Ui, rivals: &[RivalEntry]) {
    let mut sorted: Vec<&RivalEntry> = rivals.iter().collect();
    sorted.sort_by_key(|r| std::cmp::Reverse(r.games));

    ui::card_with_head(
        ui,
        "라이벌 매치업",
        |ui| ui::muted(ui, "블루 vs 레드 개인 대결 기록"),
        |ui| {
            for r in sorted.iter().take(8) {
                ui::table_row(ui, |ui| {
                    ui::cell(ui, 130.0, |ui| {
                        ui.label(ui::txt(short_name(&r.player1), 14.0, ui::W::Semibold, TEAM_BLUE))
                    });
                    ui::cell(ui, 26.0, |ui| ui::muted(ui, "vs"));
                    ui::cell(ui, 130.0, |ui| {
                        ui.label(ui::txt(short_name(&r.player2), 14.0, ui::W::Semibold, TEAM_RED))
                    });
                    ui::cell(ui, 100.0, |ui| {
                        ui::num(ui, format!("{} - {}", r.player1_wins, r.player2_wins), GRAY_900)
                    });
                    ui::cell(ui, 56.0, |ui| ui::muted(ui, format!("{}판", r.games)));
                });
            }
        },
    );
}

fn top_champs_card(ui: &mut egui::Ui, d: &CustomData) {
    ui::card_with_head(
        ui,
        "플레이어별 Top 3 챔피언",
        |ui| ui::muted(ui, "내전 기준 모스트"),
        |ui| {
            for (p, color) in d
                .blue
                .iter()
                .map(|p| (p, TEAM_BLUE))
                .chain(d.red.iter().map(|p| (p, TEAM_RED)))
            {
                ui::table_row(ui, |ui| {
                    ui::cell(ui, 130.0, |ui| {
                        ui.label(ui::txt(p.display(), 14.0, ui::W::Semibold, color))
                    });
                    match p.stats.as_ref().filter(|s| !s.champion_stats.is_empty()) {
                        None => ui::muted(ui, "기록 없음"),
                        Some(s) => {
                            for c in s.champion_stats.iter().take(3) {
                                champion_icon(ui, c.champion_id, 24.0);
                                ui.label(ui::txt(
                                    format!("{} {}", ui::pct(c.win_rate), c.games),
                                    12.0,
                                    ui::W::Regular,
                                    win_rate_color(c.win_rate),
                                ));
                                ui.add_space(6.0);
                            }
                        }
                    }
                });
            }
        },
    );
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn win_probability_is_symmetric_and_monotonic() {
        assert!((win_probability(1000.0, 1000.0) - 0.5).abs() < 1e-9);
        // 400점 차이는 10:1, 즉 약 90.9%.
        let p = win_probability(1400.0, 1000.0);
        assert!((p - 0.909).abs() < 0.001, "{p}");
        assert!((win_probability(1000.0, 1400.0) - (1.0 - p)).abs() < 1e-9);
    }

    #[test]
    fn average_elo_falls_back_to_baseline() {
        let no_stats = vec![PlayerData::default()];
        assert_eq!(average_elo(&no_stats), (1000.0, 0));

        let with_stats = vec![
            PlayerData {
                stats: Some(PlayerStats {
                    elo: Some(1200.0),
                    ..Default::default()
                }),
                ..Default::default()
            },
            PlayerData {
                stats: Some(PlayerStats {
                    elo: Some(1000.0),
                    ..Default::default()
                }),
                ..Default::default()
            },
            // NaN 은 평균을 오염시키므로 걸러져야 한다.
            PlayerData {
                stats: Some(PlayerStats {
                    elo: Some(f64::NAN),
                    ..Default::default()
                }),
                ..Default::default()
            },
        ];
        assert_eq!(average_elo(&with_stats), (1100.0, 2));
    }

    #[test]
    fn display_falls_back_to_riot_id() {
        let p = PlayerData {
            riot_id: "홍길동#KR1".to_owned(),
            ..Default::default()
        };
        assert_eq!(p.display(), "홍길동");

        let p = PlayerData {
            summoner_name: "표시이름".to_owned(),
            riot_id: "홍길동#KR1".to_owned(),
            ..Default::default()
        };
        assert_eq!(p.display(), "표시이름");
    }
}
