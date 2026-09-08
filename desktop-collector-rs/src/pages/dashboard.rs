//! 내전 대시보드 — 통계 한눈에 보기.

use crate::icons::champion_icon;
use crate::models::*;
use crate::net::Remote;
use crate::theme::*;
use crate::ui;

use super::{slot_body, MODE};

#[derive(Default)]
pub struct DashboardPage {
    overview: Remote<OverviewResult>,
    elo: Remote<EloLeaderboardResult>,
    awards: Remote<AwardsResult>,
    multikill: Remote<MultikillResult>,
    mvp: Remote<MvpRankingResult>,
    bans: Remote<BanAnalysisResult>,
    tiers: Remote<ChampionTierResult>,
    duos: Remote<DuoSynergyResult>,
}

impl DashboardPage {
    fn ensure_all(&self, ctx: &egui::Context) {
        self.overview.ensure(ctx, crate::api::overview(MODE));
        self.elo.ensure(ctx, crate::api::elo_leaderboard());
        self.awards.ensure(ctx, crate::api::awards(MODE));
        self.multikill.ensure(ctx, crate::api::multikill(MODE));
        self.mvp.ensure(ctx, crate::api::mvp_ranking(MODE));
        self.bans.ensure(ctx, crate::api::ban_analysis(MODE));
        self.tiers.ensure(ctx, crate::api::champion_tier(MODE, 3));
        self.duos.ensure(ctx, crate::api::duo_synergy(MODE, 2));
    }

    fn reload_all(&self, ctx: &egui::Context) {
        self.overview.reload(ctx, crate::api::overview(MODE));
        self.elo.reload(ctx, crate::api::elo_leaderboard());
        self.awards.reload(ctx, crate::api::awards(MODE));
        self.multikill.reload(ctx, crate::api::multikill(MODE));
        self.mvp.reload(ctx, crate::api::mvp_ranking(MODE));
        self.bans.reload(ctx, crate::api::ban_analysis(MODE));
        self.tiers.reload(ctx, crate::api::champion_tier(MODE, 3));
        self.duos.reload(ctx, crate::api::duo_synergy(MODE, 2));
    }

    pub fn ui(&mut self, ui: &mut egui::Ui) {
        let ctx = ui.ctx().clone();
        self.ensure_all(&ctx);

        ui::page(ui, "내전 대시보드", "내전 통계 한눈에 보기", |ui| {
            if super::refresh_button(ui, false) {
                self.reload_all(&ctx);
            }

            self.overview_card(ui);
            ui::two_columns(ui, |ui| self.elo_card(ui), |ui| self.awards_card(ui));
            ui::two_columns(ui, |ui| self.streak_card(ui), |ui| self.mvp_card(ui));
            ui::two_columns(ui, |ui| self.ban_card(ui), |ui| self.tier_card(ui));
            ui::two_columns(ui, |ui| self.duo_card(ui), |ui| self.multikill_card(ui));
            self.top_picks_card(ui);
        });
    }

    fn overview_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "전체 개요",
            |_ui| {},
            |ui| {
                let slot = self.overview.lock();
                slot_body(
                    ui,
                    &slot,
                    "집계된 경기가 없습니다",
                    |o| o.match_count == 0,
                    |ui, o| {
                        // 한 줄에 넷씩. 좁아지면 Grid 가 알아서 접는다.
                        egui::Grid::new("overview-grid")
                            .num_columns(4)
                            .spacing([32.0, 14.0])
                            .show(ui, |ui| {
                                ui::stat(ui, "총 경기", &o.match_count.to_string(), GRAY_900);
                                ui::stat(
                                    ui,
                                    "평균 시간",
                                    &format!("{:.1}분", o.avg_game_minutes),
                                    GRAY_900,
                                );
                                ui::stat(ui, "드래곤", &o.total_dragon_kills.to_string(), GRAY_900);
                                ui::stat(ui, "바론", &o.total_baron_kills.to_string(), GRAY_900);
                                ui.end_row();
                                ui::stat(ui, "포탑", &o.total_tower_kills.to_string(), GRAY_900);
                                ui::stat(
                                    ui,
                                    "억제기",
                                    &o.total_inhibitor_kills.to_string(),
                                    GRAY_900,
                                );
                                ui::stat(ui, "퍼블", &o.total_first_bloods.to_string(), GRAY_900);
                                ui::stat(ui, "총 CS", &ui::short_num(o.total_cs as f64), GRAY_900);
                                ui.end_row();
                            });
                    },
                );
            },
        );
    }

    fn elo_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "Elo 리더보드",
            |ui| {
                let slot = self.elo.lock();
                if let Some(d) = slot.value.as_ref() {
                    ui::muted(ui, format!("{}판 이상", d.min_games));
                }
            },
            |ui| {
                let slot = self.elo.lock();
                slot_body(
                    ui,
                    &slot,
                    "Elo 데이터가 없습니다",
                    |d| d.players.is_empty(),
                    |ui, data| {
                        // 배치 중인 사람은 순위를 매기지 않는다. 상위 10명만 본다.
                        let ranked: Vec<&EloEntry> = data
                            .players
                            .iter()
                            .filter(|p| !p.placement)
                            .take(10)
                            .collect();
                        for e in ranked {
                            ui::table_row(ui, |ui| {
                                ui::rank_cell(ui, e.rank.max(1) as usize);
                                ui::flex_cell(ui, 176.0, |ui| {
                                    ui::person(ui, &e.riot_id, e.rank <= 3)
                                });
                                ui::cell(ui, 50.0, |ui| {
                                    ui::num(ui, format!("{:.0}", e.elo), elo_color(Some(e.elo)))
                                });
                                ui::cell(ui, 50.0, |ui| {
                                    ui::num(ui, ui::pct(e.win_rate), win_rate_color(e.win_rate))
                                });
                                ui::games_cell(ui, 52.0, e.games, e.sample_grade);
                            });
                        }
                    },
                );
            },
        );
    }

    fn awards_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "어워즈",
            |_ui| {},
            |ui| {
                let slot = self.awards.lock();
                slot_body(
                    ui,
                    &slot,
                    "어워즈 데이터가 없습니다",
                    |a| a.entries().is_empty(),
                    |ui, a| {
                        for (title, e) in a.entries() {
                            ui::table_row(ui, |ui| {
                                ui::cell(ui, 104.0, |ui| {
                                    ui.label(ui::txt(title, 12.0, ui::W::Semibold, GRAY_500))
                                });
                                ui::flex_cell(ui, 108.0, |ui| ui::person(ui, &e.riot_id, false));
                                ui::cell(ui, 100.0, |ui| {
                                    ui::num(ui, e.display_value.clone(), BLUE_600)
                                });
                            });
                        }
                    },
                );
            },
        );
    }

    /// 연승/연패는 Elo 리더보드가 이미 들고 있다. 사람마다 따로 부르지 않는다.
    fn streak_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "연승 · 연패",
            |ui| ui::muted(ui, "2연속 이상"),
            |ui| {
                let slot = self.elo.lock();
                slot_body(
                    ui,
                    &slot,
                    "스트릭 데이터가 없습니다",
                    |d| d.players.is_empty(),
                    |ui, data| {
                        let mut hot: Vec<&EloEntry> =
                            data.players.iter().filter(|p| p.win_streak >= 2).collect();
                        let mut cold: Vec<&EloEntry> =
                            data.players.iter().filter(|p| p.loss_streak >= 2).collect();
                        hot.sort_by_key(|p| std::cmp::Reverse(p.win_streak));
                        cold.sort_by_key(|p| std::cmp::Reverse(p.loss_streak));

                        if hot.is_empty() && cold.is_empty() {
                            ui::empty(ui, "현재 2연승/2연패 이상인 플레이어가 없습니다");
                            return;
                        }

                        for (label, list, color, suffix, winning) in [
                            ("연승 중", &hot, WIN, "연승", true),
                            ("연패 중", &cold, LOSS, "연패", false),
                        ] {
                            if list.is_empty() {
                                continue;
                            }
                            ui.label(ui::txt(label, 13.0, ui::W::Bold, color));
                            ui.add_space(2.0);
                            for p in list.iter().take(5) {
                                let n = if winning { p.win_streak } else { p.loss_streak };
                                ui::table_row(ui, |ui| {
                                    ui::flex_cell(ui, 168.0, |ui| {
                                        ui::person(ui, &p.riot_id, false)
                                    });
                                    ui::cell(ui, 62.0, |ui| {
                                        ui::num(ui, format!("{n}{suffix}"), color)
                                    });
                                    ui::cell(ui, 48.0, |ui| {
                                        ui::muted(ui, format!("{}판", p.games))
                                    });
                                    ui::cell(ui, 50.0, |ui| {
                                        ui::num(ui, format!("{:.0}", p.elo), elo_color(Some(p.elo)))
                                    });
                                });
                            }
                            ui.add_space(6.0);
                        }
                    },
                );
            },
        );
    }

    fn mvp_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "MVP 랭킹",
            |ui| ui::muted(ui, "MVP 비율 순"),
            |ui| {
                let slot = self.mvp.lock();
                slot_body(
                    ui,
                    &slot,
                    "MVP 데이터가 없습니다",
                    |d| d.rankings.is_empty(),
                    |ui, data| {
                        for (i, e) in data.rankings.iter().take(6).enumerate() {
                            ui::table_row(ui, |ui| {
                                ui::rank_cell(ui, i + 1);
                                ui::flex_cell(ui, 190.0, |ui| ui::person(ui, &e.riot_id, i < 3));
                                ui::cell(ui, 28.0, |ui| {
                                    champion_icon(ui, e.top_champion_id.unwrap_or(0), 24.0);
                                });
                                ui::cell(ui, 50.0, |ui| ui::num(ui, ui::pct(e.mvp_rate), BLUE_600));
                                ui::cell(ui, 96.0, |ui| {
                                    ui::muted(
                                        ui,
                                        format!("MVP {} · ACE {}", e.mvp_count, e.ace_count),
                                    )
                                });
                            });
                        }
                    },
                );
            },
        );
    }

    fn ban_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "밴 트렌드",
            |ui| ui::muted(ui, "자주 밴되는 챔피언"),
            |ui| {
                let slot = self.bans.lock();
                slot_body(
                    ui,
                    &slot,
                    "밴 데이터가 없습니다",
                    |d| d.top_banned.is_empty(),
                    |ui, data| {
                        for ban in data.top_banned.iter().take(8) {
                            ui::table_row(ui, |ui| {
                                ui::cell(ui, 30.0, |ui| {
                                    champion_icon(ui, ban.champion_id, 24.0);
                                });
                                ui::flex_cell(ui, 176.0, |ui| ui::td(ui, ban.champion.clone()));
                                ui::bar(ui, (ban.ban_rate / 100.0) as f32, 62.0, BLUE_500);
                                ui::cell(ui, 52.0, |ui| {
                                    ui::num(ui, ui::pct(ban.ban_rate), BLUE_600)
                                });
                                ui::cell(ui, 46.0, |ui| {
                                    ui::muted(ui, format!("{}회", ban.ban_count))
                                });
                            });
                        }
                    },
                );
            },
        );
    }

    fn tier_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "챔피언 티어",
            |ui| ui::muted(ui, "3판 이상"),
            |ui| {
                let slot = self.tiers.lock();
                slot_body(
                    ui,
                    &slot,
                    "챔피언 티어 데이터가 없습니다",
                    |d| d.tier_list.is_empty(),
                    |ui, data| {
                        for e in data.tier_list.iter().take(10) {
                            ui::table_row(ui, |ui| {
                                ui::cell(ui, 30.0, |ui| {
                                    champion_icon(ui, e.champion_id, 24.0);
                                });
                                ui::flex_cell(ui, 174.0, |ui| ui::td(ui, e.champion.clone()));
                                ui::cell(ui, 28.0, |ui| ui::tier_badge(ui, &e.tier));
                                ui::cell(ui, 50.0, |ui| {
                                    ui::num(ui, ui::pct(e.win_rate), win_rate_color(e.win_rate))
                                });
                                ui::games_cell(ui, 60.0, e.games, e.sample_grade);
                            });
                        }
                    },
                );
            },
        );
    }

    fn duo_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "듀오 시너지 Top 5",
            |ui| ui::muted(ui, "보정 승률 순"),
            |ui| {
                let slot = self.duos.lock();
                slot_body(
                    ui,
                    &slot,
                    "듀오 데이터가 없습니다",
                    |d| d.duos.is_empty(),
                    |ui, data| {
                        // 관측 승률로 줄 세우면 2판 100% 가 맨 위로 온다. 보정 승률로 정렬한다.
                        let mut duos: Vec<&DuoSynergy> = data.duos.iter().collect();
                        duos.sort_by(|a, b| {
                            b.adjusted_win_rate
                                .partial_cmp(&a.adjusted_win_rate)
                                .unwrap_or(std::cmp::Ordering::Equal)
                        });
                        for (i, duo) in duos.iter().take(5).enumerate() {
                            ui::table_row(ui, |ui| {
                                ui::rank_cell(ui, i + 1);
                                ui::flex_cell(ui, 130.0, |ui| {
                                    ui::td(
                                        ui,
                                        format!(
                                            "{} + {}",
                                            short_name(&duo.player1),
                                            short_name(&duo.player2)
                                        ),
                                    )
                                });
                                ui::cell(ui, 52.0, |ui| {
                                    ui::num(ui, ui::pct(duo.win_rate), win_rate_color(duo.win_rate))
                                });
                                ui::games_cell(ui, 60.0, duo.games, duo.sample_grade);
                            });
                        }
                    },
                );
            },
        );
    }

    fn multikill_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "멀티킬 하이라이트",
            |ui| ui::muted(ui, "펜타킬 · 최근 기록"),
            |ui| {
                let slot = self.multikill.lock();
                slot_body(
                    ui,
                    &slot,
                    "멀티킬 기록이 없습니다",
                    |d| d.penta_kill_events.is_empty() && d.recent_highlights.is_empty(),
                    |ui, data| {
                        for (label, events) in [
                            ("펜타킬", &data.penta_kill_events),
                            ("최근 기록", &data.recent_highlights),
                        ] {
                            if events.is_empty() {
                                continue;
                            }
                            ui.label(ui::txt(label, 13.0, ui::W::Bold, GRAY_700));
                            ui.add_space(2.0);
                            for e in events.iter().take(5) {
                                ui::table_row(ui, |ui| {
                                    ui::cell(ui, 30.0, |ui| {
                                        champion_icon(ui, e.champion_id, 24.0);
                                    });
                                    ui::flex_cell(ui, 178.0, |ui| {
                                        ui::person(ui, &e.riot_id, false)
                                    });
                                    ui::cell(ui, 80.0, |ui| ui::td(ui, e.champion.clone()));
                                    ui::cell(ui, 58.0, |ui| {
                                        ui::chip_blue(ui, &multikill_label(&e.multi_kill_type))
                                    });
                                    ui::cell(ui, 40.0, |ui| {
                                        ui::muted(ui, ui::short_date_kst(e.game_creation))
                                    });
                                });
                            }
                            ui.add_space(6.0);
                        }
                    },
                );
            },
        );
    }

    fn top_picks_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "많이 고른 챔피언",
            |ui| ui::muted(ui, "픽 수 기준"),
            |ui| {
                let slot = self.overview.lock();
                slot_body(
                    ui,
                    &slot,
                    "픽 데이터가 없습니다",
                    |o| o.top_picked_champions.is_empty(),
                    |ui, o| {
                        ui::table_row(ui, |ui| {
                            ui::cell(ui, 32.0, |_ui| {});
                            ui::cell(ui, 140.0, |ui| ui::th(ui, "챔피언"));
                            ui::cell(ui, 60.0, |ui| ui::th(ui, "픽"));
                            ui::cell(ui, 60.0, |ui| ui::th(ui, "승"));
                            ui::cell(ui, 108.0, |ui| ui::th(ui, "승률"));
                            ui::cell(ui, 60.0, |ui| ui::th(ui, "KDA"));
                            ui::cell(ui, 80.0, |ui| ui::th(ui, "평균 딜"));
                        });
                        for c in o.top_picked_champions.iter().take(10) {
                            ui::table_row(ui, |ui| {
                                ui::cell(ui, 32.0, |ui| {
                                    champion_icon(ui, c.champion_id, 24.0);
                                });
                                ui::cell(ui, 140.0, |ui| ui::td(ui, c.champion.clone()));
                                ui::cell(ui, 60.0, |ui| ui::td(ui, c.picks.to_string()));
                                ui::cell(ui, 60.0, |ui| ui::muted(ui, c.wins.to_string()));
                                ui::cell(ui, 108.0, |ui| {
                                    ui::bar(
                                        ui,
                                        (c.win_rate / 100.0) as f32,
                                        44.0,
                                        win_rate_color(c.win_rate),
                                    );
                                    ui::num(ui, ui::pct(c.win_rate), win_rate_color(c.win_rate));
                                });
                                ui::cell(ui, 60.0, |ui| ui::td(ui, format!("{:.2}", c.kda)));
                                ui::cell(ui, 80.0, |ui| {
                                    ui::muted(ui, ui::short_num(c.avg_damage))
                                });
                            });
                        }
                    },
                );
            },
        );
    }
}

fn multikill_label(kind: &str) -> String {
    match kind.to_uppercase().as_str() {
        "PENTA" => "펜타킬".to_owned(),
        "QUADRA" => "쿼드라킬".to_owned(),
        "TRIPLE" => "트리플킬".to_owned(),
        "DOUBLE" => "더블킬".to_owned(),
        other => other.to_owned(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn multikill_labels_are_korean() {
        assert_eq!(multikill_label("PENTA"), "펜타킬");
        assert_eq!(multikill_label("quadra"), "쿼드라킬");
        // 모르는 종류는 그대로 보여 준다. 빈칸보다는 낫다.
        assert_eq!(multikill_label("HEXA"), "HEXA");
    }
}
