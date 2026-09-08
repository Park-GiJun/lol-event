//! 부호 있는 시계열 차트.
//!
//! 골드 차이처럼 0을 기준으로 위아래가 뒤집히는 값을 그린다. 양수 구간은 블루팀
//! 색, 음수 구간은 레드팀 색으로 채우고 y=0 기준선을 항상 보여 준다.

use egui::{Color32, Pos2, Rect, Sense, Shape, Stroke, Ui};

use crate::theme::*;

/// `points` 는 (시간, 값). 시간 순으로 정렬돼 있다고 가정한다.
pub fn signed_line(ui: &mut Ui, points: &[(f32, f32)], height: f32) {
    let width = ui.available_width();
    let (rect, _) = ui.allocate_exact_size(egui::vec2(width, height), Sense::hover());
    if !ui.is_rect_visible(rect) || points.len() < 2 {
        return;
    }

    let pad = egui::vec2(4.0, 6.0);
    let plot = Rect::from_min_max(
        rect.min + pad,
        egui::pos2(rect.max.x - pad.x, rect.max.y - pad.y),
    );
    let center_y = plot.center().y;

    let x_min = points.first().expect("len >= 2").0;
    let x_max = points.last().expect("len >= 2").0;
    let x_range = (x_max - x_min).max(1.0);
    let abs_max = points
        .iter()
        .map(|(_, v)| v.abs())
        .fold(1.0f32, f32::max);

    let map = |(t, v): (f32, f32)| -> Pos2 {
        egui::pos2(
            plot.min.x + (t - x_min) / x_range * plot.width(),
            center_y - (v / abs_max) * (plot.height() / 2.0),
        )
    };

    let painter = ui.painter().with_clip_rect(rect);

    // 0 기준선. 여기 위/아래가 뒤집히는 지점이 이 그래프의 전부다.
    painter.hline(
        plot.x_range(),
        center_y,
        Stroke::new(1.0, GRAY_200),
    );

    // 면 채우기 — 부호가 바뀌는 지점에서 구간을 끊는다.
    let mut run: Vec<Pos2> = Vec::new();
    let mut run_positive = points[0].1 >= 0.0;
    let flush = |run: &mut Vec<Pos2>, positive: bool| -> Option<Shape> {
        if run.len() < 2 {
            run.clear();
            return None;
        }
        let mut poly = Vec::with_capacity(run.len() + 2);
        poly.push(egui::pos2(run[0].x, center_y));
        poly.extend(run.iter().copied());
        poly.push(egui::pos2(run[run.len() - 1].x, center_y));
        run.clear();
        let color = if positive { TEAM_BLUE } else { TEAM_RED };
        Some(Shape::convex_polygon(
            poly,
            color.gamma_multiply(0.16),
            Stroke::NONE,
        ))
    };

    for &(t, v) in points {
        let positive = v >= 0.0;
        if positive != run_positive {
            // 부호가 바뀌면 기준선 위의 교차점에서 끊는다.
            if let Some(last) = run.last().copied() {
                run.push(egui::pos2(last.x, center_y));
            }
            if let Some(shape) = flush(&mut run, run_positive) {
                painter.add(shape);
            }
            run_positive = positive;
            run.push(egui::pos2(map((t, v)).x, center_y));
        }
        run.push(map((t, v)));
    }
    if let Some(shape) = flush(&mut run, run_positive) {
        painter.add(shape);
    }

    // 선 — 구간별로 색을 바꾼다.
    for pair in points.windows(2) {
        let (a, b) = (pair[0], pair[1]);
        let color = if (a.1 + b.1) / 2.0 >= 0.0 {
            TEAM_BLUE
        } else {
            TEAM_RED
        };
        painter.line_segment([map(a), map(b)], Stroke::new(2.0, color));
    }

    // 마지막 점 강조. "지금 어디인지"가 이 차트에서 가장 자주 보는 값이다.
    let last = *points.last().expect("len >= 2");
    let color = if last.1 >= 0.0 { TEAM_BLUE } else { TEAM_RED };
    painter.circle_filled(map(last), 3.5, color);
    painter.circle_stroke(map(last), 3.5, Stroke::new(1.5, Color32::WHITE));
}
