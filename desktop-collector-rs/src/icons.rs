//! 챔피언 아이콘.
//!
//! Community Dragon 에서 받아 디스크에 캐시하고, 그림으로 만든 뒤 egui 텍스처로
//! 올린다. 같은 아이콘을 표에서 수십 번 그리므로 캐시가 없으면 못 쓴다.

use std::collections::{HashMap, HashSet};
use std::sync::{Mutex, OnceLock};

use egui::{ColorImage, TextureHandle, TextureOptions};

const CDN: &str = "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons";

pub fn icon_url(champion_id: i32) -> String {
    format!("{CDN}/{champion_id}.png")
}

fn cache_dir() -> std::path::PathBuf {
    crate::install::install_dir().join("cache").join("champ-icons")
}

#[derive(Default)]
struct State {
    textures: HashMap<i32, TextureHandle>,
    /// 받는 중이거나, 받아 봤지만 없던 것. 무한 재시도를 막는다.
    attempted: HashSet<i32>,
    /// 백그라운드가 디코딩해 놓은 것 — UI 스레드가 텍스처로 올린다.
    decoded: Vec<(i32, ColorImage)>,
}

fn state() -> &'static Mutex<State> {
    static S: OnceLock<Mutex<State>> = OnceLock::new();
    S.get_or_init(|| Mutex::new(State::default()))
}

fn decode(bytes: &[u8]) -> Option<ColorImage> {
    let img = image::load_from_memory(bytes).ok()?.into_rgba8();
    let size = [img.width() as usize, img.height() as usize];
    Some(ColorImage::from_rgba_unmultiplied(size, img.as_raw()))
}

fn fetch(champion_id: i32, ctx: egui::Context) {
    crate::net::spawn(async move {
        let path = cache_dir().join(format!("{champion_id}.png"));

        let bytes = match std::fs::read(&path) {
            Ok(b) if !b.is_empty() => b,
            _ => {
                let Ok(resp) = crate::net::http().get(icon_url(champion_id)).send().await else {
                    return;
                };
                if !resp.status().is_success() {
                    return;
                }
                let Ok(b) = resp.bytes().await else { return };
                let b = b.to_vec();
                // 캐시 저장은 실패해도 상관없다. 다음에 다시 받으면 그만이다.
                if std::fs::create_dir_all(cache_dir()).is_ok() {
                    let _ = std::fs::write(&path, &b);
                }
                b
            }
        };

        if let Some(image) = decode(&bytes) {
            state()
                .lock()
                .unwrap_or_else(|e| e.into_inner())
                .decoded
                .push((champion_id, image));
            ctx.request_repaint();
        }
    });
}

/// 백그라운드가 디코딩해 둔 이미지를 텍스처로 올린다. 프레임마다 한 번 부른다.
pub fn pump(ctx: &egui::Context) {
    let pending: Vec<(i32, ColorImage)> = {
        let mut s = state().lock().unwrap_or_else(|e| e.into_inner());
        std::mem::take(&mut s.decoded)
    };
    if pending.is_empty() {
        return;
    }
    let mut s = state().lock().unwrap_or_else(|e| e.into_inner());
    for (id, image) in pending {
        let handle = ctx.load_texture(format!("champ-{id}"), image, TextureOptions::LINEAR);
        s.textures.insert(id, handle);
    }
}

fn texture(ctx: &egui::Context, champion_id: i32) -> Option<TextureHandle> {
    let mut s = state().lock().unwrap_or_else(|e| e.into_inner());
    if let Some(t) = s.textures.get(&champion_id) {
        return Some(t.clone());
    }
    if s.attempted.insert(champion_id) {
        drop(s);
        fetch(champion_id, ctx.clone());
    }
    None
}

/// 챔피언 아이콘 한 칸. 아직 못 받았으면 웹의 `.t-champ` 과 같은 회색 자리를 그린다.
pub fn champion_icon(ui: &mut egui::Ui, champion_id: i32, size: f32) -> egui::Response {
    let radius = (size / 4.0).round().clamp(4.0, 12.0) as u8;
    let (rect, response) = ui.allocate_exact_size(egui::vec2(size, size), egui::Sense::hover());

    if !ui.is_rect_visible(rect) {
        return response;
    }

    let placeholder = || {
        ui.painter()
            .rect_filled(rect, crate::theme::cr(radius), crate::theme::GRAY_100);
    };

    if champion_id <= 0 {
        placeholder();
        return response;
    }

    match texture(ui.ctx(), champion_id) {
        Some(tex) => {
            let mut mesh = egui::Mesh::with_texture(tex.id());
            mesh.add_rect_with_uv(
                rect,
                egui::Rect::from_min_max(egui::pos2(0.0, 0.0), egui::pos2(1.0, 1.0)),
                egui::Color32::WHITE,
            );
            ui.painter().with_clip_rect(rect).add(egui::Shape::mesh(mesh));
            // 모서리를 둥글게 보이도록 바깥을 배경색으로 덮는다.
            ui.painter().rect_stroke(
                rect,
                crate::theme::cr(radius),
                egui::Stroke::new(1.0, crate::theme::BORDER),
                egui::StrokeKind::Inside,
            );
        }
        None => placeholder(),
    }

    response
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn icon_url_points_at_community_dragon() {
        assert_eq!(
            icon_url(266),
            format!("{CDN}/266.png"),
        );
    }

    #[test]
    fn cache_dir_is_under_install_dir() {
        assert!(cache_dir().starts_with(crate::install::install_dir()));
    }
}
