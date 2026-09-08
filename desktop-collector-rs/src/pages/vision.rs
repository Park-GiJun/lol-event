//! 시야 분석 — 시야 점수 랭킹 · 와드 설치/제거 · 제어 와드.

use crate::models::{as_percent, VisionDominanceResult};
use crate::net::Remote;
use crate::theme::*;
use crate::ui;

use super::{slot_body, MODE};

#[derive(Default)]
pub struct VisionPage {
    data: Remote<VisionDominanceResult>,
}

impl VisionPage {
    pub fn ui(&mut self, ui: &mut egui::Ui) {
        let ctx = ui.ctx().clone();
        self.data.ensure(&ctx, crate::api::vision_dominance(MODE));

        ui::page(
            ui,
            "시야 분석",
            "시야 점수 랭킹 · 와드 설치/제거 · 제어 와드",
            |ui| {
                if super::refresh_button(ui, self.data.lock().loading) {
                    self.data.reload(&ctx, crate::api::vision_dominance(MODE));
                }

                ui::card_with_head(
                    ui,
                    "시야 점수 랭킹",
                    |ui| ui::muted(ui, "내전 기록 기반 평균"),
                    |ui| {
                        let slot = self.data.lock();
                        slot_body(
                            ui,
                            &slot,
                            "시야 데이터가 없습니다",
                            |d| d.players.is_empty(),
                            |ui, data| {
                                let mut players = data.players.clone();
                                players.sort_by(|a, b| {
                                    b.avg_vision_score
                                        .partial_cmp(&a.avg_vision_score)
                                        .unwrap_or(std::cmp::Ordering::Equal)
                                });

                                ui::table_row(ui, |ui| {
                                    ui::cell(ui, 30.0, |ui| ui::th(ui, "#"));
                                    ui::cell(ui, 150.0, |ui| ui::th(ui, "소환사"));
                                    ui::cell(ui, 76.0, |ui| ui::th(ui, "시야 점수"));
                                    ui::cell(ui, 76.0, |ui| ui::th(ui, "와드 설치"));
                                    ui::cell(ui, 76.0, |ui| ui::th(ui, "와드 제거"));
                                    ui::cell(ui, 76.0, |ui| ui::th(ui, "제어 와드"));
                                    ui::cell(ui, 76.0, |ui| ui::th(ui, "제거율"));
                                    ui::cell(ui, 56.0, |ui| ui::th(ui, "판수"));
                                });

                                for (i, p) in players.iter().enumerate() {
                                    // 표본이 한두 판이면 평균이 튄다. 순위 자체를 믿기 어렵다.
                                    let thin = p.games < 3;
                                    let score_color = if thin {
                                        GRAY_500
                                    } else if p.avg_vision_score >= 60.0 {
                                        BLUE_600
                                    } else if p.avg_vision_score >= 35.0 {
                                        BLUE_500
                                    } else {
                                        GRAY_600
                                    };
                                    ui::table_row(ui, |ui| {
                                        ui::rank_cell(ui, i + 1);
                                        ui::cell(ui, 150.0, |ui| {
                                            ui::person(ui, &p.riot_id, false)
                                        });
                                        ui::cell(ui, 76.0, |ui| {
                                            ui::num(
                                                ui,
                                                format!("{:.1}", p.avg_vision_score),
                                                score_color,
                                            )
                                        });
                                        ui::cell(ui, 76.0, |ui| {
                                            ui::td(ui, format!("{:.1}", p.avg_wards_placed))
                                        });
                                        ui::cell(ui, 76.0, |ui| {
                                            ui::td(ui, format!("{:.1}", p.avg_wards_killed))
                                        });
                                        ui::cell(ui, 76.0, |ui| {
                                            ui::num(
                                                ui,
                                                format!("{:.1}", p.avg_control_wards_bought),
                                                BLUE_500,
                                            )
                                        });
                                        ui::cell(ui, 76.0, |ui| {
                                            ui::muted(ui, ui::pct(as_percent(p.ward_kill_rate)))
                                        });
                                        ui::cell(ui, 56.0, |ui| {
                                            if thin {
                                                ui::chip(
                                                    ui,
                                                    &format!("{}판", p.games),
                                                    WARN_BG,
                                                    WARN_FG,
                                                );
                                            } else {
                                                ui::muted(ui, format!("{}판", p.games));
                                            }
                                        });
                                    });
                                }
                            },
                        );
                    },
                );
            },
        );
    }
}
