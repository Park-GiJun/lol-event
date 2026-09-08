//! Pretendard 를 egui 가 실제로 읽어 한글 글리프를 내주는지 확인한다.
//! 폰트 파일이나 등록 방식을 바꾸면 여기서 먼저 깨진다.
//!
//! `has_glyph` 는 "이 문자가 대체 글리프 face 로 떨어지는가"로 판정한다. 그래서
//! 검사하려는 폰트를 목록 맨 앞에 두면 대체 face 자체가 그 폰트가 되어 항상
//! false 가 나온다. Ubuntu-Light(egui 기본 폰트)를 앞에 세우고, Ubuntu 에 없는
//! 한글만 물어보면 "Pretendard 가 이 글자를 맡았는가"를 정확히 판정할 수 있다.

use std::sync::Arc;

/// Ubuntu-Light 에 없는 글자들 — 반드시 Pretendard 가 맡아야 한다.
const KOREAN_SAMPLES: [&str; 4] = [
    "내전대시보드",
    "명예의전당챔피언분석",
    "승률판수",
    "닷지감지총회",
];

fn probe_ctx() -> egui::Context {
    let ctx = egui::Context::default();
    let mut defs = egui::FontDefinitions::default();
    for (name, bytes) in [
        ("regular", lol_collector::theme::fonts::REGULAR),
        ("semibold", lol_collector::theme::fonts::SEMIBOLD),
        ("bold", lol_collector::theme::fonts::BOLD),
    ] {
        defs.font_data
            .insert(name.to_owned(), Arc::new(egui::FontData::from_static(bytes)));
        defs.families.insert(
            egui::FontFamily::Name(name.into()),
            vec!["Ubuntu-Light".to_owned(), name.to_owned()],
        );
    }
    ctx.set_fonts(defs);
    // 렌더러가 없으니 텍스처 델타는 버려야 한다 (안 버리면 Drop 에서 패닉).
    ctx.run_ui(Default::default(), |_| {}).textures_delta.clear();
    ctx
}

#[test]
fn every_weight_has_korean_glyphs() {
    let ctx = probe_ctx();
    for weight in ["regular", "semibold", "bold"] {
        let font_id = egui::FontId::new(14.0, egui::FontFamily::Name(weight.into()));
        for s in KOREAN_SAMPLES {
            let ok = ctx.fonts_mut(|f| f.has_glyphs(&font_id, s));
            assert!(ok, "Pretendard {weight} 에 '{s}' 글리프가 없다");
            let w = ctx.fonts_mut(|f| f.glyph_width(&font_id, s.chars().next().unwrap()));
            assert!(w > 0.0, "Pretendard {weight} 의 한글 폭이 0이다");
        }
    }
}

#[test]
fn theme_binds_every_weight() {
    let ctx = egui::Context::default();
    lol_collector::theme::install(&ctx);
    ctx.run_ui(Default::default(), |_| {}).textures_delta.clear();

    ctx.fonts_mut(|f| {
        let families = &f.definitions().families;
        for name in [
            lol_collector::theme::F_REGULAR,
            lol_collector::theme::F_SEMIBOLD,
            lol_collector::theme::F_BOLD,
        ] {
            let bound = families
                .get(&egui::FontFamily::Name(name.into()))
                .unwrap_or_else(|| panic!("{name} 패밀리가 등록되지 않았다"));
            assert_eq!(bound.first().map(String::as_str), Some(name));
        }
        // 기본 비례 폰트도 Pretendard 가 먼저여야 한다. 아니면 라벨이 Ubuntu 로 나온다.
        let prop = &families[&egui::FontFamily::Proportional];
        assert_eq!(
            prop.first().map(String::as_str),
            Some(lol_collector::theme::F_REGULAR)
        );
    });
}
