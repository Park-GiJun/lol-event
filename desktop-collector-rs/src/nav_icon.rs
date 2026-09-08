//! 사이드바 아이콘.
//!
//! 웹은 lucide 아이콘을 쓰는데, SVG 를 통째로 들고 오는 대신 같은 인상의 선
//! 그림을 직접 그린다. 전부 24×24 좌표계에서 그린 뒤 실제 크기로 스케일한다.

use egui::{Color32, Painter, Pos2, Rect, Stroke, Vec2};

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum Icon {
    Chart,
    List,
    User,
    Swords,
    Radio,
    Users,
    Flame,
    Eye,
    Flag,
    Download,
}

/// 24×24 기준 좌표를 실제 위치로.
fn mapper(rect: Rect) -> impl Fn(f32, f32) -> Pos2 {
    let scale = rect.width().min(rect.height()) / 24.0;
    let origin = rect.center() - Vec2::splat(12.0 * scale);
    move |x, y| origin + Vec2::new(x * scale, y * scale)
}

fn arc(center: Pos2, radius: f32, from_deg: f32, to_deg: f32, steps: usize) -> Vec<Pos2> {
    (0..=steps)
        .map(|i| {
            let t = from_deg + (to_deg - from_deg) * (i as f32 / steps as f32);
            let rad = t.to_radians();
            center + Vec2::new(rad.cos() * radius, rad.sin() * radius)
        })
        .collect()
}

pub fn draw(painter: &Painter, rect: Rect, icon: Icon, color: Color32) {
    let p = mapper(rect);
    let w = rect.width().min(rect.height()) / 24.0 * 2.0;
    let s = Stroke::new(w.max(1.2), color);

    match icon {
        Icon::Chart => {
            for (x, top) in [(5.0, 14.0), (12.0, 8.0), (19.0, 11.0)] {
                painter.line_segment([p(x, top), p(x, 20.0)], s);
            }
        }
        Icon::List => {
            for y in [7.0, 12.0, 17.0] {
                painter.line_segment([p(4.0, y), p(20.0, y)], s);
            }
        }
        Icon::User => {
            painter.circle_stroke(p(12.0, 9.0), rect.width() / 24.0 * 3.6, s);
            painter.add(egui::Shape::line(
                arc(p(12.0, 20.0), rect.width() / 24.0 * 7.0, 180.0, 360.0, 16),
                s,
            ));
        }
        Icon::Swords => {
            // 날 두 개를 X 로 겹치고, 아래쪽에 짧은 가드를 달아 칼로 읽히게 한다.
            // 가드가 없으면 그냥 닫기(X) 버튼처럼 보인다.
            painter.line_segment([p(4.5, 4.0), p(16.0, 17.0)], s);
            painter.line_segment([p(19.5, 4.0), p(8.0, 17.0)], s);
            painter.line_segment([p(13.0, 17.5), p(19.0, 13.5)], s);
            painter.line_segment([p(11.0, 17.5), p(5.0, 13.5)], s);
            painter.line_segment([p(15.0, 19.5), p(19.0, 16.5)], s);
            painter.line_segment([p(9.0, 19.5), p(5.0, 16.5)], s);
        }
        Icon::Radio => {
            painter.circle_filled(p(12.0, 12.0), rect.width() / 24.0 * 2.4, color);
            for r in [6.0, 9.5] {
                painter.add(egui::Shape::line(
                    arc(p(12.0, 12.0), rect.width() / 24.0 * r, -60.0, 60.0, 12),
                    s,
                ));
                painter.add(egui::Shape::line(
                    arc(p(12.0, 12.0), rect.width() / 24.0 * r, 120.0, 240.0, 12),
                    s,
                ));
            }
        }
        Icon::Users => {
            painter.circle_stroke(p(9.0, 9.0), rect.width() / 24.0 * 3.2, s);
            painter.circle_stroke(p(17.0, 10.0), rect.width() / 24.0 * 2.4, s);
            painter.add(egui::Shape::line(
                arc(p(9.0, 19.0), rect.width() / 24.0 * 6.0, 180.0, 360.0, 14),
                s,
            ));
        }
        Icon::Flame => {
            painter.add(egui::Shape::line(
                vec![
                    p(12.0, 3.0),
                    p(17.0, 10.0),
                    p(18.0, 15.0),
                    p(12.0, 21.0),
                    p(6.0, 15.0),
                    p(8.0, 9.0),
                    p(11.0, 12.0),
                    p(12.0, 3.0),
                ],
                s,
            ));
        }
        Icon::Eye => {
            painter.add(egui::Shape::line(
                vec![p(3.0, 12.0), p(8.0, 6.5), p(16.0, 6.5), p(21.0, 12.0)],
                s,
            ));
            painter.add(egui::Shape::line(
                vec![p(3.0, 12.0), p(8.0, 17.5), p(16.0, 17.5), p(21.0, 12.0)],
                s,
            ));
            painter.circle_filled(p(12.0, 12.0), rect.width() / 24.0 * 2.4, color);
        }
        Icon::Flag => {
            painter.line_segment([p(6.0, 3.0), p(6.0, 21.0)], s);
            painter.add(egui::Shape::line(
                vec![p(6.0, 5.0), p(19.0, 8.5), p(6.0, 13.0)],
                s,
            ));
        }
        Icon::Download => {
            painter.line_segment([p(12.0, 4.0), p(12.0, 15.0)], s);
            painter.add(egui::Shape::line(
                vec![p(7.5, 10.5), p(12.0, 15.0), p(16.5, 10.5)],
                s,
            ));
            painter.line_segment([p(5.0, 19.5), p(19.0, 19.5)], s);
        }
    }
}
