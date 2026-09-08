//! 챔피언 선택 — 상대 분석 · 밴 추천 · 카운터픽 · 룬 자동 적용.
//!
//! 챔피언 선택에 들어가면 LCU 가 상대팀 `summonerId` 를 가린다. 그래서 로비에서
//! 받아 둔 [`crate::monitor::LobbyCache`] 를 상대팀 자리에 대신 끼워 넣는다.

use std::collections::HashMap;
use std::time::{Duration, Instant};

use crate::icons::champion_icon;
use crate::lcu::{ChampSelectFull, ChampSelectSlot};
use crate::models::*;
use crate::monitor::Signals;
use crate::net::{Remote, Shared};
use crate::theme::*;
use crate::ui;

use super::{position_label, MODE};

const REFRESH: Duration = Duration::from_secs(2);

#[derive(Clone, Copy, PartialEq, Eq)]
enum Tab {
    Ban,
    Pick,
    Counter,
    Tier,
}

#[derive(Clone, Default)]
struct SelectData {
    session: Option<ChampSelectFull>,
    /// riotId → 내전 통계.
    stats: HashMap<String, PlayerStats>,
    /// 상대팀이 가려져 로비 캐시를 대신 쓰고 있는가.
    cache_active: bool,
    cached_enemies: Vec<String>,
}

pub struct ChampSelectPage {
    data: Remote<SelectData>,
    duos: Remote<DuoSynergyResult>,
    rivals: Remote<RivalMatchupResult>,
    bans: Remote<BanAnalysisResult>,
    tiers: Remote<ChampionTierResult>,
    counters: Remote<HashMap<i32, MatchupResult>>,
    runes: Remote<Option<RuneSuggestion>>,
    /// 마지막으로 룬을 조회한 챔피언. 챔피언이 바뀌면 다시 받는다.
    runes_for: Option<i32>,
    rune_status: Shared<String>,
    tab: Tab,
    last_refresh: Instant,
    counters_for: String,
}

impl Default for ChampSelectPage {
    fn default() -> Self {
        Self {
            data: Remote::new(),
            duos: Remote::new(),
            rivals: Remote::new(),
            bans: Remote::new(),
            tiers: Remote::new(),
            counters: Remote::new(),
            runes: Remote::new(),
            runes_for: None,
            rune_status: Shared::new(String::new()),
            tab: Tab::Ban,
            last_refresh: Instant::now() - REFRESH,
            counters_for: String::new(),
        }
    }
}

impl ChampSelectPage {
    pub fn ui(&mut self, ui: &mut egui::Ui, signals: &Signals) {
        let ctx = ui.ctx().clone();

        self.duos.ensure(&ctx, crate::api::duo_synergy(MODE, 2));
        self.rivals.ensure(&ctx, crate::api::rival_matchup(MODE));
        self.bans.ensure(&ctx, crate::api::ban_analysis(MODE));
        self.tiers.ensure(&ctx, crate::api::champion_tier(MODE, 3));

        if self.last_refresh.elapsed() >= REFRESH {
            self.last_refresh = Instant::now();
            self.data.reload(&ctx, load(signals.clone()));
        }
        ctx.request_repaint_after(Duration::from_millis(500));

        let data = self.data.lock().value.clone().unwrap_or_default();

        self.sync_counters(&ctx, &data);
        self.sync_runes(&ctx, &data);

        ui::page(ui, "챔피언 선택", "상대 분석 · 밴 추천 · 카운터픽", |ui| {
            let Some(session) = data.session.clone() else {
                ui::card(ui, |ui| {
                    ui::empty(ui, "챔피언 선택 화면이 아닙니다")
                });
                return;
            };

            ui::card(ui, |ui| {
                ui.horizontal(|ui| {
                    if !session.phase.is_empty() {
                        ui::chip_blue(ui, &session.phase);
                    }
                    if session.timer > 0 {
                        ui::chip_neutral(ui, &format!("{}초", session.timer));
                    }
                    if data.cache_active {
                        ui::chip(
                            ui,
                            &format!("로비 캐시 — 상대 {}명", data.cached_enemies.len()),
                            WIN_BG,
                            WIN,
                        );
                    }
                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        ui::muted(ui, "2초마다 자동 갱신");
                    });
                });
            });

            ui::two_columns(
                ui,
                |ui| {
                    team_card(
                        ui,
                        "우리팀",
                        TEAM_BLUE,
                        &session.my_team.iter().map(|s| s.riot_id.clone()).collect::<Vec<_>>(),
                        &data.stats,
                        &session.my_team,
                    )
                },
                |ui| {
                    let enemy_ids: Vec<String> = if data.cache_active {
                        data.cached_enemies.clone()
                    } else {
                        session.their_team.iter().map(|s| s.riot_id.clone()).collect()
                    };
                    team_card(ui, "상대팀", TEAM_RED, &enemy_ids, &data.stats, &session.their_team)
                },
            );

            ui::card(ui, |ui| {
                ui::tabs(
                    ui,
                    &mut self.tab,
                    &[
                        (Tab::Ban, "밴 추천"),
                        (Tab::Pick, "내 픽 추천"),
                        (Tab::Counter, "카운터픽"),
                        (Tab::Tier, "챔피언 티어"),
                    ],
                );
                match self.tab {
                    Tab::Ban => self.ban_tab(ui, &data, &session),
                    Tab::Pick => self.pick_tab(ui, &data, &session),
                    Tab::Counter => self.counter_tab(ui, &session),
                    Tab::Tier => self.tier_tab(ui),
                }
            });

            self.team_strength(ui, &data, &session);
            self.rune_card(ui, &session, &data);
            self.duo_card(ui, &session);
            self.rival_card(ui, &data, &session);

            if !session.bans.is_empty() {
                ui::card_with_head(
                    ui,
                    "밴 목록",
                    |ui| ui::muted(ui, format!("{}개", session.bans.len())),
                    |ui| {
                        ui.horizontal_wrapped(|ui| {
                            for ban in &session.bans {
                                let color = if ban.team == "blue" { TEAM_BLUE } else { TEAM_RED };
                                ui.vertical(|ui| {
                                    ui.spacing_mut().item_spacing.y = 2.0;
                                    champion_icon(ui, ban.champion_id, 32.0);
                                    let (rect, _) = ui.allocate_exact_size(
                                        egui::vec2(32.0, 3.0),
                                        egui::Sense::hover(),
                                    );
                                    ui.painter().rect_filled(rect, cr(2), color);
                                });
                            }
                        });
                    },
                );
            }
        });
    }

    /// 상대 챔피언이 바뀌면 카운터 매치업을 다시 받는다.
    fn sync_counters(&mut self, ctx: &egui::Context, data: &SelectData) {
        let Some(session) = data.session.as_ref() else {
            return;
        };
        let key = session
            .their_team
            .iter()
            .map(|s| s.champion_id)
            .filter(|c| *c > 0)
            .map(|c| c.to_string())
            .collect::<Vec<_>>()
            .join(",");
        if key.is_empty() || key == self.counters_for {
            return;
        }
        self.counters_for = key;
        let ids: Vec<i32> = session
            .their_team
            .iter()
            .map(|s| s.champion_id)
            .filter(|c| *c > 0)
            .collect();
        self.counters.reload(ctx, load_counters(ids));
    }

    /// 내 챔피언이 바뀌면 룬 추천을 다시 계산한다.
    fn sync_runes(&mut self, ctx: &egui::Context, data: &SelectData) {
        let champion_id = data
            .session
            .as_ref()
            .and_then(|s| s.my_team.iter().find(|p| p.is_me))
            .map(|s| s.champion_id)
            .filter(|c| *c > 0);
        if champion_id == self.runes_for {
            return;
        }
        self.runes_for = champion_id;
        self.rune_status.set(String::new());
        match champion_id {
            Some(id) => self.runes.reload(ctx, rune_suggestion(id)),
            None => self.runes.reload(ctx, async { Ok(None) }),
        }
    }

    fn ban_tab(&self, ui: &mut egui::Ui, data: &SelectData, session: &ChampSelectFull) {
        let targets: Vec<String> = if data.cache_active {
            data.cached_enemies.clone()
        } else {
            session
                .their_team
                .iter()
                .map(|s| s.riot_id.clone())
                .filter(|id| !id.is_empty())
                .collect()
        };

        if targets.is_empty() {
            ui::empty(ui, "상대팀 소환사 정보를 기다리는 중입니다");
            return;
        }
        for riot_id in &targets {
            let Some(stats) = data.stats.get(riot_id) else {
                ui::table_row(ui, |ui| {
                    ui::cell(ui, 160.0, |ui| ui::person(ui, riot_id, false));
                    ui::muted(ui, "내전 데이터 없음");
                });
                continue;
            };
            // 승률이 높거나 판수가 쌓인 챔피언이 밴 후보다.
            let mut picks: Vec<&ChampionStat> = stats
                .champion_stats
                .iter()
                .filter(|c| c.win_rate >= 50.0 || c.games >= 3)
                .collect();
            if picks.is_empty() {
                picks = stats.champion_stats.iter().take(3).collect();
            }

            ui::table_row(ui, |ui| {
                ui::cell(ui, 160.0, |ui| ui::person(ui, riot_id, false));
                ui::cell(ui, 110.0, |ui| {
                    ui::muted(ui, format!("{}판 {}", stats.games, ui::pct(stats.win_rate)))
                });
                if picks.is_empty() {
                    ui::muted(ui, "내전 데이터 없음");
                }
                for (i, c) in picks.iter().take(3).enumerate() {
                    champion_icon(ui, c.champion_id, 26.0);
                    ui.vertical(|ui| {
                        ui.spacing_mut().item_spacing.y = 0.0;
                        ui.label(ui::txt(
                            &c.champion,
                            13.0,
                            if i == 0 { ui::W::Bold } else { ui::W::Regular },
                            GRAY_900,
                        ));
                        ui.label(ui::txt(
                            format!("{} · {}판", ui::pct(c.win_rate), c.games),
                            11.0,
                            ui::W::Regular,
                            win_rate_color(c.win_rate),
                        ));
                    });
                    ui.add_space(10.0);
                }
            });
        }

        let slot = self.bans.lock();
        if let Some(ba) = slot.value.as_ref().filter(|b| !b.top_banned.is_empty()) {
            ui.add_space(12.0);
            ui.label(ui::txt("내전에서 자주 밴되는 챔피언", 14.0, ui::W::Bold, GRAY_700));
            ui.add_space(6.0);
            ui.horizontal_wrapped(|ui| {
                for ban in ba.top_banned.iter().take(8) {
                    ui.vertical(|ui| {
                        ui.spacing_mut().item_spacing.y = 2.0;
                        champion_icon(ui, ban.champion_id, 32.0);
                        ui.label(ui::txt(
                            format!("{}밴", ui::pct(ban.ban_rate)),
                            11.0,
                            ui::W::Semibold,
                            BLUE_600,
                        ));
                    });
                    ui.add_space(8.0);
                }
            });
        }
    }

    fn pick_tab(&self, ui: &mut egui::Ui, data: &SelectData, session: &ChampSelectFull) {
        let Some(me) = session.my_team.iter().find(|s| s.is_me) else {
            ui::empty(ui, "내 소환사 정보를 기다리는 중입니다");
            return;
        };
        let Some(stats) = data.stats.get(&me.riot_id) else {
            ui::empty(ui, "내전 데이터가 없습니다");
            return;
        };

        let banned: Vec<i32> = session.bans.iter().map(|b| b.champion_id).collect();
        let mut picks: Vec<&ChampionStat> = stats
            .champion_stats
            .iter()
            .filter(|c| c.games >= 2)
            .collect();
        picks.sort_by(|a, b| {
            b.win_rate
                .partial_cmp(&a.win_rate)
                .unwrap_or(std::cmp::Ordering::Equal)
        });
        if picks.is_empty() {
            ui::empty(ui, "2판 이상 플레이한 챔피언이 없습니다");
            return;
        }

        ui.horizontal(|ui| {
            ui::muted(
                ui,
                format!(
                    "{} · 내전 승률순 (2판 이상)",
                    position_label(&me.assigned_position)
                ),
            );
        });
        ui.add_space(6.0);
        ui.horizontal_wrapped(|ui| {
            for c in picks.iter().take(12) {
                let is_banned = banned.contains(&c.champion_id);
                ui.vertical(|ui| {
                    ui.spacing_mut().item_spacing.y = 2.0;
                    champion_icon(ui, c.champion_id, 40.0);
                    if is_banned {
                        ui.label(ui::txt("밴됨", 11.0, ui::W::Semibold, LOSS));
                    } else {
                        ui.label(ui::txt(
                            ui::pct(c.win_rate),
                            12.0,
                            ui::W::Semibold,
                            win_rate_color(c.win_rate),
                        ));
                    }
                    ui::muted(ui, format!("{}판", c.games));
                });
                ui.add_space(10.0);
            }
        });
    }

    fn counter_tab(&self, ui: &mut egui::Ui, session: &ChampSelectFull) {
        let picked: Vec<&ChampSelectSlot> = session
            .their_team
            .iter()
            .filter(|s| s.champion_id > 0)
            .collect();
        if picked.is_empty() {
            ui::empty(ui, "상대팀 챔피언 선택을 기다리는 중입니다");
            return;
        }

        let slot = self.counters.lock();
        if slot.is_first_load() {
            ui::empty(ui, "카운터 분석 중...");
            return;
        }
        let results = slot.value.clone().unwrap_or_default();

        for enemy in picked {
            ui::table_row(ui, |ui| {
                ui::cell(ui, 40.0, |ui| {
                    champion_icon(ui, enemy.champion_id, 32.0);
                });
                ui::cell(ui, 130.0, |ui| {
                    let name = if enemy.riot_id.is_empty() {
                        "상대".to_owned()
                    } else {
                        short_name(&enemy.riot_id).to_owned()
                    };
                    ui::td(ui, name)
                });
                ui::cell(ui, 60.0, |ui| {
                    ui::muted(ui, position_label(&enemy.assigned_position))
                });

                match results.get(&enemy.champion_id) {
                    None => ui::muted(ui, "데이터 없음"),
                    Some(r) if r.matchups.is_empty() => ui::muted(ui, "데이터 없음"),
                    Some(r) => {
                        for m in r.matchups.iter().take(3) {
                            champion_icon(ui, m.opponent_id, 24.0);
                            ui.vertical(|ui| {
                                ui.spacing_mut().item_spacing.y = 0.0;
                                ui.label(ui::txt(&m.opponent, 13.0, ui::W::Regular, GRAY_900));
                                ui.label(ui::txt(
                                    format!("{} · {}판", ui::pct(m.win_rate), m.games),
                                    11.0,
                                    ui::W::Regular,
                                    win_rate_color(m.win_rate),
                                ));
                            });
                            ui.add_space(10.0);
                        }
                    }
                }
            });
        }
    }

    fn tier_tab(&self, ui: &mut egui::Ui) {
        let slot = self.tiers.lock();
        let Some(data) = slot.value.as_ref().filter(|d| !d.tier_list.is_empty()) else {
            ui::empty(ui, "티어 데이터가 없습니다");
            return;
        };
        ui::table_row(ui, |ui| {
            ui::cell(ui, 34.0, |_ui| {});
            ui::cell(ui, 130.0, |ui| ui::th(ui, "챔피언"));
            ui::cell(ui, 40.0, |ui| ui::th(ui, "티어"));
            ui::cell(ui, 50.0, |ui| ui::th(ui, "판수"));
            ui::cell(ui, 60.0, |ui| ui::th(ui, "승률"));
            ui::cell(ui, 50.0, |ui| ui::th(ui, "KDA"));
        });
        for e in data.tier_list.iter().take(20) {
            ui::table_row(ui, |ui| {
                ui::cell(ui, 34.0, |ui| {
                    champion_icon(ui, e.champion_id, 26.0);
                });
                ui::cell(ui, 130.0, |ui| ui::td(ui, e.champion.clone()));
                ui::cell(ui, 40.0, |ui| ui::tier_badge(ui, &e.tier));
                ui::cell(ui, 50.0, |ui| ui::muted(ui, e.games.to_string()));
                ui::cell(ui, 60.0, |ui| {
                    ui::num(ui, ui::pct(e.win_rate), win_rate_color(e.win_rate))
                });
                ui::cell(ui, 50.0, |ui| ui::td(ui, format!("{:.1}", e.kda)));
            });
        }
    }

    fn team_strength(&self, ui: &mut egui::Ui, data: &SelectData, session: &ChampSelectFull) {
        let my_ids: Vec<String> = session
            .my_team
            .iter()
            .map(|s| s.riot_id.clone())
            .filter(|id| !id.is_empty())
            .collect();
        let their_ids: Vec<String> = if data.cache_active {
            data.cached_enemies.clone()
        } else {
            session
                .their_team
                .iter()
                .map(|s| s.riot_id.clone())
                .filter(|id| !id.is_empty())
                .collect()
        };

        let avg = |ids: &[String]| -> (f64, usize) {
            let elos: Vec<f64> = ids
                .iter()
                .filter_map(|id| data.stats.get(id))
                .filter_map(PlayerStats::finite_elo)
                .collect();
            if elos.is_empty() {
                (1000.0, 0)
            } else {
                (elos.iter().sum::<f64>() / elos.len() as f64, elos.len())
            }
        };
        let (my_avg, my_n) = avg(&my_ids);
        let (their_avg, their_n) = avg(&their_ids);
        if my_n == 0 && their_n == 0 {
            return;
        }
        let prob = 1.0 / (1.0 + 10f64.powf(-(my_avg - their_avg) / 400.0));
        let pct = (prob * 100.0).round() as i32;
        let diff = (my_avg - their_avg).round() as i32;

        ui::card_with_head(
            ui,
            "팀 전력 분석",
            |ui| ui::muted(ui, "내전 Elo 기준"),
            |ui| {
                ui.horizontal(|ui| {
                    ui::stat(ui, &format!("우리팀 ({my_n}명)"), &format!("{my_avg:.0}"), TEAM_BLUE);
                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        ui::stat(
                            ui,
                            &format!("상대팀 ({their_n}명)"),
                            &format!("{their_avg:.0}"),
                            TEAM_RED,
                        );
                    });
                });
                ui.add_space(8.0);
                ui.vertical_centered(|ui| {
                    ui.label(ui::txt(format!("{pct}%"), 32.0, ui::W::Bold, GRAY_900));
                    ui::muted(
                        ui,
                        format!("승률 예측 · Elo 차이 {}{diff}", if diff >= 0 { "+" } else { "" }),
                    );
                });
                ui.add_space(8.0);
                ui::split_bar(ui, prob as f32);

                ui.add_space(10.0);
                ui::two_columns(
                    ui,
                    |ui| elo_list(ui, &my_ids, &data.stats, session, TEAM_BLUE),
                    |ui| elo_list(ui, &their_ids, &data.stats, session, TEAM_RED),
                );
            },
        );
    }

    fn rune_card(&self, ui: &mut egui::Ui, session: &ChampSelectFull, data: &SelectData) {
        let Some(me) = session.my_team.iter().find(|s| s.is_me) else {
            return;
        };
        if me.champion_id <= 0 {
            return;
        }

        ui::card_with_head(
            ui,
            "룬 추천",
            |ui| ui::muted(ui, "내 매치 히스토리에서 가장 자주 쓴 조합"),
            |ui| {
                let slot = self.runes.lock();
                if slot.is_first_load() {
                    ui::muted(ui, "룬 데이터 분석 중...");
                    return;
                }
                let Some(Some(rune)) = slot.value.as_ref() else {
                    ui::muted(ui, "이 챔피언의 룬 기록이 없습니다");
                    return;
                };

                ui.horizontal(|ui| {
                    ui.vertical(|ui| {
                        ui.spacing_mut().item_spacing.y = 2.0;
                        ui.horizontal(|ui| {
                            ui.label(ui::txt(
                                rune_style_name(rune.primary_style_id),
                                16.0,
                                ui::W::Bold,
                                BLUE_600,
                            ));
                            ui::muted(ui, "+");
                            ui.label(ui::txt(
                                rune_style_name(rune.sub_style_id),
                                15.0,
                                ui::W::Semibold,
                                GRAY_700,
                            ));
                        });
                        ui.label(ui::txt(
                            format!(
                                "{}판 기반 · 승률 {}",
                                rune.sample_size,
                                ui::pct(rune.win_rate)
                            ),
                            13.0,
                            ui::W::Regular,
                            win_rate_color(rune.win_rate),
                        ));
                    });

                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        let status = self.rune_status.get();
                        let busy = status == "적용 중...";
                        if ui::primary_button(ui, if busy { "적용 중..." } else { "룬 자동 적용" }, !busy)
                            .clicked()
                        {
                            self.apply_runes(ui.ctx(), rune, data, me);
                        }
                    });
                });

                let status = self.rune_status.get();
                if !status.is_empty() {
                    ui.add_space(6.0);
                    let color = if status.contains("완료") { WIN } else { LOSS };
                    ui.label(ui::txt(&status, 13.0, ui::W::Semibold, color));
                }
            },
        );
    }

    fn apply_runes(
        &self,
        ctx: &egui::Context,
        rune: &RuneSuggestion,
        data: &SelectData,
        me: &ChampSelectSlot,
    ) {
        let champion = data
            .stats
            .get(&me.riot_id)
            .and_then(|s| {
                s.champion_stats
                    .iter()
                    .find(|c| c.champion_id == me.champion_id)
            })
            .map(|c| c.champion.clone())
            .unwrap_or_else(|| format!("Champion {}", me.champion_id));

        self.rune_status.set("적용 중...".to_owned());
        let status = self.rune_status.clone();
        let ctx = ctx.clone();
        let (primary, sub, perks) = (
            rune.primary_style_id,
            rune.sub_style_id,
            rune.perk_ids.clone(),
        );
        crate::net::spawn(async move {
            let result =
                crate::lcu::apply_rune_page(format!("Auto: {champion}"), primary, sub, perks).await;
            status.set(match result {
                Ok(()) => "룬 적용 완료".to_owned(),
                Err(e) => format!("룬 적용 실패 — {e}"),
            });
            ctx.request_repaint();
        });
    }

    fn duo_card(&self, ui: &mut egui::Ui, session: &ChampSelectFull) {
        let my_ids: Vec<&String> = session
            .my_team
            .iter()
            .map(|s| &s.riot_id)
            .filter(|id| !id.is_empty())
            .collect();
        let slot = self.duos.lock();
        let Some(all) = slot.value.as_ref() else { return };
        let mut duos: Vec<&DuoSynergy> = all
            .duos
            .iter()
            .filter(|d| my_ids.contains(&&d.player1) && my_ids.contains(&&d.player2))
            .collect();
        if duos.is_empty() {
            return;
        }
        duos.sort_by(|a, b| {
            b.win_rate
                .partial_cmp(&a.win_rate)
                .unwrap_or(std::cmp::Ordering::Equal)
        });

        ui::card_with_head(
            ui,
            "듀오 시너지 (우리팀)",
            |ui| ui::muted(ui, "내전에서 함께 이긴 조합"),
            |ui| {
                for d in duos.iter().take(5) {
                    ui::table_row(ui, |ui| {
                        ui::cell(ui, 200.0, |ui| {
                            ui::td(
                                ui,
                                format!("{} + {}", short_name(&d.player1), short_name(&d.player2)),
                            )
                        });
                        ui::cell(ui, 60.0, |ui| {
                            ui::num(ui, ui::pct(d.win_rate), win_rate_color(d.win_rate))
                        });
                        ui::cell(ui, 50.0, |ui| ui::muted(ui, format!("{}판", d.games)));
                        ui::cell(ui, 80.0, |ui| {
                            ui::muted(ui, format!("KDA {:.1}", d.kda))
                        });
                    });
                }
            },
        );
    }

    fn rival_card(&self, ui: &mut egui::Ui, data: &SelectData, session: &ChampSelectFull) {
        let my_ids: Vec<String> = session
            .my_team
            .iter()
            .map(|s| s.riot_id.clone())
            .filter(|id| !id.is_empty())
            .collect();
        let their_ids: Vec<String> = if data.cache_active {
            data.cached_enemies.clone()
        } else {
            session
                .their_team
                .iter()
                .map(|s| s.riot_id.clone())
                .filter(|id| !id.is_empty())
                .collect()
        };

        let slot = self.rivals.lock();
        let Some(all) = slot.value.as_ref() else { return };
        let mut rivals: Vec<&RivalEntry> = all
            .rivalries
            .iter()
            .filter(|r| {
                (my_ids.contains(&r.player1) && their_ids.contains(&r.player2))
                    || (my_ids.contains(&r.player2) && their_ids.contains(&r.player1))
            })
            .collect();
        if rivals.is_empty() {
            return;
        }
        rivals.sort_by_key(|r| std::cmp::Reverse(r.games));

        ui::card_with_head(
            ui,
            "라이벌 경고",
            |ui| ui::muted(ui, "내전에서 자주 맞붙은 상대"),
            |ui| {
                for r in rivals.iter().take(5) {
                    ui::table_row(ui, |ui| {
                        ui::cell(ui, 130.0, |ui| {
                            ui.label(ui::txt(short_name(&r.player1), 14.0, ui::W::Semibold, TEAM_BLUE))
                        });
                        ui::cell(ui, 26.0, |ui| ui::muted(ui, "vs"));
                        ui::cell(ui, 130.0, |ui| {
                            ui.label(ui::txt(short_name(&r.player2), 14.0, ui::W::Semibold, TEAM_RED))
                        });
                        ui::cell(ui, 90.0, |ui| {
                            ui::num(ui, format!("{} - {}", r.player1_wins, r.player2_wins), GRAY_900)
                        });
                        ui::cell(ui, 56.0, |ui| ui::muted(ui, format!("{}판", r.games)));
                    });
                }
            },
        );
    }
}

fn elo_list(
    ui: &mut egui::Ui,
    ids: &[String],
    stats: &HashMap<String, PlayerStats>,
    session: &ChampSelectFull,
    color: egui::Color32,
) {
    for id in ids {
        let is_me = session
            .my_team
            .iter()
            .any(|s| s.is_me && &s.riot_id == id);
        let elo = stats.get(id).and_then(PlayerStats::finite_elo);
        ui.horizontal(|ui| {
            ui.label(ui::txt(
                short_name(id),
                13.0,
                if is_me { ui::W::Bold } else { ui::W::Regular },
                if is_me { color } else { GRAY_700 },
            ));
            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                ui.label(ui::txt(
                    elo.map(|e| format!("{e:.0}")).unwrap_or_else(|| "—".into()),
                    13.0,
                    ui::W::Semibold,
                    elo_color(elo),
                ));
            });
        });
    }
}

fn team_card(
    ui: &mut egui::Ui,
    title: &str,
    color: egui::Color32,
    riot_ids: &[String],
    stats: &HashMap<String, PlayerStats>,
    slots: &[ChampSelectSlot],
) {
    ui::card_with_head(
        ui,
        title,
        |ui| ui::muted(ui, format!("{}명", riot_ids.len())),
        |ui| {
            if riot_ids.iter().all(String::is_empty) {
                ui::empty(ui, "소환사 정보를 기다리는 중입니다");
                return;
            }
            for (i, riot_id) in riot_ids.iter().enumerate() {
                if riot_id.is_empty() {
                    continue;
                }
                let slot = slots.get(i);
                let s = stats.get(riot_id);
                ui::card_sunken(ui, |ui| {
                    ui.horizontal(|ui| {
                        if let Some(sl) = slot.filter(|s| s.champion_id > 0) {
                            champion_icon(ui, sl.champion_id, 28.0);
                        }
                        ui::person(ui, riot_id, slot.is_some_and(|s| s.is_me));
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            let elo = s.and_then(PlayerStats::finite_elo);
                            ui.label(ui::txt(
                                elo.map(|e| format!("Elo {e:.0}")).unwrap_or_else(|| "Elo —".into()),
                                13.0,
                                ui::W::Semibold,
                                elo_color(elo),
                            ));
                        });
                    });
                    match s.filter(|s| !s.champion_stats.is_empty()) {
                        None => {
                            ui.add_space(4.0);
                            ui::muted(ui, "내전 기록 없음");
                        }
                        Some(s) => {
                            ui.add_space(6.0);
                            ui.horizontal(|ui| {
                                for c in s.champion_stats.iter().take(3) {
                                    ui.vertical(|ui| {
                                        ui.spacing_mut().item_spacing.y = 2.0;
                                        champion_icon(ui, c.champion_id, 26.0);
                                        ui.label(ui::txt(
                                            ui::pct(c.win_rate),
                                            11.0,
                                            ui::W::Semibold,
                                            win_rate_color(c.win_rate),
                                        ));
                                    });
                                }
                                // 상대가 특정 챔피언을 많이 했으면 그게 밴 근거가 된다.
                                if let Some(top) = s.champion_stats.first().filter(|c| c.games >= 3)
                                {
                                    ui.add_space(6.0);
                                    ui::muted(
                                        ui,
                                        format!(
                                            "{} {}판 {}",
                                            top.champion,
                                            top.games,
                                            ui::pct(top.win_rate)
                                        ),
                                    );
                                }
                            });
                        }
                    }
                });
            }
            let _ = color;
        },
    );
}

async fn load(signals: Signals) -> Result<SelectData, String> {
    let Some(session) = crate::lcu::champ_select_full().await else {
        return Ok(SelectData::default());
    };

    let mut riot_ids: Vec<String> = session
        .my_team
        .iter()
        .chain(session.their_team.iter())
        .map(|s| s.riot_id.clone())
        .filter(|id| !id.is_empty())
        .collect();

    // 상대팀 riotId 가 통째로 비었으면 LCU 가 가린 것이다. 로비 캐시로 메운다.
    let enemies_hidden = session.their_team.iter().all(|s| s.riot_id.is_empty());
    let (cache_active, cached_enemies) = {
        let cache = signals.lobby.lock();
        if enemies_hidden && cache.is_valid() {
            let ids: Vec<String> = cache
                .enemy_team()
                .iter()
                .map(|p| p.riot_id.clone())
                .filter(|id| !id.is_empty())
                .collect();
            (true, ids)
        } else {
            (false, Vec::new())
        }
    };
    riot_ids.extend(cached_enemies.iter().cloned());
    riot_ids.sort();
    riot_ids.dedup();

    // 캐시에 이미 있는 통계는 다시 받지 않는다. 2초마다 도는 화면이라 중요하다.
    let mut stats: HashMap<String, PlayerStats> = {
        let cache = signals.lobby.lock();
        cache.player_stats.clone()
    };
    for id in &riot_ids {
        if stats.contains_key(id) {
            continue;
        }
        if let Ok(s) = crate::api::player_stats(id.clone(), MODE).await {
            stats.insert(id.clone(), s);
        }
    }
    {
        let mut cache = signals.lobby.lock();
        for (id, s) in &stats {
            cache.player_stats.entry(id.clone()).or_insert(s.clone());
        }
    }

    Ok(SelectData {
        session: Some(session),
        stats,
        cache_active,
        cached_enemies,
    })
}

async fn load_counters(champion_ids: Vec<i32>) -> Result<HashMap<i32, MatchupResult>, String> {
    let names = champion_names().await?;
    let mut out = HashMap::new();
    for id in champion_ids {
        let Some(name) = names.get(&id) else { continue };
        // 같은 라인 기준을 먼저 보고, 표본이 없으면 라인 무관으로 넓힌다.
        let mut result = crate::api::matchup(name.clone(), MODE).await.ok();
        if result.as_ref().is_none_or(|r| r.matchups.is_empty()) {
            result = crate::api::matchup(name.clone(), MODE).await.ok();
        }
        if let Some(r) = result {
            out.insert(id, r);
        }
    }
    Ok(out)
}

/// championId → 영문 키. Data Dragon 최신 버전 기준.
async fn champion_names() -> Result<HashMap<i32, String>, String> {
    let versions: Vec<String> = crate::net::http()
        .get("https://ddragon.leagueoflegends.com/api/versions.json")
        .send()
        .await
        .map_err(|e| e.to_string())?
        .json()
        .await
        .map_err(|e| e.to_string())?;
    let latest = versions.first().ok_or("버전 목록이 비어 있습니다")?;
    let data: serde_json::Value = crate::net::http()
        .get(format!(
            "https://ddragon.leagueoflegends.com/cdn/{latest}/data/ko_KR/champion.json"
        ))
        .send()
        .await
        .map_err(|e| e.to_string())?
        .json()
        .await
        .map_err(|e| e.to_string())?;

    let mut map = HashMap::new();
    if let Some(obj) = data["data"].as_object() {
        for champ in obj.values() {
            if let (Some(key), Some(id)) = (champ["key"].as_str(), champ["id"].as_str()) {
                if let Ok(n) = key.parse::<i32>() {
                    map.insert(n, id.to_owned());
                }
            }
        }
    }
    Ok(map)
}

// ── 룬 추천 ──────────────────────────────────────

#[derive(Clone, Debug, PartialEq)]
pub struct RuneSuggestion {
    pub primary_style_id: i32,
    pub sub_style_id: i32,
    pub perk_ids: Vec<i32>,
    pub sample_size: usize,
    pub win_rate: f64,
}

fn rune_style_name(style_id: i32) -> String {
    match style_id {
        8000 => "정밀".to_owned(),
        8100 => "지배".to_owned(),
        8200 => "마법".to_owned(),
        8300 => "영감".to_owned(),
        8400 => "결의".to_owned(),
        other => format!("스타일 {other}"),
    }
}

#[derive(Clone)]
struct RuneSetup {
    primary: i32,
    sub: i32,
    perks: Vec<i32>,
    win: bool,
}

/// 내 매치 히스토리 100판에서 이 챔피언으로 가장 자주 쓴 룬 조합을 고른다.
async fn rune_suggestion(champion_id: i32) -> Result<Option<RuneSuggestion>, String> {
    let Some(creds) = crate::lcu::credentials() else {
        return Ok(None);
    };

    let mut setups: Vec<RuneSetup> = Vec::new();
    for beg in (0..100).step_by(20) {
        let endpoint = format!(
            "/lol-match-history/v1/products/lol/current-summoner/matches?begIndex={beg}&endIndex={}",
            beg + 19
        );
        let Ok(data) = crate::lcu::get(&creds, &endpoint).await else {
            break;
        };
        let Some(games) = data["games"]["games"].as_array() else {
            break;
        };
        let count = games.len();
        for game in games {
            let Some(participants) = game["participants"].as_array() else {
                continue;
            };
            for p in participants {
                if p["championId"].as_i64() != Some(champion_id as i64) {
                    continue;
                }
                let stats = &p["stats"];
                let (Some(primary), Some(sub)) = (
                    stats["perkPrimaryStyle"].as_i64(),
                    stats["perkSubStyle"].as_i64(),
                ) else {
                    continue;
                };
                let perks: Vec<i32> = (0..6)
                    .filter_map(|i| stats[format!("perk{i}")].as_i64())
                    .map(|v| v as i32)
                    .collect();
                if perks.len() < 6 || primary <= 0 {
                    continue;
                }
                setups.push(RuneSetup {
                    primary: primary as i32,
                    sub: sub as i32,
                    perks,
                    win: stats["win"].as_bool().unwrap_or(false),
                });
            }
        }
        if count < 20 {
            break;
        }
    }

    Ok(most_common_setup(&setups))
}

/// (주 스타일, 부 스타일, 핵심 룬) 이 같은 것끼리 묶어 가장 많은 조합을 고른다.
fn most_common_setup(setups: &[RuneSetup]) -> Option<RuneSuggestion> {
    if setups.is_empty() {
        return None;
    }
    let mut groups: HashMap<(i32, i32, i32), Vec<&RuneSetup>> = HashMap::new();
    for s in setups {
        let keystone = s.perks.first().copied().unwrap_or(0);
        groups
            .entry((s.primary, s.sub, keystone))
            .or_default()
            .push(s);
    }
    let best = groups.values().max_by_key(|v| v.len())?;
    let representative = best.first()?;
    let wins = best.iter().filter(|s| s.win).count();
    Some(RuneSuggestion {
        primary_style_id: representative.primary,
        sub_style_id: representative.sub,
        perk_ids: representative.perks.clone(),
        sample_size: best.len(),
        win_rate: wins as f64 / best.len() as f64 * 100.0,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    fn setup(primary: i32, sub: i32, keystone: i32, win: bool) -> RuneSetup {
        RuneSetup {
            primary,
            sub,
            perks: vec![keystone, 1, 2, 3, 4, 5],
            win,
        }
    }

    #[test]
    fn picks_the_most_used_rune_setup() {
        let setups = vec![
            setup(8000, 8100, 8005, true),
            setup(8000, 8100, 8005, false),
            setup(8000, 8100, 8005, true),
            setup(8200, 8300, 8214, true),
        ];
        let best = most_common_setup(&setups).expect("조합이 있어야 한다");
        assert_eq!(best.primary_style_id, 8000);
        assert_eq!(best.sub_style_id, 8100);
        assert_eq!(best.sample_size, 3);
        // 3판 중 2승.
        assert!((best.win_rate - 66.666).abs() < 0.01);
    }

    #[test]
    fn no_history_gives_no_suggestion() {
        assert!(most_common_setup(&[]).is_none());
    }

    #[test]
    fn different_keystones_are_different_setups() {
        let setups = vec![
            setup(8000, 8100, 8005, true),
            setup(8000, 8100, 8008, true),
            setup(8000, 8100, 8008, true),
        ];
        let best = most_common_setup(&setups).expect("조합이 있어야 한다");
        assert_eq!(best.perk_ids[0], 8008);
        assert_eq!(best.sample_size, 2);
    }

    #[test]
    fn rune_style_names_cover_all_five_trees() {
        for id in [8000, 8100, 8200, 8300, 8400] {
            assert!(!rune_style_name(id).starts_with("스타일"));
        }
        assert_eq!(rune_style_name(9999), "스타일 9999");
    }
}
