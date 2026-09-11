//! 웹(frontend/src/styles/global.css · ds.css)의 디자인 토큰을 그대로 옮긴 것.
//! 값을 바꿀 일이 생기면 웹 쪽 CSS 변수를 먼저 고치고 여기에 반영한다.

use egui::{Color32, CornerRadius, FontData, FontDefinitions, FontFamily, Stroke, Visuals};
use std::sync::Arc;

// ── GRAY RAMP — 푸른기 도는 회색. 화면 구분은 선이 아니라 인접 두 단계로 만든다.
pub const GRAY_0: Color32 = Color32::from_rgb(0xFF, 0xFF, 0xFF);
pub const GRAY_50: Color32 = Color32::from_rgb(0xF9, 0xFA, 0xFB);
pub const GRAY_100: Color32 = Color32::from_rgb(0xF2, 0xF4, 0xF6);
pub const GRAY_200: Color32 = Color32::from_rgb(0xE5, 0xE8, 0xEB);
pub const GRAY_300: Color32 = Color32::from_rgb(0xD1, 0xD6, 0xDB);
pub const GRAY_400: Color32 = Color32::from_rgb(0xB0, 0xB8, 0xC1);
pub const GRAY_500: Color32 = Color32::from_rgb(0x8B, 0x95, 0xA1);
pub const GRAY_600: Color32 = Color32::from_rgb(0x6B, 0x76, 0x84);
pub const GRAY_700: Color32 = Color32::from_rgb(0x4E, 0x59, 0x68);
pub const GRAY_800: Color32 = Color32::from_rgb(0x33, 0x3D, 0x4B);
pub const GRAY_900: Color32 = Color32::from_rgb(0x19, 0x1F, 0x28);

// ── BLUE — 포인트는 하나뿐이다. 링크 · 선택 · 진행 전부 이 파랑.
pub const BLUE_50: Color32 = Color32::from_rgb(0xEB, 0xF3, 0xFE);
pub const BLUE_100: Color32 = Color32::from_rgb(0xC9, 0xE2, 0xFF);
pub const BLUE_300: Color32 = Color32::from_rgb(0x6B, 0xA6, 0xF8);
pub const BLUE_500: Color32 = Color32::from_rgb(0x31, 0x82, 0xF6);
pub const BLUE_600: Color32 = Color32::from_rgb(0x1B, 0x64, 0xDA);
pub const BLUE_700: Color32 = Color32::from_rgb(0x19, 0x57, 0xB8);

pub const PRIMARY: Color32 = BLUE_500;

// ── SEMANTIC
pub const SUCCESS: Color32 = Color32::from_rgb(0x00, 0xB3, 0x7E);
pub const WARNING: Color32 = Color32::from_rgb(0xF5, 0x9E, 0x0B);
pub const ERROR: Color32 = Color32::from_rgb(0xE5, 0x48, 0x4D);

/// 승은 파랑, 패는 빨강. 초록/빨강 조합은 적록색약에서 구분이 안 된다.
pub const WIN: Color32 = BLUE_600;
pub const LOSS: Color32 = Color32::from_rgb(0xE5, 0x48, 0x4D);
pub const WIN_BG: Color32 = Color32::from_rgb(0xEB, 0xF3, 0xFE);
pub const LOSS_BG: Color32 = Color32::from_rgb(0xFD, 0xEC, 0xEC);

// 팀 색은 승/패와 같은 축을 쓴다 (블루팀 = 파랑, 레드팀 = 빨강).
pub const TEAM_BLUE: Color32 = BLUE_600;
pub const TEAM_RED: Color32 = LOSS;

/// `.t-chip-low` — 표본이 부족하거나 주의가 필요할 때만 쓰는 주황.
pub const WARN_FG: Color32 = Color32::from_rgb(0xB2, 0x5E, 0x00);
pub const WARN_BG: Color32 = Color32::from_rgb(0xFF, 0xF4, 0xE5);

// ── SURFACES
pub const BG_BASE: Color32 = GRAY_50;
pub const BG_SURFACE: Color32 = GRAY_0;
pub const BORDER: Color32 = GRAY_200;

// ── TEXT
pub const TEXT: Color32 = GRAY_900;
pub const TEXT_SECONDARY: Color32 = GRAY_700;
pub const TEXT_MUTED: Color32 = GRAY_500;
pub const TEXT_DISABLED: Color32 = GRAY_400;

// ── LAYOUT
pub const SIDEBAR_W: f32 = 240.0;
pub const TITLEBAR_H: f32 = 40.0;
pub const CONTENT_MAX: f32 = 1200.0;

// ── RADIUS
pub const R_XS: u8 = 4;
pub const R_SM: u8 = 6;
pub const R_MD: u8 = 8;
pub const R_LG: u8 = 12;
pub const R_XL: u8 = 16;

pub fn cr(r: u8) -> CornerRadius {
    CornerRadius::same(r)
}

/// t-tier-* / TIER_COLORS. (배경, 글자색) 쌍.
pub fn tier_colors(tier: &str) -> (Color32, Color32) {
    match tier.to_uppercase().as_str() {
        "S" => (
            Color32::from_rgb(0xFF, 0xE9, 0xE9),
            Color32::from_rgb(0xD4, 0x2B, 0x2B),
        ),
        "A" => (
            Color32::from_rgb(0xFF, 0xF1, 0xE0),
            Color32::from_rgb(0xC2, 0x66, 0x0A),
        ),
        "B" => (BLUE_50, BLUE_600),
        "C" => (GRAY_100, GRAY_600),
        _ => (GRAY_100, GRAY_500),
    }
}

/// 승률 색. 웹의 표 규칙과 같다 — 높으면 파랑, 평범하면 회색.
pub fn win_rate_color(wr: f64) -> Color32 {
    if wr >= 60.0 {
        BLUE_600
    } else if wr >= 50.0 {
        BLUE_500
    } else if wr > 0.0 {
        GRAY_600
    } else {
        GRAY_500
    }
}

/// 내전 Elo 색. 시작점이 1500 이라 경계도 같이 옮겼다 — 1500 이 "아무것도 안 한 사람"의 자리다.
pub fn elo_color(elo: Option<f64>) -> Color32 {
    match elo {
        Some(e) if e >= 1700.0 => BLUE_600,
        Some(e) if e >= 1500.0 => BLUE_500,
        Some(_) => GRAY_600,
        None => GRAY_400,
    }
}

pub mod fonts {
    /// 웹과 같은 Pretendard. 한글 글리프가 있어야 해서 반드시 임베드한다.
    pub const REGULAR: &[u8] = include_bytes!("../assets/Pretendard-Regular.otf");
    pub const SEMIBOLD: &[u8] = include_bytes!("../assets/Pretendard-SemiBold.otf");
    pub const BOLD: &[u8] = include_bytes!("../assets/Pretendard-Bold.otf");
}

pub const F_REGULAR: &str = "pretendard";
pub const F_SEMIBOLD: &str = "pretendard-semibold";
pub const F_BOLD: &str = "pretendard-bold";

pub fn family(name: &'static str) -> FontFamily {
    FontFamily::Name(name.into())
}

fn install_fonts(ctx: &egui::Context) {
    let mut defs = FontDefinitions::default();

    for (name, bytes) in [
        (F_REGULAR, fonts::REGULAR),
        (F_SEMIBOLD, fonts::SEMIBOLD),
        (F_BOLD, fonts::BOLD),
    ] {
        defs.font_data
            .insert(name.to_owned(), Arc::new(FontData::from_static(bytes)));
        // 각 굵기 뒤에 egui 기본 폰트를 남겨 둔다. Pretendard에 없는 기호가
        // 두부(tofu)로 깨지는 대신 기본 폰트로 대체된다.
        defs.families.insert(
            FontFamily::Name(name.into()),
            vec![name.to_owned(), "Ubuntu-Light".to_owned()],
        );
    }

    defs.families
        .entry(FontFamily::Proportional)
        .or_default()
        .insert(0, F_REGULAR.to_owned());
    defs.families
        .entry(FontFamily::Monospace)
        .or_default()
        .insert(0, F_REGULAR.to_owned());

    ctx.set_fonts(defs);
}

pub fn install(ctx: &egui::Context) {
    install_fonts(ctx);

    let mut v = Visuals::light();
    v.panel_fill = BG_BASE;
    v.window_fill = BG_SURFACE;
    v.extreme_bg_color = GRAY_100;
    v.faint_bg_color = GRAY_50;
    v.override_text_color = Some(TEXT);
    v.selection.bg_fill = BLUE_50;
    v.selection.stroke = Stroke::new(1.0, BLUE_600);
    v.hyperlink_color = BLUE_500;

    v.widgets.noninteractive.bg_fill = BG_SURFACE;
    v.widgets.noninteractive.weak_bg_fill = BG_SURFACE;
    v.widgets.noninteractive.bg_stroke = Stroke::new(1.0, BORDER);
    v.widgets.noninteractive.corner_radius = cr(R_MD);

    v.widgets.inactive.bg_fill = GRAY_100;
    v.widgets.inactive.weak_bg_fill = GRAY_100;
    v.widgets.inactive.bg_stroke = Stroke::NONE;
    v.widgets.inactive.corner_radius = cr(R_MD);
    v.widgets.inactive.fg_stroke = Stroke::new(1.0, GRAY_700);

    v.widgets.hovered.bg_fill = GRAY_100;
    v.widgets.hovered.weak_bg_fill = GRAY_100;
    v.widgets.hovered.bg_stroke = Stroke::new(1.0, GRAY_300);
    v.widgets.hovered.corner_radius = cr(R_MD);
    v.widgets.hovered.fg_stroke = Stroke::new(1.0, GRAY_900);

    v.widgets.active.bg_fill = GRAY_200;
    v.widgets.active.weak_bg_fill = GRAY_200;
    v.widgets.active.bg_stroke = Stroke::new(1.0, GRAY_400);
    v.widgets.active.corner_radius = cr(R_MD);
    v.widgets.active.fg_stroke = Stroke::new(1.0, GRAY_900);

    v.widgets.open.bg_fill = GRAY_100;
    v.widgets.open.weak_bg_fill = GRAY_100;

    // 웹은 그림자를 거의 안 쓴다. 카드는 그림자보다 면으로 띄운다.
    v.window_shadow = egui::epaint::Shadow::NONE;
    v.popup_shadow = egui::epaint::Shadow {
        offset: [0, 4],
        blur: 16,
        spread: 0,
        color: Color32::from_black_alpha(20),
    };
    v.window_corner_radius = cr(R_LG);
    v.menu_corner_radius = cr(R_MD);

    ctx.set_theme(egui::ThemePreference::Light);
    ctx.set_visuals(v);

    ctx.all_styles_mut(|s| {
        s.spacing.item_spacing = egui::vec2(8.0, 8.0);
        s.spacing.button_padding = egui::vec2(12.0, 7.0);
        s.spacing.window_margin = egui::Margin::same(12);
        s.spacing.scroll.bar_width = 10.0;
        s.spacing.interact_size.y = 28.0;
        s.visuals.text_cursor.stroke.color = BLUE_500;
    });
}
