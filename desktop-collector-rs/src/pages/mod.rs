//! 화면들. 사이드바 항목 하나가 파일 하나에 대응한다.

pub mod champ_select;
pub mod collect;
pub mod custom;
pub mod damage;
pub mod dashboard;
pub mod live;
pub mod matches;
pub mod summoner;
pub mod surrender;
pub mod vision;

/// 백엔드 통계 엔드포인트의 기본 모드. 내전만 집계한다.
pub const MODE: &str = "normal";

/// 큐 ID → 사람이 읽는 이름.
pub fn queue_label(queue_id: i32) -> String {
    match queue_id {
        0 => "커스텀".to_owned(),
        3130 => "5v5 내전".to_owned(),
        3270 => "칼바람".to_owned(),
        other => format!("Q{other}"),
    }
}

/// 포지션 코드 → 한글. LCU 는 소문자, 백엔드는 대문자를 준다.
pub fn position_label(position: &str) -> &'static str {
    match position.to_lowercase().as_str() {
        "top" => "탑",
        "jungle" => "정글",
        "middle" | "mid" => "미드",
        "bottom" | "bot" => "원딜",
        "utility" | "support" => "서포터",
        _ => "—",
    }
}

/// 페이지 오른쪽 위 새로고침 버튼. 눌렸으면 true.
pub fn refresh_button(ui: &mut egui::Ui, loading: bool) -> bool {
    ui.horizontal(|ui| {
        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
            crate::ui::secondary_button(
                ui,
                if loading { "불러오는 중..." } else { "새로고침" },
                !loading,
            )
            .clicked()
        })
        .inner
    })
    .inner
}

/// 로딩/에러/빈 상태를 한 군데서 처리한다. 페이지마다 다시 쓰지 않도록.
pub fn slot_body<T>(
    ui: &mut egui::Ui,
    slot: &crate::net::Slot<T>,
    empty_text: &str,
    is_empty: impl Fn(&T) -> bool,
    add: impl FnOnce(&mut egui::Ui, &T),
) {
    if let Some(err) = &slot.error {
        crate::ui::error(ui, err);
        return;
    }
    match &slot.value {
        None => crate::ui::empty(ui, "불러오는 중..."),
        Some(v) if is_empty(v) => crate::ui::empty(ui, empty_text),
        Some(v) => add(ui, v),
    }
}
