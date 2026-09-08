//! 데미지 분석 — 물리/마법/트루 딜 비율 · 탱킹량 · 포탑 딜.

use crate::models::{DamageAnalysisResult, DamagePlayerEntry};
use crate::net::Remote;
use crate::theme::*;
use crate::ui;

use super::{slot_body, MODE};

#[derive(Default)]
pub struct DamagePage {
    data: Remote<DamageAnalysisResult>,
}

impl DamagePage {
    pub fn ui(&mut self, ui: &mut egui::Ui) {
        let ctx = ui.ctx().clone();
        self.data.ensure(&ctx, crate::api::damage_analysis(MODE));

        ui::page(
            ui,
            "데미지 분석",
            "물리 / 마법 / 트루 딜 비율 · 탱킹량 · 포탑 딜",
            |ui| {
                if super::refresh_button(ui, self.data.lock().loading) {
                    self.data.reload(&ctx, crate::api::damage_analysis(MODE));
                }

                let slot = self.data.lock();
                slot_body(
                    ui,
                    &slot,
                    "데미지 데이터가 없습니다",
                    |d| d.players.is_empty(),
                    |ui, data| {
                        let mut players = data.players.clone();
                        players.sort_by(|a, b| {
                            b.avg_total
                                .partial_cmp(&a.avg_total)
                                .unwrap_or(std::cmp::Ordering::Equal)
                        });
                        for p in &players {
                            player_card(ui, p);
                        }
                    },
                );
            },
        );
    }
}

/// 딜 성향 배지. 파랑/빨강 축으로 못 나누는 값이라 보조색을 쓴다.
fn profile_chip(ui: &mut egui::Ui, profile: &str) {
    const PURPLE_BG: egui::Color32 = egui::Color32::from_rgb(0xF1, 0xEC, 0xFD);
    const PURPLE_FG: egui::Color32 = egui::Color32::from_rgb(0x6D, 0x45, 0xC7);
    const TANK_BG: egui::Color32 = egui::Color32::from_rgb(0xE6, 0xF7, 0xF1);
    const TANK_FG: egui::Color32 = egui::Color32::from_rgb(0x00, 0x8A, 0x60);

    match profile.to_uppercase().as_str() {
        "AD" => ui::chip(ui, profile, BLUE_50, BLUE_600),
        "AP" | "HYBRID" => ui::chip(ui, profile, PURPLE_BG, PURPLE_FG),
        "TANK" => ui::chip(ui, profile, TANK_BG, TANK_FG),
        _ => ui::chip_neutral(ui, profile),
    }
}

fn player_card(ui: &mut egui::Ui, p: &DamagePlayerEntry) {
    ui::card(ui, |ui| {
        ui.horizontal(|ui| {
            ui::person(ui, &p.riot_id, false);
            if !p.damage_profile.is_empty() {
                profile_chip(ui, &p.damage_profile);
            }
            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                ui::muted(ui, format!("{}판", p.games));
            });
        });
        ui.add_space(10.0);

        let bar_width = (ui.available_width() - 230.0).max(120.0);
        for (label, avg, rate, color) in [
            ("물리 딜", p.avg_physical, p.physical_rate, BLUE_600),
            ("마법 딜", p.avg_magic, p.magic_rate, BLUE_300),
            ("트루 딜", p.avg_true, p.true_rate, GRAY_400),
        ] {
            damage_row(ui, label, avg, rate, color, bar_width);
        }

        ui.add_space(10.0);
        ui.horizontal(|ui| {
            ui.spacing_mut().item_spacing.x = 28.0;
            ui::stat(ui, "받은 피해", &ui::short_num(p.avg_mitigated), GRAY_900);
            ui::stat(ui, "포탑 딜", &ui::short_num(p.avg_turret_dmg), GRAY_900);
            ui::stat(ui, "총 딜", &ui::short_num(p.avg_total), BLUE_600);
        });
    });
}

fn damage_row(
    ui: &mut egui::Ui,
    label: &str,
    avg: f64,
    rate: f64,
    color: egui::Color32,
    bar_width: f32,
) {
    // 백엔드는 0~1 비율로 준다.
    let pct = crate::models::as_percent(rate);
    ui.horizontal(|ui| {
        ui::cell(ui, 56.0, |ui| {
            ui.label(ui::txt(label, 12.0, ui::W::Regular, GRAY_600))
        });
        ui::bar(ui, (pct / 100.0) as f32, bar_width, color);
        ui::cell(ui, 52.0, |ui| ui::num(ui, ui::pct(pct), color));
        ui::cell(ui, 60.0, |ui| ui::muted(ui, ui::short_num(avg)));
    });
}
