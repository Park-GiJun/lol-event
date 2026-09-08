//! 서렌더 분석 — 전체 서렌더율 · 조기 서렌더 · 플레이어별 비율.

use crate::models::{as_percent, SurrenderAnalysisResult};
use crate::net::Remote;
use crate::theme::*;
use crate::ui;

use super::MODE;

#[derive(Default)]
pub struct SurrenderPage {
    data: Remote<SurrenderAnalysisResult>,
}

impl SurrenderPage {
    pub fn ui(&mut self, ui: &mut egui::Ui) {
        let ctx = ui.ctx().clone();
        self.data.ensure(&ctx, crate::api::surrender_analysis(MODE));

        ui::page(
            ui,
            "서렌더 분석",
            "전체 서렌더율 · 조기 서렌더 · 플레이어별 비율",
            |ui| {
                if super::refresh_button(ui, self.data.lock().loading) {
                    self.data.reload(&ctx, crate::api::surrender_analysis(MODE));
                }

                let slot = self.data.lock();
                let Some(data) = slot.value.as_ref() else {
                    ui::card(ui, |ui| match &slot.error {
                        Some(e) => ui::error(ui, e),
                        None => ui::empty(ui, "불러오는 중..."),
                    });
                    return;
                };

                summary_card(ui, data);
                player_card(ui, data);
            },
        );
    }
}

fn summary_card(ui: &mut egui::Ui, data: &SurrenderAnalysisResult) {
    // 백엔드는 0~1 비율로 준다.
    let surrender = as_percent(data.overall_surrender_rate);
    let early = as_percent(data.overall_early_surrender_rate);

    ui::card_with_head(
        ui,
        "전체 서렌더 통계",
        |ui| ui::muted(ui, format!("{}경기 기준", data.total_games)),
        |ui| {
            ui.horizontal(|ui| {
                ui.spacing_mut().item_spacing.x = 36.0;
                ui::stat_hero(
                    ui,
                    "전체 서렌더율",
                    &ui::pct(surrender),
                    if surrender >= 50.0 { LOSS } else { BLUE_600 },
                );
                ui::stat(
                    ui,
                    "서렌더 경기",
                    &format!("{}경기", data.surrender_games),
                    GRAY_900,
                );
                if data.early_surrender_games > 0 || early > 0.0 {
                    ui::stat(
                        ui,
                        "조기 서렌더",
                        &format!("{}경기 · {}", data.early_surrender_games, ui::pct(early)),
                        if early >= 30.0 { WARN_FG } else { GRAY_900 },
                    );
                }
            });
            ui.add_space(14.0);

            ui.horizontal(|ui| {
                ui.label(ui::txt("끝까지 싸움", 12.0, ui::W::Semibold, BLUE_600));
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    ui.label(ui::txt("서렌더", 12.0, ui::W::Semibold, LOSS));
                });
            });
            // 왼쪽(파랑)이 "끝까지 싸운 비율".
            ui::split_bar(ui, (1.0 - surrender / 100.0) as f32);
        },
    );
}

fn player_card(ui: &mut egui::Ui, data: &SurrenderAnalysisResult) {
    if data.players.is_empty() {
        return;
    }
    ui::card_with_head(
        ui,
        "플레이어별 서렌더 비율",
        |ui| ui::muted(ui, "서렌더로 끝난 경기 / 전체 경기"),
        |ui| {
            let mut players = data.players.clone();
            players.sort_by(|a, b| {
                b.surrender_rate
                    .partial_cmp(&a.surrender_rate)
                    .unwrap_or(std::cmp::Ordering::Equal)
            });

            ui::table_row(ui, |ui| {
                ui::cell(ui, 30.0, |ui| ui::th(ui, "#"));
                ui::cell(ui, 170.0, |ui| ui::th(ui, "소환사"));
                ui::cell(ui, 130.0, |ui| ui::th(ui, "서렌더율"));
                ui::cell(ui, 110.0, |ui| ui::th(ui, "서렌더/전체"));
                ui::cell(ui, 90.0, |ui| ui::th(ui, "조기 서렌더"));
            });

            for (i, p) in players.iter().enumerate() {
                let rate = as_percent(p.surrender_rate);
                // 표본이 적으면 비율이 0%/100% 로 튄다. 색을 죽여 눈에 덜 띄게 한다.
                let thin = p.games < 5;
                let color = if thin {
                    GRAY_500
                } else if rate >= 30.0 {
                    LOSS
                } else if rate <= 5.0 {
                    BLUE_600
                } else {
                    GRAY_700
                };
                ui::table_row(ui, |ui| {
                    ui::rank_cell(ui, i + 1);
                    ui::cell(ui, 170.0, |ui| ui::person(ui, &p.riot_id, false));
                    ui::cell(ui, 130.0, |ui| {
                        ui::bar(ui, (rate / 100.0) as f32, 62.0, color);
                        ui::num(ui, ui::pct(rate), color);
                    });
                    ui::cell(ui, 110.0, |ui| {
                        if thin {
                            ui::chip(
                                ui,
                                &format!("{}/{}", p.surrender_games, p.games),
                                WARN_BG,
                                WARN_FG,
                            );
                        } else {
                            ui::td(ui, format!("{}/{}", p.surrender_games, p.games));
                        }
                    });
                    ui::cell(ui, 90.0, |ui| {
                        if p.early_surrender_games > 0 {
                            ui::muted(ui, format!("{}회", p.early_surrender_games));
                        } else {
                            ui::muted(ui, "—");
                        }
                    });
                });
            }
        },
    );
}
