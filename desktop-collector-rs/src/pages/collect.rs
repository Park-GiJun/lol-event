//! 매치 수집 — LCU 에서 내전 데이터를 긁어 서버로 보낸다.
//!
//! 게임이 끝나면 [`crate::monitor`] 가 알아서 돌리므로, 이 화면의 버튼은
//! "지금 당장" 이 필요할 때만 쓴다.

use crate::collect::LogKind;
use crate::monitor::Signals;
use crate::theme::*;
use crate::ui;

#[derive(Default)]
pub struct CollectPage {}

impl CollectPage {
    pub fn ui(&mut self, ui: &mut egui::Ui, signals: &Signals) {
        let ctx = ui.ctx().clone();
        let connected = signals.lcu_status.lock().connected;
        let collecting = crate::collect::is_collecting();

        ui::page(
            ui,
            "매치 수집",
            "LCU에서 내전 데이터를 수집해 서버로 전송합니다",
            |ui| {
                let auto_status = signals.auto_status.get();
                if !auto_status.is_empty() {
                    ui::banner(ui, &auto_status, BLUE_600, BLUE_50, |_ui| {});
                }

                let dodges = *signals.dodge_count.lock();
                if dodges > 0 {
                    ui::banner(
                        ui,
                        &format!("닷지 감지: {dodges}회"),
                        WARN_FG,
                        WARN_BG,
                        |ui| ui::muted(ui, "이번 세션 기준"),
                    );
                }

                ui::card(ui, |ui| {
                    ui.horizontal(|ui| {
                        ui.vertical(|ui| {
                            ui.spacing_mut().item_spacing.y = 2.0;
                            ui.label(ui::txt(
                                if connected {
                                    "롤 클라이언트 연결됨"
                                } else {
                                    "롤 클라이언트를 실행해주세요"
                                },
                                15.0,
                                ui::W::Semibold,
                                if connected { GRAY_900 } else { GRAY_600 },
                            ));
                            ui::muted(
                                ui,
                                "게임이 끝나면 30초 뒤에 자동으로 수집합니다. 아래 버튼은 즉시 실행용입니다.",
                            );
                        });
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            let label = if collecting { "수집 중..." } else { "지금 수집" };
                            if ui::primary_button(ui, label, connected && !collecting).clicked() {
                                signals.logs.lock().clear();
                                let s = signals.clone();
                                let c = ctx.clone();
                                crate::net::spawn(async move {
                                    crate::monitor::run_collect(s, c).await;
                                });
                            }
                        });
                    });
                });

                ui::card_with_head(
                    ui,
                    "수집 로그",
                    |ui| {
                        if ui::secondary_button(ui, "지우기", true).clicked() {
                            signals.logs.lock().clear();
                        }
                    },
                    |ui| {
                        let logs = signals.logs.lock();
                        if logs.is_empty() {
                            ui::empty(
                                ui,
                                if connected {
                                    "대기 중입니다"
                                } else {
                                    "롤 클라이언트를 실행해주세요"
                                },
                            );
                            return;
                        }
                        egui::ScrollArea::vertical()
                            .max_height(360.0)
                            .stick_to_bottom(true)
                            .auto_shrink([false, true])
                            .show(ui, |ui| {
                                ui.spacing_mut().item_spacing.y = 2.0;
                                for line in logs.iter() {
                                    let (color, weight) = match line.kind {
                                        LogKind::Done => (BLUE_600, ui::W::Semibold),
                                        LogKind::Error => (LOSS, ui::W::Semibold),
                                        LogKind::Warn => (WARN_FG, ui::W::Regular),
                                        LogKind::Progress => (GRAY_500, ui::W::Regular),
                                        LogKind::Info => (GRAY_700, ui::W::Regular),
                                    };
                                    ui.label(ui::txt(&line.message, 13.0, weight, color));
                                }
                            });
                    },
                );
            },
        );
    }
}

