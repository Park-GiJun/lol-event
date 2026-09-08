#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

//! 진입점.
//!
//! 창을 띄우기 전에 세 가지를 순서대로 처리한다.
//! 1. 받아 둔 새 버전이 있으면 지금 끼우고 재시작한다.
//! 2. 설치 위치(`%LOCALAPPDATA%`)가 아니면 스스로를 복사하고 그쪽을 띄운다.
//! 3. 이미 떠 있는 인스턴스가 있으면 그쪽 창을 깨우고 끝낸다.

use std::io::{Read, Write};
use std::net::{Ipv4Addr, SocketAddrV4, TcpListener, TcpStream};

use lol_collector::{app, install, updater};

/// 두 번 뜨는 걸 막는 데 쓰는 포트. Kotlin 판과 같은 번호를 유지한다.
const SINGLE_INSTANCE_PORT: u16 = 47632;

fn main() -> eframe::Result<()> {
    install::cleanup_old_exe();

    // 지난번에 받아 둔 새 버전을 여기서 끼운다. 설치 프로그램도 UAC 도 없다.
    if matches!(updater::apply_staged(), Ok(true)) {
        return Ok(());
    }

    // 개발 빌드는 설치 위치로 스스로를 옮기지 않는다. target/debug 에서 돌린 걸
    // %LOCALAPPDATA% 에 깔아 버리면 실제 설치본을 덮어쓴다.
    if !cfg!(debug_assertions) && matches!(install::ensure_installed(), Ok(true)) {
        return Ok(());
    }

    let Some(listener) = acquire_single_instance() else {
        wake_running_instance();
        return Ok(());
    };

    let opts = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_title("LoL 수집기")
            .with_inner_size([1120.0, 820.0])
            .with_min_inner_size([880.0, 620.0])
            .with_decorations(false)
            .with_transparent(false)
            .with_icon(window_icon()),
        ..Default::default()
    };

    eframe::run_native(
        "LoL 수집기",
        opts,
        Box::new(move |cc| {
            watch_for_wakeups(listener, cc.egui_ctx.clone());
            Ok(Box::new(app::App::new(&cc.egui_ctx)))
        }),
    )
}

fn acquire_single_instance() -> Option<TcpListener> {
    TcpListener::bind(SocketAddrV4::new(Ipv4Addr::LOCALHOST, SINGLE_INSTANCE_PORT)).ok()
}

/// 이미 떠 있는 인스턴스에게 "창 좀 띄워라"라고 알린다.
/// 트레이에 숨어 있을 때 바로가기를 다시 눌러도 창이 나오게 하려는 것이다.
fn wake_running_instance() {
    if let Ok(mut stream) =
        TcpStream::connect(SocketAddrV4::new(Ipv4Addr::LOCALHOST, SINGLE_INSTANCE_PORT))
    {
        let _ = stream.write_all(b"show");
    }
}

fn watch_for_wakeups(listener: TcpListener, ctx: egui::Context) {
    std::thread::spawn(move || {
        for stream in listener.incoming() {
            let Ok(mut stream) = stream else { continue };
            let mut buf = [0u8; 8];
            let _ = stream.read(&mut buf);
            ctx.send_viewport_cmd(egui::ViewportCommand::Visible(true));
            ctx.send_viewport_cmd(egui::ViewportCommand::Minimized(false));
            ctx.send_viewport_cmd(egui::ViewportCommand::Focus);
            ctx.request_repaint();
        }
    });
}

fn window_icon() -> egui::IconData {
    // 실패하면 빈 아이콘. 창은 떠야 한다.
    match image::load_from_memory(include_bytes!("../assets/icon.png")) {
        Ok(img) => {
            let rgba = img.into_rgba8();
            let (width, height) = rgba.dimensions();
            egui::IconData {
                rgba: rgba.into_raw(),
                width,
                height,
            }
        }
        Err(_) => egui::IconData {
            rgba: vec![0; 4],
            width: 1,
            height: 1,
        },
    }
}
