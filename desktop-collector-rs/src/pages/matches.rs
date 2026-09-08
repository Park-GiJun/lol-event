//! 경기 기록 — 최근 내전 목록. 행을 누르면 양 팀 상세가 펼쳐진다.

use std::collections::HashSet;

use crate::icons::champion_icon;
use crate::models::{MatchDetail, MatchPageResult, MatchParticipant};
use crate::net::Remote;
use crate::theme::*;
use crate::ui;

use super::{queue_label, slot_body, MODE};

#[derive(Default)]
pub struct MatchesPage {
    data: Remote<MatchPageResult>,
    expanded: HashSet<String>,
}

impl MatchesPage {
    pub fn ui(&mut self, ui: &mut egui::Ui) {
        let ctx = ui.ctx().clone();
        self.data.ensure(&ctx, crate::api::recent_matches(MODE, 30));

        let mut toggle: Option<String> = None;

        ui::page(ui, "경기 기록", "최근 내전 경기 결과", |ui| {
            let loading = self.data.lock().loading;
            let clicked = ui
                .horizontal(|ui| {
                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        ui::secondary_button(
                            ui,
                            if loading { "불러오는 중..." } else { "새로고침" },
                            !loading,
                        )
                        .clicked()
                    })
                    .inner
                })
                .inner;
            if clicked {
                self.data.reload(&ctx, crate::api::recent_matches(MODE, 30));
            }

            let slot = self.data.lock();
            slot_body(
                ui,
                &slot,
                "경기 기록이 없습니다",
                |d| d.matches.is_empty(),
                |ui, data| {
                    for m in &data.matches {
                        let open = self.expanded.contains(&m.match_id);
                        if match_card(ui, m, open) {
                            toggle = Some(m.match_id.clone());
                        }
                    }
                },
            );
        });

        if let Some(id) = toggle {
            if !self.expanded.remove(&id) {
                self.expanded.insert(id);
            }
        }
    }
}

/// 클릭되면 true.
fn match_card(ui: &mut egui::Ui, m: &MatchDetail, expanded: bool) -> bool {
    let blue: Vec<&MatchParticipant> = m.participants.iter().filter(|p| p.team == "blue").collect();
    let red: Vec<&MatchParticipant> = m.participants.iter().filter(|p| p.team == "red").collect();
    let blue_win = blue.first().map(|p| p.win).unwrap_or(false);

    let mut clicked = false;
    ui::result_frame(ui, blue_win, |ui| {
        let head = ui.horizontal(|ui| {
            ui.spacing_mut().item_spacing.x = 6.0;
            for p in &blue {
                champion_icon(ui, p.champion_id, 28.0);
            }
            ui.add_space(8.0);
            if blue_win {
                ui::chip_win(ui, "블루 승");
            } else {
                ui::chip_loss(ui, "블루 패");
            }
            ui.add_space(8.0);
            for p in &red {
                champion_icon(ui, p.champion_id, 28.0);
            }

            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                ui.label(ui::txt(
                    if expanded { "접기" } else { "펼치기" },
                    12.0,
                    ui::W::Semibold,
                    GRAY_500,
                ));
                ui.add_space(8.0);
                ui.vertical(|ui| {
                    ui.spacing_mut().item_spacing.y = 1.0;
                    ui.label(ui::txt(ui::date_kst(m.game_creation), 12.0, ui::W::Regular, GRAY_600));
                    ui.label(ui::txt(
                        format!("{} · {}", ui::mmss(m.game_duration as i64), queue_label(m.queue_id)),
                        11.0,
                        ui::W::Regular,
                        GRAY_500,
                    ));
                });
            });
        });
        if head.response.interact(egui::Sense::click()).clicked() {
            clicked = true;
        }

        if expanded {
            ui.add_space(10.0);
            ui.painter().hline(
                ui.min_rect().x_range(),
                ui.cursor().top(),
                egui::Stroke::new(1.0, GRAY_100),
            );
            ui.add_space(10.0);
            detail(ui, m, &blue, &red, blue_win);
        }
    });
    clicked
}

/// 이긴 팀의 MVP, 진 팀의 ACE 를 고른다. 웹 계산식과 같다.
fn mvp_score(p: &MatchParticipant) -> f64 {
    let kda = if p.deaths == 0 {
        (p.kills + p.assists) as f64 * 1.5
    } else {
        (p.kills + p.assists) as f64 / p.deaths as f64
    };
    kda * 0.4 + p.damage as f64 / 1000.0 * 0.3 + p.cs as f64 * 0.1 + p.gold as f64 / 1000.0 * 0.2
}

fn best<'a>(players: impl Iterator<Item = &'a MatchParticipant>) -> Option<String> {
    players
        .max_by(|a, b| {
            mvp_score(a)
                .partial_cmp(&mvp_score(b))
                .unwrap_or(std::cmp::Ordering::Equal)
        })
        .map(|p| p.riot_id.clone())
}

fn detail(
    ui: &mut egui::Ui,
    m: &MatchDetail,
    blue: &[&MatchParticipant],
    red: &[&MatchParticipant],
    blue_win: bool,
) {
    let mvp = best(m.participants.iter().filter(|p| p.win));
    let ace = best(m.participants.iter().filter(|p| !p.win));

    ui::two_columns(
        ui,
        |ui| team_column(ui, "블루팀", TEAM_BLUE, blue_win, blue, &mvp, &ace),
        |ui| team_column(ui, "레드팀", TEAM_RED, !blue_win, red, &mvp, &ace),
    );
}

fn team_column(
    ui: &mut egui::Ui,
    title: &str,
    color: egui::Color32,
    won: bool,
    players: &[&MatchParticipant],
    mvp: &Option<String>,
    ace: &Option<String>,
) {
    ui.horizontal(|ui| {
        ui.label(ui::txt(title, 14.0, ui::W::Bold, color));
        ui.label(ui::txt(
            if won { "승리" } else { "패배" },
            13.0,
            ui::W::Semibold,
            if won { WIN } else { LOSS },
        ));
    });
    ui.add_space(4.0);

    ui::table_row(ui, |ui| {
        ui::cell(ui, 26.0, |_ui| {});
        ui::cell(ui, 120.0, |ui| ui::th(ui, "소환사"));
        ui::cell(ui, 70.0, |ui| ui::th(ui, "KDA"));
        ui::cell(ui, 52.0, |ui| ui::th(ui, "딜량"));
        ui::cell(ui, 40.0, |ui| ui::th(ui, "CS"));
    });

    for p in players {
        let is_mvp = mvp.as_deref() == Some(p.riot_id.as_str());
        let is_ace = ace.as_deref() == Some(p.riot_id.as_str());
        ui::table_row(ui, |ui| {
            ui::cell(ui, 26.0, |ui| {
                champion_icon(ui, p.champion_id, 22.0);
            });
            ui::cell(ui, 120.0, |ui| {
                ui.label(ui::txt(
                    crate::models::short_name(&p.riot_id),
                    13.0,
                    ui::W::Semibold,
                    GRAY_900,
                ));
                if is_mvp {
                    ui::chip_blue(ui, "MVP");
                } else if is_ace {
                    ui::chip_neutral(ui, "ACE");
                }
            });
            ui::cell(ui, 70.0, |ui| {
                ui::td(ui, format!("{}/{}/{}", p.kills, p.deaths, p.assists))
            });
            ui::cell(ui, 52.0, |ui| {
                ui::muted(ui, ui::short_num(p.damage as f64))
            });
            ui::cell(ui, 40.0, |ui| ui::muted(ui, p.cs.to_string()));
        });
    }
}
