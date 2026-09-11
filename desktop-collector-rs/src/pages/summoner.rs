//! 소환사 검색 — 닉네임으로 내전 전적 조회.
//!
//! 프로필 · 연승 · 챔피언 · 최근 경기는 `/summoner/{riotId}` 한 번에 다 온다.
//! Elo 히스토리 · 플레이스타일 · 포지션 · 라이엇 랭크만 따로 받는다.

use std::collections::HashMap;

use crate::icons::champion_icon;
use crate::models::*;
use crate::net::Remote;
use crate::theme::*;
use crate::ui;

use super::{position_label, queue_label, MODE};

#[derive(Default)]
pub struct SummonerPage {
    query: String,
    /// 검색어와 이름이 겹치는 후보들. 하나면 바로 연다.
    candidates: Vec<String>,
    search: Remote<Vec<String>>,
    searched_once: bool,

    selected: Option<String>,
    detail: Remote<SummonerResult>,
    profile: Remote<RiotProfile>,
    elo_history: Remote<EloHistoryResult>,
    dna: Remote<Option<DnaEntry>>,
    positions: Remote<Vec<PositionPoolEntry>>,

    expanded: Option<String>,
    match_details: HashMap<String, Remote<MatchDetail>>,
}

impl SummonerPage {
    fn select(&mut self, ctx: &egui::Context, riot_id: String) {
        self.selected = Some(riot_id.clone());
        self.expanded = None;
        self.match_details.clear();

        self.detail
            .reload(ctx, crate::api::summoner(riot_id.clone(), MODE));
        self.profile
            .reload(ctx, crate::api::riot_profile(riot_id.clone()));
        self.elo_history
            .reload(ctx, crate::api::elo_history(riot_id.clone(), 20));

        let id = riot_id.clone();
        self.dna.reload(ctx, async move {
            let r = crate::api::playstyle_dna(MODE).await?;
            Ok(r.players.into_iter().find(|p| p.riot_id == id))
        });
        let id = riot_id;
        self.positions.reload(ctx, async move {
            let r = crate::api::position_pool(MODE).await?;
            Ok(r.all_players
                .into_iter()
                .filter(|p| p.riot_id == id)
                .collect())
        });
    }

    fn run_search(&mut self, ctx: &egui::Context) {
        let q = self.query.trim().to_lowercase();
        if q.is_empty() {
            return;
        }
        self.searched_once = true;
        self.candidates.clear();
        self.selected = None;
        self.search.reload(ctx, async move {
            let list = crate::api::stats_list(MODE).await?;
            Ok(list
                .stats
                .into_iter()
                .map(|s| s.riot_id)
                .filter(|id| short_name(id).to_lowercase().contains(&q))
                .collect())
        });
    }

    pub fn ui(&mut self, ui: &mut egui::Ui) {
        let ctx = ui.ctx().clone();

        // 검색 결과가 도착했으면 후보 목록을 채운다. 한 명이면 바로 연다.
        let arrived = self.search.lock().value.take();
        if let Some(found) = arrived {
            self.candidates = found;
            if self.candidates.len() == 1 {
                let only = self.candidates[0].clone();
                self.select(&ctx, only);
            }
        }

        let mut pick: Option<String> = None;
        let mut do_search = false;

        ui::page(ui, "소환사 검색", "닉네임으로 내전 전적 조회", |ui| {
            ui::card(ui, |ui| {
                ui.horizontal(|ui| {
                    let button_w = 92.0;
                    ui.allocate_ui_with_layout(
                        egui::vec2(ui.available_width() - button_w - 8.0, 40.0),
                        egui::Layout::top_down(egui::Align::Min),
                        |ui| {
                            let r = ui::search_input(ui, &mut self.query, "닉네임 입력 (태그 제외)");
                            if r.lost_focus() && ui.input(|i| i.key_pressed(egui::Key::Enter)) {
                                do_search = true;
                            }
                        },
                    );
                    let searching = self.search.lock().loading;
                    if ui::primary_button(
                        ui,
                        if searching { "검색 중" } else { "검색" },
                        !searching && !self.query.trim().is_empty(),
                    )
                    .clicked()
                    {
                        do_search = true;
                    }
                });

                let (loading, err) = {
                    let slot = self.search.lock();
                    (slot.loading, slot.error.clone())
                };
                if let Some(e) = err {
                    ui.add_space(6.0);
                    ui::error(ui, &e);
                } else if self.searched_once
                    && self.candidates.is_empty()
                    && !loading
                    && self.selected.is_none()
                {
                    ui.add_space(6.0);
                    ui::muted(ui, "검색 결과가 없습니다");
                }

                if self.candidates.len() > 1 {
                    ui.add_space(10.0);
                    ui::muted(ui, format!("{}명 검색됨 — 선택하세요", self.candidates.len()));
                    ui.add_space(4.0);
                    ui.horizontal_wrapped(|ui| {
                        for id in &self.candidates {
                            let selected = self.selected.as_deref() == Some(id.as_str());
                            let (bg, fg) = if selected {
                                (BLUE_50, BLUE_600)
                            } else {
                                (GRAY_100, GRAY_700)
                            };
                            let button = egui::Button::new(ui::txt(id, 13.0, ui::W::Semibold, fg))
                                .fill(bg)
                                .stroke(egui::Stroke::NONE)
                                .corner_radius(cr(R_MD));
                            if ui.add(button).clicked() {
                                pick = Some(id.clone());
                            }
                        }
                    });
                }
            });

            if self.selected.is_none() {
                return;
            }

            let (loading, error, detail) = {
                let slot = self.detail.lock();
                (slot.is_first_load(), slot.error.clone(), slot.value.clone())
            };
            if loading {
                ui::card(ui, |ui| ui::empty(ui, "전적 조회 중..."));
                return;
            }
            if let Some(e) = error {
                ui::card(ui, |ui| ui::error(ui, &e));
                return;
            }
            let Some(d) = detail else { return };

            summary_card(ui, &d);
            self.profile_card(ui);
            ui::two_columns(
                ui,
                |ui| streak_card(ui, d.streak.as_ref()),
                |ui| self.position_card(ui),
            );
            ui::two_columns(ui, |ui| self.elo_card(ui), |ui| self.dna_card(ui));
            champion_card(ui, &d);
            self.recent_card(ui, &ctx, &d);
        });

        if do_search {
            self.run_search(&ctx);
        }
        if let Some(id) = pick {
            self.select(&ctx, id);
        }
    }

    fn profile_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "Riot 프로필",
            |ui| ui::muted(ui, "솔로 · 자유 랭크"),
            |ui| {
                let slot = self.profile.lock();
                let Some(p) = slot.value.as_ref() else {
                    ui::muted(ui, "프로필 불러오는 중...");
                    return;
                };
                if p.solo_rank.is_none() && p.flex_rank.is_none() && p.top_mastery.is_empty() {
                    ui::muted(ui, "라이엇 프로필 정보가 없습니다");
                    return;
                }
                ui.horizontal(|ui| {
                    ui.spacing_mut().item_spacing.x = 36.0;
                    rank_block(ui, "솔로 랭크", p.solo_rank.as_ref());
                    rank_block(ui, "자유 랭크", p.flex_rank.as_ref());
                    if let Some(level) = p.summoner_level {
                        ui::stat(ui, "레벨", &level.to_string(), GRAY_900);
                    }
                });
                if !p.top_mastery.is_empty() {
                    ui.add_space(12.0);
                    ui.label(ui::txt("숙련도 Top 챔피언", 13.0, ui::W::Semibold, GRAY_600));
                    ui.add_space(4.0);
                    ui.horizontal(|ui| {
                        for m in p.top_mastery.iter().take(5) {
                            ui.vertical(|ui| {
                                ui.spacing_mut().item_spacing.y = 2.0;
                                champion_icon(ui, m.champion_id, 34.0);
                                ui::muted(ui, format!("Lv.{}", m.level));
                                ui::muted(ui, format!("{}k", m.points / 1000));
                            });
                        }
                    });
                }
            },
        );
    }

    fn position_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "포지션 분포",
            |_ui| {},
            |ui| {
                let slot = self.positions.lock();
                let Some(entries) = slot.value.as_ref().filter(|v| !v.is_empty()) else {
                    ui::muted(ui, "포지션 데이터가 없습니다");
                    return;
                };
                let total: i32 = entries.iter().map(|e| e.games).sum::<i32>().max(1);
                let mut sorted: Vec<&PositionPoolEntry> = entries.iter().collect();
                sorted.sort_by_key(|e| std::cmp::Reverse(e.games));

                if let Some(main) = sorted.first() {
                    ui.label(ui::txt(
                        format!("주 포지션: {}", position_label(&main.position)),
                        13.0,
                        ui::W::Semibold,
                        BLUE_600,
                    ));
                    ui.add_space(6.0);
                }

                for e in sorted {
                    let share = e.games as f64 / total as f64 * 100.0;
                    ui.horizontal(|ui| {
                        ui::cell(ui, 56.0, |ui| ui::td(ui, position_label(&e.position)));
                        ui::bar(ui, (share / 100.0) as f32, 96.0, BLUE_500);
                        ui::cell(ui, 46.0, |ui| ui::num(ui, format!("{share:.0}%"), GRAY_900));
                        ui::cell(ui, 46.0, |ui| ui::muted(ui, format!("{}판", e.games)));
                        ui::cell(ui, 52.0, |ui| {
                            ui::num(ui, ui::pct(e.win_rate), win_rate_color(e.win_rate))
                        });
                    });
                }
            },
        );
    }

    fn elo_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "Elo 히스토리",
            |ui| ui::muted(ui, "최근 10경기"),
            |ui| {
                let slot = self.elo_history.lock();
                let Some(h) = slot.value.as_ref().filter(|h| !h.history.is_empty()) else {
                    ui::muted(ui, "Elo 기록이 없습니다");
                    return;
                };
                for e in h.history.iter().take(10) {
                    ui::table_row(ui, |ui| {
                        ui::cell(ui, 34.0, |ui| {
                            let (bg, fg) = if e.win { (WIN_BG, WIN) } else { (LOSS_BG, LOSS) };
                            ui::chip(ui, if e.win { "승" } else { "패" }, bg, fg);
                        });
                        ui::cell(ui, 62.0, |ui| {
                            ui::num(ui, format!("{:.0}", e.elo_after), GRAY_900)
                        });
                        ui::cell(ui, 56.0, |ui| {
                            let color = if e.delta >= 0.0 { WIN } else { LOSS };
                            let sign = if e.delta >= 0.0 { "+" } else { "" };
                            ui::num(ui, format!("{sign}{:.0}", e.delta), color)
                        });
                        // 라인 맞대결 결과. 이게 실제로 라인 레이팅을 움직인 신호다.
                        // (예전의 lane_performance 점수는 검증에서 탈락해 사라졌다.)
                        ui::cell(ui, 86.0, |ui| {
                            let text = match e.lane_result.as_str() {
                                "WIN" => "라인 승",
                                "LOSS" => "라인 패",
                                _ => "라인 -",
                            };
                            ui::muted(ui, text.to_owned())
                        });
                        ui::cell(ui, 56.0, |ui| {
                            ui::muted(ui, ui::short_date_kst(e.game_creation))
                        });
                    });
                }
            },
        );
    }

    fn dna_card(&self, ui: &mut egui::Ui) {
        ui::card_with_head(
            ui,
            "플레이스타일 DNA",
            |_ui| {},
            |ui| {
                let slot = self.dna.lock();
                let Some(Some(d)) = slot.value.as_ref() else {
                    ui::muted(ui, "DNA 데이터가 없습니다");
                    return;
                };
                if !d.style_tag.is_empty() {
                    ui::chip_blue(ui, &d.style_tag);
                    ui.add_space(8.0);
                }
                for (label, value) in d.axes() {
                    ui.horizontal(|ui| {
                        ui::cell(ui, 68.0, |ui| ui::td(ui, label));
                        ui::bar(ui, (value / 100.0) as f32, 110.0, BLUE_500);
                        ui::cell(ui, 40.0, |ui| ui::num(ui, format!("{value:.0}"), GRAY_900));
                    });
                }
            },
        );
    }

    fn recent_card(&mut self, ui: &mut egui::Ui, ctx: &egui::Context, d: &SummonerResult) {
        if d.recent_matches.is_empty() {
            return;
        }
        let searched = d.profile.riot_id.clone();
        let mut toggle: Option<String> = None;

        ui::card_with_head(
            ui,
            &format!("최근 경기 ({}게임)", d.recent_matches.len()),
            |_ui| {},
            |ui| {
                for m in &d.recent_matches {
                    let open = self.expanded.as_deref() == Some(m.match_id.as_str());
                    if recent_row(ui, m, open) {
                        toggle = Some(m.match_id.clone());
                    }
                    if open {
                        let remote = self
                            .match_details
                            .entry(m.match_id.clone())
                            .or_insert_with(Remote::new);
                        remote.ensure(ctx, crate::api::match_detail(m.match_id.clone()));
                        let slot = remote.lock();
                        match (&slot.value, &slot.error) {
                            (Some(detail), _) => {
                                ui.add_space(6.0);
                                match_teams(ui, detail, &searched);
                                ui.add_space(6.0);
                            }
                            (None, Some(e)) => ui::error(ui, e),
                            (None, None) => ui::muted(ui, "불러오는 중..."),
                        }
                    }
                }
            },
        );

        if let Some(id) = toggle {
            self.expanded = if self.expanded.as_deref() == Some(id.as_str()) {
                None
            } else {
                Some(id)
            };
        }
    }
}

fn summary_card(ui: &mut egui::Ui, d: &SummonerResult) {
    let p = &d.profile;
    ui::card(ui, |ui| {
        ui.horizontal(|ui| {
            ui::stat_hero(ui, "승률", &ui::pct(p.win_rate), win_rate_color(p.win_rate));
            ui.add_space(28.0);
            ui.vertical(|ui| {
                ui.spacing_mut().item_spacing.y = 2.0;
                ui.label(ui::txt("전적", 13.0, ui::W::Regular, GRAY_600));
                ui.horizontal(|ui| {
                    ui.label(ui::txt(format!("{}승", p.wins), 22.0, ui::W::Bold, WIN));
                    ui.label(ui::txt(format!("{}패", p.losses), 22.0, ui::W::Bold, LOSS));
                });
            });
            ui.add_space(28.0);
            ui::stat(ui, "KDA", &format!("{:.2}", p.kda), GRAY_900);
            ui.add_space(28.0);
            ui::stat(
                ui,
                "평균 K/D/A",
                &format!("{:.1} / {:.1} / {:.1}", p.avg_kills, p.avg_deaths, p.avg_assists),
                GRAY_900,
            );

            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                ui.vertical(|ui| {
                    ui.spacing_mut().item_spacing.y = 2.0;
                    ui.label(ui::txt(short_name(&p.riot_id), 17.0, ui::W::Bold, GRAY_900));
                    ui.label(ui::txt(
                        format!("#{} · {}판", tag_of(&p.riot_id), p.games),
                        12.0,
                        ui::W::Regular,
                        GRAY_500,
                    ));
                    if let Some(elo) = p.elo.filter(|e| e.is_finite() && *e > 0.0) {
                        let rank = match (p.elo_rank, p.elo_ranked_total) {
                            (Some(r), Some(total)) => format!(" · {r}위 / {total}명"),
                            (Some(r), None) => format!(" · {r}위"),
                            _ => String::new(),
                        };
                        ui.label(ui::txt(
                            format!("Elo {elo:.0}{rank}"),
                            13.0,
                            ui::W::Semibold,
                            elo_color(Some(elo)),
                        ));
                    }
                    ui::sample_chip(ui, p.sample_grade);
                });
            });
        });
    });
}

fn streak_card(ui: &mut egui::Ui, streak: Option<&Streak>) {
    ui::card_with_head(
        ui,
        "스트릭",
        |_ui| {},
        |ui| {
            let Some(s) = streak else {
                ui::muted(ui, "스트릭 데이터가 없습니다");
                return;
            };
            let win = s.is_win_streak();
            let color = if win { WIN } else { LOSS };
            ui::stat(
                ui,
                "현재",
                &format!("{}{}", s.length(), if win { "연승" } else { "연패" }),
                color,
            );
            ui.add_space(6.0);
            ui.horizontal(|ui| {
                ui::muted(ui, format!("최장 연승 {}", s.longest_win));
                ui::muted(ui, format!("최장 연패 {}", s.longest_loss));
            });
            if !s.recent_form.is_empty() {
                ui.add_space(10.0);
                ui.label(ui::txt("최근 전적", 13.0, ui::W::Semibold, GRAY_600));
                ui.add_space(4.0);
                ui::form_dots(ui, &s.form_oldest_first(10));
            }
        },
    );
}

fn champion_card(ui: &mut egui::Ui, d: &SummonerResult) {
    if d.champion_stats.is_empty() {
        return;
    }
    ui::card_with_head(
        ui,
        "챔피언별 통계",
        |ui| ui::muted(ui, format!("{}개 챔피언", d.champion_stats.len())),
        |ui| {
            ui::table_row(ui, |ui| {
                ui::cell(ui, 34.0, |_ui| {});
                ui::cell(ui, 130.0, |ui| ui::th(ui, "챔피언"));
                ui::cell(ui, 50.0, |ui| ui::th(ui, "판수"));
                ui::cell(ui, 96.0, |ui| ui::th(ui, "승률"));
                ui::cell(ui, 56.0, |ui| ui::th(ui, "KDA"));
                ui::cell(ui, 116.0, |ui| ui::th(ui, "평균 K/D/A"));
                ui::cell(ui, 70.0, |ui| ui::th(ui, "평균 딜"));
            });
            for c in &d.champion_stats {
                ui::table_row(ui, |ui| {
                    ui::cell(ui, 34.0, |ui| {
                        champion_icon(ui, c.champion_id, 26.0);
                    });
                    ui::cell(ui, 130.0, |ui| ui::td(ui, c.champion.clone()));
                    ui::cell(ui, 50.0, |ui| ui::muted(ui, format!("{}판", c.games)));
                    ui::cell(ui, 96.0, |ui| {
                        ui::bar(ui, (c.win_rate / 100.0) as f32, 40.0, win_rate_color(c.win_rate));
                        ui::num(ui, ui::pct(c.win_rate), win_rate_color(c.win_rate));
                    });
                    ui::cell(ui, 56.0, |ui| ui::num(ui, format!("{:.2}", c.kda), GRAY_900));
                    ui::cell(ui, 116.0, |ui| {
                        ui::muted(
                            ui,
                            format!("{:.1}/{:.1}/{:.1}", c.avg_kills, c.avg_deaths, c.avg_assists),
                        )
                    });
                    ui::cell(ui, 70.0, |ui| ui::muted(ui, ui::short_num(c.avg_damage)));
                });
            }
        },
    );
}

fn rank_block(ui: &mut egui::Ui, label: &str, rank: Option<&RankedInfo>) {
    ui.vertical(|ui| {
        ui.spacing_mut().item_spacing.y = 2.0;
        ui.label(ui::txt(label, 13.0, ui::W::Regular, GRAY_600));
        match rank {
            Some(r) => {
                ui.label(ui::txt(
                    format!("{} {} · {}LP", r.tier, r.rank, r.lp),
                    17.0,
                    ui::W::Bold,
                    GRAY_900,
                ));
                ui.horizontal(|ui| {
                    ui::muted(ui, format!("{}승 {}패", r.wins, r.losses));
                    ui.label(ui::txt(
                        ui::pct(r.win_rate),
                        12.0,
                        ui::W::Semibold,
                        win_rate_color(r.win_rate),
                    ));
                });
            }
            None => {
                ui.label(ui::txt("Unranked", 17.0, ui::W::Bold, GRAY_400));
            }
        }
    });
}

/// 클릭되면 true.
fn recent_row(ui: &mut egui::Ui, m: &RecentMatch, expanded: bool) -> bool {
    let kda = if m.deaths == 0 {
        "완벽".to_owned()
    } else {
        format!("{:.2}", (m.kills + m.assists) as f64 / m.deaths as f64)
    };
    let mut clicked = false;
    ui::result_frame(ui, m.win, |ui| {
        let r = ui.horizontal(|ui| {
            ui::cell(ui, 34.0, |ui| {
                let (bg, fg) = if m.win { (WIN_BG, WIN) } else { (LOSS_BG, LOSS) };
                ui::chip(ui, if m.win { "승" } else { "패" }, bg, fg);
            });
            champion_icon(ui, m.champion_id, 30.0);
            ui.add_space(6.0);
            ui::cell(ui, 96.0, |ui| {
                ui::num(ui, format!("{}/{}/{}", m.kills, m.deaths, m.assists), GRAY_900)
            });
            ui::cell(ui, 76.0, |ui| ui::muted(ui, format!("{kda} KDA")));
            ui::cell(ui, 70.0, |ui| ui::muted(ui, format!("CS {}", m.cs)));
            ui::cell(ui, 74.0, |ui| {
                ui::muted(ui, format!("{} 딜", ui::short_num(m.damage as f64)))
            });
            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                ui.label(ui::txt(
                    if expanded { "접기" } else { "펼치기" },
                    12.0,
                    ui::W::Semibold,
                    GRAY_500,
                ));
                ui.add_space(8.0);
                ui::muted(ui, queue_label(m.queue_id));
                ui::muted(ui, ui::mmss(m.game_duration as i64));
                ui::muted(ui, ui::short_date_kst(m.game_creation));
            });
        });
        if r.response.interact(egui::Sense::click()).clicked() {
            clicked = true;
        }
    });
    clicked
}

fn match_teams(ui: &mut egui::Ui, detail: &MatchDetail, searched: &str) {
    let blue: Vec<&MatchParticipant> = detail
        .participants
        .iter()
        .filter(|p| p.team == "blue")
        .collect();
    let red: Vec<&MatchParticipant> = detail
        .participants
        .iter()
        .filter(|p| p.team == "red")
        .collect();
    let blue_win = blue.first().map(|p| p.win).unwrap_or(false);

    ui::two_columns(
        ui,
        |ui| team_rows(ui, "블루팀", TEAM_BLUE, blue_win, &blue, searched),
        |ui| team_rows(ui, "레드팀", TEAM_RED, !blue_win, &red, searched),
    );
}

fn team_rows(
    ui: &mut egui::Ui,
    title: &str,
    color: egui::Color32,
    won: bool,
    players: &[&MatchParticipant],
    searched: &str,
) {
    ui.horizontal(|ui| {
        ui.label(ui::txt(title, 14.0, ui::W::Bold, color));
        ui.label(ui::txt(
            if won { "승" } else { "패" },
            13.0,
            ui::W::Semibold,
            if won { WIN } else { LOSS },
        ));
    });
    ui.add_space(4.0);
    for p in players {
        let is_me = p.riot_id == searched;
        ui::table_row(ui, |ui| {
            ui::cell(ui, 26.0, |ui| {
                champion_icon(ui, p.champion_id, 22.0);
            });
            ui::cell(ui, 110.0, |ui| ui::person(ui, &p.riot_id, is_me));
            ui::cell(ui, 66.0, |ui| {
                ui::td(ui, format!("{}/{}/{}", p.kills, p.deaths, p.assists))
            });
            ui::cell(ui, 46.0, |ui| ui::muted(ui, format!("{}CS", p.cs)));
            ui::cell(ui, 56.0, |ui| {
                ui::muted(ui, format!("{} 딜", ui::short_num(p.damage as f64)))
            });
        });
    }
}
