//! 웹 디자인 시스템(`frontend/src/styles/ds.css`)의 컴포넌트를 egui 로 옮긴 것.
//!
//! 클래스 이름을 함수 이름에 그대로 남겨 뒀다 — `.t-card` → [`card`],
//! `.t-chip` → [`chip`]. 웹에서 스타일이 바뀌면 짝을 찾아 같이 고치면 된다.

use egui::{Color32, FontId, Response, RichText, Sense, Stroke, Ui, Vec2};

use crate::theme::*;

// ── 타이포 ───────────────────────────────────────

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum W {
    Regular,
    Semibold,
    Bold,
}

impl W {
    fn family(self) -> egui::FontFamily {
        match self {
            W::Regular => family(F_REGULAR),
            W::Semibold => family(F_SEMIBOLD),
            W::Bold => family(F_BOLD),
        }
    }
}

pub fn txt(s: impl Into<String>, size: f32, weight: W, color: Color32) -> RichText {
    RichText::new(s).font(FontId::new(size, weight.family())).color(color)
}

/// `.t-page-title`
pub fn page_title(ui: &mut Ui, title: &str) {
    ui.label(txt(title, 26.0, W::Bold, GRAY_900));
}

/// `.t-page-sub`
pub fn page_sub(ui: &mut Ui, sub: &str) {
    ui.label(txt(sub, 14.0, W::Regular, GRAY_600));
}

/// `.t-card-title`
pub fn card_title(ui: &mut Ui, title: &str) {
    ui.label(txt(title, 16.0, W::Bold, GRAY_900));
}

/// 칸을 넘치면 줄바꿈 대신 말줄임. 웹의 `.t-table td { white-space: nowrap }` 과 같다.
/// 표에서 한 칸이 두 줄이 되면 행 높이가 들쭉날쭉해져 읽기 어렵다.
fn clipped(ui: &mut Ui, text: RichText) -> Response {
    ui.add(egui::Label::new(text).truncate())
}

/// 표 머리글 `.t-table th`
pub fn th(ui: &mut Ui, s: &str) {
    clipped(ui, txt(s, 12.0, W::Semibold, GRAY_500));
}

/// 표 본문 `.t-table td`
pub fn td(ui: &mut Ui, s: impl Into<String>) {
    clipped(ui, txt(s, 14.0, W::Regular, GRAY_800));
}

/// 숫자 칸. 웹은 `font-variant-numeric: tabular-nums` 로 자릿수를 맞춘다.
/// Pretendard 는 기본이 이미 등폭 숫자라 굵기만 맞춰 준다.
pub fn num(ui: &mut Ui, s: impl Into<String>, color: Color32) {
    clipped(ui, txt(s, 14.0, W::Semibold, color));
}

pub fn muted(ui: &mut Ui, s: impl Into<String>) {
    clipped(ui, txt(s, 12.0, W::Regular, GRAY_500));
}

/// `.t-empty`
pub fn empty(ui: &mut Ui, s: &str) {
    ui.vertical_centered(|ui| {
        ui.add_space(28.0);
        ui.label(txt(s, 14.0, W::Regular, GRAY_500));
        ui.add_space(28.0);
    });
}

pub fn error(ui: &mut Ui, s: &str) {
    ui.label(txt(s, 13.0, W::Regular, ERROR));
}

// ── 레이아웃 ─────────────────────────────────────

/// `.t-page` — 가운데 정렬 · 최대 폭 제한 · 세로 스크롤.
///
/// 폭을 명시적으로 못 박는 게 중요하다. egui 의 `available_width` 는 스크롤바가
/// 붙기 전 값이라, 그대로 쓰면 카드가 오른쪽으로 삐져나간다.
pub fn page(ui: &mut Ui, title: &str, subtitle: &str, add: impl FnOnce(&mut Ui)) {
    /// 좌우 여백. 웹 `.t-page` 의 `padding: 28px 24px`.
    const GUTTER: f32 = 24.0;

    egui::ScrollArea::vertical()
        // 페이지마다 스크롤 위치를 따로 기억한다. 하나로 묶으면 다른 탭에서
        // 내려 둔 위치 그대로 열려서 제목이 안 보인다.
        .id_salt(title)
        .auto_shrink([false, false])
        .show(ui, |ui| {
            // 스크롤바 자리는 available_width 에 안 잡힌다. 빼 주지 않으면 오른쪽
            // 카드가 스크롤바 밑으로 밀려 잘린다.
            let bar = ui.spacing().scroll.bar_width + ui.spacing().scroll.bar_inner_margin;
            let avail = ui.available_width() - bar;
            let width = (avail - GUTTER * 2.0).min(CONTENT_MAX).max(320.0);
            ui.allocate_ui_with_layout(
                egui::vec2(avail, 0.0),
                egui::Layout::top_down(egui::Align::Center),
                |ui| {
                    ui.set_width(avail);
                    ui.add_space(28.0);
                    ui.allocate_ui_with_layout(
                        egui::vec2(width, 0.0),
                        egui::Layout::top_down(egui::Align::Min),
                        |ui| {
                            ui.set_width(width);
                            ui.scope(|ui| {
                                ui.spacing_mut().item_spacing.y = 4.0;
                                page_title(ui, title);
                                if !subtitle.is_empty() {
                                    page_sub(ui, subtitle);
                                }
                            });
                            ui.add_space(16.0);
                            ui.scope(|ui| {
                                ui.spacing_mut().item_spacing.y = 16.0;
                                add(ui);
                            });
                        },
                    );
                    ui.add_space(64.0);
                },
            );
        });
}

/// `.t-card` — 흰 면, radius 16, padding 20. 테두리 대신 면으로 띄운다.
pub fn card<R>(ui: &mut Ui, add: impl FnOnce(&mut Ui) -> R) -> R {
    egui::Frame::new()
        .fill(BG_SURFACE)
        .corner_radius(cr(R_XL))
        .inner_margin(egui::Margin::same(20))
        .show(ui, |ui| {
            ui.set_width(ui.available_width());
            add(ui)
        })
        .inner
}

/// `.t-card.t-card-sunken` — 회색 바탕 위에서 한 단계 더 눌러야 할 때.
pub fn card_sunken<R>(ui: &mut Ui, add: impl FnOnce(&mut Ui) -> R) -> R {
    egui::Frame::new()
        .fill(GRAY_50)
        .corner_radius(cr(R_LG))
        .inner_margin(egui::Margin::same(14))
        .show(ui, |ui| {
            ui.set_width(ui.available_width());
            add(ui)
        })
        .inner
}

/// 제목이 붙은 카드. `.t-card-head` 자리에 오른쪽 위젯을 붙일 수 있다.
pub fn card_with_head<R>(
    ui: &mut Ui,
    title: &str,
    right: impl FnOnce(&mut Ui),
    add: impl FnOnce(&mut Ui) -> R,
) -> R {
    card(ui, |ui| {
        ui.horizontal(|ui| {
            card_title(ui, title);
            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), right);
        });
        ui.add_space(10.0);
        add(ui)
    })
}

/// 카드 두 장을 좌우로 나란히. 좁아지면 세로로 쌓는다 (웹의 `.t-grid` 와 같은 규칙).
pub fn two_columns(ui: &mut Ui, left: impl FnOnce(&mut Ui), right: impl FnOnce(&mut Ui)) {
    let gap = 16.0;
    let total = ui.available_width();
    if total < 640.0 {
        left(ui);
        ui.add_space(gap);
        right(ui);
        return;
    }
    // 폭 계산은 egui 에 맡긴다. 직접 반씩 나눠 주면 부모의 max_rect 가 실제보다
    // 넓게 잡히는 경우가 있어 오른쪽 카드가 화면 밖으로 밀린다.
    ui.spacing_mut().item_spacing.x = gap;
    ui.columns(2, |cols| {
        left(&mut cols[0]);
        right(&mut cols[1]);
    });
}

/// 고정 폭 칸. 표에서 열을 맞출 때 쓴다.
pub fn cell<R>(ui: &mut Ui, width: f32, add: impl FnOnce(&mut Ui) -> R) -> R {
    // 남은 폭보다 넓게 잡으면 카드 밖으로 삐져나간다. 마지막 칸이 좁아지는 편이 낫다.
    let width = width.min(ui.available_width().max(24.0));
    ui.allocate_ui_with_layout(
        egui::vec2(width, ui.spacing().interact_size.y),
        egui::Layout::left_to_right(egui::Align::Center),
        |ui| {
            ui.set_width(width);
            add(ui)
        },
    )
    .inner
}

/// 남은 폭을 다 쓰는 칸. 이름처럼 길이가 들쭉날쭉한 열에 쓴다.
/// `reserve` 는 이 칸 뒤에 올 고정 칸들의 폭 합(칸 사이 간격 포함).
pub fn flex_cell<R>(ui: &mut Ui, reserve: f32, add: impl FnOnce(&mut Ui) -> R) -> R {
    let width = (ui.available_width() - reserve).max(48.0);
    cell(ui, width, add)
}

/// 판수 칸. 표본이 못 미더우면 주황 배지로, 충분하면 흐린 글씨로.
pub fn games_cell(ui: &mut Ui, width: f32, games: i32, grade: crate::models::SampleGrade) {
    cell(ui, width, |ui| {
        let text = format!("{games}판");
        if grade.warns() {
            chip(ui, &text, WARN_BG, WARN_FG);
        } else {
            muted(ui, text);
        }
    });
}

/// 표 왼쪽의 순위 번호 칸.
pub fn rank_cell(ui: &mut Ui, rank: usize) {
    cell(ui, 30.0, |ui| {
        ui.label(txt(rank.to_string(), 13.0, W::Semibold, GRAY_500))
    });
}

/// 표의 한 행. 웹은 행 구분을 선이 아니라 `inset 0 -1px` 의 아주 얕은 면으로 한다.
///
/// 폭을 명시적으로 못 박는다. 그냥 `horizontal` 로 두면 칸 폭의 합이 카드보다
/// 넓을 때 행이 카드를 밀어 넓혀 버려서, 옆 카드가 화면 밖으로 나간다.
pub fn table_row<R>(ui: &mut Ui, add: impl FnOnce(&mut Ui) -> R) -> R {
    let width = ui.available_width();
    let r = ui
        .allocate_ui_with_layout(
            egui::vec2(width, 0.0),
            egui::Layout::left_to_right(egui::Align::Center),
            |ui| {
                ui.set_max_width(width);
                ui.spacing_mut().item_spacing.x = 8.0;
                ui.add_space(2.0);
                add(ui)
            },
        )
        .inner;
    let rect = ui.min_rect();
    let y = ui.cursor().top() - 2.0;
    ui.painter().hline(rect.x_range(), y, Stroke::new(1.0, GRAY_100));
    ui.add_space(2.0);
    r
}

// ── 숫자 · 배지 ──────────────────────────────────

/// `.t-stat` — 라벨 위, 값 아래.
pub fn stat(ui: &mut Ui, label: &str, value: &str, color: Color32) {
    stat_sized(ui, label, value, color, 22.0);
}

/// `.t-stat.t-stat-hero` — 한 화면에 하나만 쓴다.
pub fn stat_hero(ui: &mut Ui, label: &str, value: &str, color: Color32) {
    stat_sized(ui, label, value, color, 40.0);
}

fn stat_sized(ui: &mut Ui, label: &str, value: &str, color: Color32, size: f32) {
    ui.vertical(|ui| {
        ui.spacing_mut().item_spacing.y = 2.0;
        // 숫자는 절대 줄바꿈하지 않는다. "1844" 가 "184 / 4" 로 쪼개지면 오독한다.
        ui.style_mut().wrap_mode = Some(egui::TextWrapMode::Extend);
        ui.label(txt(label, 13.0, W::Regular, GRAY_600));
        ui.label(txt(value, size, W::Bold, color));
    });
}

/// `.t-chip`
pub fn chip(ui: &mut Ui, text: &str, bg: Color32, fg: Color32) {
    egui::Frame::new()
        .fill(bg)
        .corner_radius(cr(11))
        .inner_margin(egui::Margin::symmetric(8, 3))
        .show(ui, |ui| {
            ui.label(txt(text, 12.0, W::Semibold, fg));
        });
}

pub fn chip_neutral(ui: &mut Ui, text: &str) {
    chip(ui, text, GRAY_100, GRAY_700);
}

pub fn chip_win(ui: &mut Ui, text: &str) {
    chip(ui, text, WIN_BG, WIN);
}

pub fn chip_loss(ui: &mut Ui, text: &str) {
    chip(ui, text, LOSS_BG, LOSS);
}

pub fn chip_blue(ui: &mut Ui, text: &str) {
    chip(ui, text, BLUE_50, BLUE_600);
}

/// `.t-chip-low` — 표본이 못 미더울 때만 눈에 띈다. 충분하면 배지를 아예 안 단다.
pub fn sample_chip(ui: &mut Ui, grade: crate::models::SampleGrade) {
    if grade.warns() {
        chip(ui, grade.label(), WARN_BG, WARN_FG);
    }
}

/// `.t-tier` — S/A/B/C 한 글자 배지.
pub fn tier_badge(ui: &mut Ui, tier: &str) {
    let (bg, fg) = tier_colors(tier);
    let (rect, _) = ui.allocate_exact_size(Vec2::splat(24.0), Sense::hover());
    ui.painter().rect_filled(rect, cr(7), bg);
    ui.painter().text(
        rect.center(),
        egui::Align2::CENTER_CENTER,
        tier,
        FontId::new(12.0, W::Bold.family()),
        fg,
    );
}

/// `.t-bar` — 승률 막대.
pub fn bar(ui: &mut Ui, fraction: f32, width: f32, color: Color32) {
    let (rect, _) = ui.allocate_exact_size(egui::vec2(width, 6.0), Sense::hover());
    ui.painter().rect_filled(rect, cr(3), GRAY_200);
    let f = fraction.clamp(0.0, 1.0);
    if f > 0.0 {
        let filled = egui::Rect::from_min_size(rect.min, egui::vec2(rect.width() * f, rect.height()));
        ui.painter().rect_filled(filled, cr(3), color);
    }
}

/// 두 팀의 비율을 하나의 막대로. 왼쪽이 블루, 오른쪽이 레드.
pub fn split_bar(ui: &mut Ui, blue_fraction: f32) {
    let width = ui.available_width();
    let (rect, _) = ui.allocate_exact_size(egui::vec2(width, 8.0), Sense::hover());
    let f = blue_fraction.clamp(0.03, 0.97);
    let split = rect.min.x + rect.width() * f;
    ui.painter().rect_filled(rect, cr(4), TEAM_RED);
    let left = egui::Rect::from_min_max(rect.min, egui::pos2(split, rect.max.y));
    ui.painter().rect_filled(left, cr(4), TEAM_BLUE);
}

/// `.t-form` — 최근 전적 W/L 점.
pub fn form_dots(ui: &mut Ui, form: &[bool]) {
    ui.horizontal(|ui| {
        ui.spacing_mut().item_spacing.x = 3.0;
        for win in form {
            let (rect, _) = ui.allocate_exact_size(Vec2::splat(18.0), Sense::hover());
            let (bg, fg) = if *win { (WIN_BG, WIN) } else { (LOSS_BG, LOSS) };
            ui.painter().rect_filled(rect, cr(R_SM), bg);
            ui.painter().text(
                rect.center(),
                egui::Align2::CENTER_CENTER,
                if *win { "승" } else { "패" },
                FontId::new(10.0, W::Bold.family()),
                fg,
            );
        }
    });
}

/// `.t-person` — 이름 + 태그. 칸을 넘치면 이름부터 말줄임한다.
pub fn person(ui: &mut Ui, riot_id: &str, highlight: bool) {
    ui.horizontal(|ui| {
        ui.spacing_mut().item_spacing.x = 4.0;
        let color = if highlight { BLUE_600 } else { GRAY_900 };
        let weight = if highlight { W::Bold } else { W::Semibold };
        clipped(ui, txt(crate::models::short_name(riot_id), 14.0, weight, color));
        let tag = crate::models::tag_of(riot_id);
        if !tag.is_empty() {
            clipped(ui, txt(format!("#{tag}"), 12.0, W::Regular, GRAY_500));
        }
    });
}

/// `.t-result` — 승패를 왼쪽 4px 막대로만 말한다. 배경을 물들이면 표가 시끄러워진다.
pub fn result_frame<R>(ui: &mut Ui, win: bool, add: impl FnOnce(&mut Ui) -> R) -> R {
    let color = if win { WIN } else { LOSS };
    let response = egui::Frame::new()
        .fill(BG_SURFACE)
        .corner_radius(cr(R_LG))
        .inner_margin(egui::Margin {
            left: 16,
            right: 14,
            top: 12,
            bottom: 12,
        })
        .show(ui, |ui| {
            ui.set_width(ui.available_width());
            add(ui)
        });
    let rect = response.response.rect;
    let bar = egui::Rect::from_min_max(rect.min, egui::pos2(rect.min.x + 4.0, rect.max.y));
    ui.painter().rect_filled(bar, cr(R_LG), color);
    response.inner
}

// ── 컨트롤 ───────────────────────────────────────

/// `.t-seg` — 모드 전환처럼 선택지가 적을 때.
pub fn segmented<T: PartialEq + Copy>(ui: &mut Ui, current: &mut T, options: &[(T, &str)]) -> bool {
    let mut changed = false;
    egui::Frame::new()
        .fill(GRAY_100)
        .corner_radius(cr(10))
        .inner_margin(egui::Margin::same(3))
        .show(ui, |ui| {
            ui.horizontal(|ui| {
                ui.spacing_mut().item_spacing.x = 2.0;
                for (value, label) in options {
                    let selected = *current == *value;
                    let (bg, fg) = if selected {
                        (GRAY_0, GRAY_900)
                    } else {
                        (Color32::TRANSPARENT, GRAY_600)
                    };
                    let text = txt(*label, 13.0, W::Semibold, fg);
                    let button = egui::Button::new(text)
                        .fill(bg)
                        .stroke(Stroke::NONE)
                        .corner_radius(cr(R_MD))
                        .min_size(egui::vec2(0.0, 26.0));
                    if ui.add(button).clicked() && !selected {
                        *current = *value;
                        changed = true;
                    }
                }
            });
        });
    changed
}

/// `.t-tabs` — 밑줄 탭.
pub fn tabs<T: PartialEq + Copy>(ui: &mut Ui, current: &mut T, options: &[(T, &str)]) -> bool {
    let mut changed = false;
    let start = ui.cursor().top();
    ui.horizontal(|ui| {
        ui.spacing_mut().item_spacing.x = 4.0;
        for (value, label) in options {
            let selected = *current == *value;
            let color = if selected { GRAY_900 } else { GRAY_500 };
            let weight = if selected { W::Bold } else { W::Semibold };
            let button = egui::Button::new(txt(*label, 15.0, weight, color))
                .fill(Color32::TRANSPARENT)
                .stroke(Stroke::NONE)
                .min_size(egui::vec2(0.0, 34.0));
            let r = ui.add(button);
            if r.clicked() && !selected {
                *current = *value;
                changed = true;
            }
            if selected {
                let rect = r.rect;
                ui.painter().hline(
                    (rect.left() + 6.0)..=(rect.right() - 6.0),
                    rect.bottom(),
                    Stroke::new(2.0, GRAY_900),
                );
            }
        }
    });
    let bottom = ui.min_rect().bottom().max(start + 34.0);
    ui.painter()
        .hline(ui.min_rect().x_range(), bottom, Stroke::new(1.0, GRAY_200));
    ui.add_space(6.0);
    changed
}

/// `.btn.btn-primary`
pub fn primary_button(ui: &mut Ui, label: &str, enabled: bool) -> Response {
    let fg = if enabled { GRAY_0 } else { GRAY_0 };
    let bg = if enabled { BLUE_500 } else { BLUE_300 };
    ui.add_enabled(
        enabled,
        egui::Button::new(txt(label, 14.0, W::Semibold, fg))
            .fill(bg)
            .stroke(Stroke::NONE)
            .corner_radius(cr(R_MD))
            .min_size(egui::vec2(0.0, 36.0)),
    )
}

/// `.btn.btn-secondary`
pub fn secondary_button(ui: &mut Ui, label: &str, enabled: bool) -> Response {
    ui.add_enabled(
        enabled,
        egui::Button::new(txt(label, 13.0, W::Semibold, GRAY_700))
            .fill(GRAY_100)
            .stroke(Stroke::NONE)
            .corner_radius(cr(R_MD))
            .min_size(egui::vec2(0.0, 32.0)),
    )
}

/// `.t-search` — 회색 면 위의 입력.
pub fn search_input(ui: &mut Ui, value: &mut String, placeholder: &str) -> Response {
    let width = ui.available_width();
    egui::Frame::new()
        .fill(GRAY_100)
        .corner_radius(cr(10))
        .inner_margin(egui::Margin::symmetric(12, 8))
        .show(ui, |ui| {
            ui.set_width(width - 24.0);
            ui.add(
                egui::TextEdit::singleline(value)
                    .desired_width(f32::INFINITY)
                    .frame(egui::Frame::NONE)
                    .hint_text(txt(placeholder, 14.0, W::Regular, GRAY_500))
                    .font(FontId::new(14.0, W::Regular.family()))
                    .text_color(GRAY_900),
            )
        })
        .inner
}

/// 안내 배너. 자동 수집 상태나 업데이트 알림처럼 화면 위쪽에 한 줄로 뜬다.
pub fn banner(ui: &mut Ui, text: &str, accent: Color32, bg: Color32, right: impl FnOnce(&mut Ui)) {
    egui::Frame::new()
        .fill(bg)
        .corner_radius(cr(R_LG))
        .inner_margin(egui::Margin::symmetric(14, 10))
        .show(ui, |ui| {
            ui.set_width(ui.available_width());
            ui.horizontal(|ui| {
                ui.label(txt(text, 13.0, W::Semibold, accent));
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), right);
            });
        });
}

/// 승률 등 비율을 "62.5%" 로. 소수 한 자리는 표에서 자릿수를 맞추기 위한 것이다.
pub fn pct(v: f64) -> String {
    format!("{v:.1}%")
}

/// 큰 숫자를 "12.3k" 로 줄인다.
pub fn short_num(v: f64) -> String {
    if v.abs() >= 1000.0 {
        format!("{:.1}k", v / 1000.0)
    } else {
        format!("{:.0}", v)
    }
}

/// 초를 "12:34" 로.
pub fn mmss(seconds: i64) -> String {
    let s = seconds.max(0);
    format!("{}:{:02}", s / 60, s % 60)
}

/// epoch millis → "2024.01.01" (Asia/Seoul).
pub fn date_kst(millis: i64) -> String {
    use chrono::{FixedOffset, TimeZone};
    let kst = FixedOffset::east_opt(9 * 3600).expect("고정 오프셋");
    kst.timestamp_millis_opt(millis)
        .single()
        .map(|d| d.format("%Y.%m.%d").to_string())
        .unwrap_or_default()
}

/// epoch millis → "1/1" (Asia/Seoul).
pub fn short_date_kst(millis: i64) -> String {
    use chrono::{FixedOffset, TimeZone};
    let kst = FixedOffset::east_opt(9 * 3600).expect("고정 오프셋");
    kst.timestamp_millis_opt(millis)
        .single()
        .map(|d| d.format("%-m/%-d").to_string())
        .unwrap_or_default()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn formats_numbers_like_the_web() {
        assert_eq!(pct(62.5), "62.5%");
        assert_eq!(short_num(24_300.0), "24.3k");
        assert_eq!(short_num(940.0), "940");
        assert_eq!(mmss(754), "12:34");
        assert_eq!(mmss(-5), "0:00");
    }

    #[test]
    fn dates_use_seoul_time() {
        // 2023-12-31T16:00:00Z = KST 2024-01-01 01:00
        assert_eq!(date_kst(1_704_038_400_000), "2024.01.01");
        assert_eq!(short_date_kst(1_704_038_400_000), "1/1");
    }
}
