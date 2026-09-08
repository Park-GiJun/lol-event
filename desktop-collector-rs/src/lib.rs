//! LoL 내전 수집기.
//!
//! 데스크탑 본체 · 런처 · MSI 를 하나의 단일 exe 로 합친 것이다.
//! 설치 프로그램이 없으므로 업데이트 때 msiexec 도 UAC 승격도 일어나지 않는다.
//! (자세한 내용은 [`updater`] 참고.)

pub mod api;
pub mod app;
pub mod chart;
pub mod collect;
pub mod icons;
pub mod install;
pub mod lcu;
pub mod models;
pub mod monitor;
pub mod nav_icon;
pub mod net;
pub mod pages;
pub mod theme;
pub mod ui;
pub mod updater;

/// 현재 버전. `release.ps1` 이 Cargo.toml 과 함께 갱신한다.
pub const APP_VERSION: &str = env!("CARGO_PKG_VERSION");
