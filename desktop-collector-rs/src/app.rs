//! 앱 셸 — 타이틀바 · 사이드바 · 라우팅 · 트레이 · 업데이트 배너.
//!
//! 레이아웃은 웹의 `.t-shell` 을 따른다. 사이드바 240px 흰 면, 본문은 회색 바탕
//! 위에 흰 카드. 다른 점은 창 테두리를 직접 그린다는 것 하나뿐이다.

use std::time::Duration;

use crate::install;
use crate::monitor::Signals;
use crate::nav_icon::{self, Icon};
use crate::pages;
use crate::theme::*;
use crate::ui;
use crate::updater;

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum Page {
    Dashboard,
    Matches,
    Summoner,
    ChampSelect,
    Live,
    Custom,
    Damage,
    Vision,
    Surrender,
    Collect,
}

impl Page {
    fn label(self) -> &'static str {
        match self {
            Page::Dashboard => "대시보드",
            Page::Matches => "경기 기록",
            Page::Summoner => "소환사 검색",
            Page::ChampSelect => "챔피언 선택",
            Page::Live => "실시간 분석",
            Page::Custom => "내전 분석",
            Page::Damage => "데미지 분석",
            Page::Vision => "시야 분석",
            Page::Surrender => "서렌더 분석",
            Page::Collect => "매치 수집",
        }
    }

    fn icon(self) -> Icon {
        match self {
            Page::Dashboard => Icon::Chart,
            Page::Matches => Icon::List,
            Page::Summoner => Icon::User,
            Page::ChampSelect => Icon::Swords,
            Page::Live => Icon::Radio,
            Page::Custom => Icon::Users,
            Page::Damage => Icon::Flame,
            Page::Vision => Icon::Eye,
            Page::Surrender => Icon::Flag,
            Page::Collect => Icon::Download,
        }
    }
}

/// 사이드바 묶음. 웹의 `NAV_GROUPS` 와 같은 순서다.
const NAV: [(Option<&str>, &[Page]); 4] = [
    (None, &[Page::Dashboard, Page::Matches, Page::Summoner]),
    (
        Some("실시간"),
        &[Page::ChampSelect, Page::Live, Page::Custom],
    ),
    (
        Some("분석"),
        &[Page::Damage, Page::Vision, Page::Surrender],
    ),
    (Some("관리"), &[Page::Collect]),
];

pub struct App {
    page: Page,
    signals: Signals,

    dashboard: pages::dashboard::DashboardPage,
    matches: pages::matches::MatchesPage,
    summoner: pages::summoner::SummonerPage,
    champ_select: pages::champ_select::ChampSelectPage,
    live: pages::live::LivePage,
    custom: pages::custom::CustomPage,
    damage: pages::damage::DamagePage,
    vision: pages::vision::VisionPage,
    surrender: pages::surrender::SurrenderPage,
    collect: pages::collect::CollectPage,

    startup_on: bool,
    update_ready: crate::net::Shared<Option<String>>,
    tray: Option<tray_icon::TrayIcon>,
    tray_ids: TrayIds,
}

#[derive(Default)]
struct TrayIds {
    open: Option<tray_icon::menu::MenuId>,
    website: Option<tray_icon::menu::MenuId>,
    startup: Option<tray_icon::menu::MenuId>,
    quit: Option<tray_icon::menu::MenuId>,
}

impl App {
    pub fn new(ctx: &egui::Context) -> Self {
        theme_install(ctx);

        let signals = Signals::default();
        crate::monitor::start(signals.clone(), ctx.clone());

        let update_ready = crate::net::Shared::new(None);
        spawn_update_check(ctx.clone(), update_ready.clone());

        let (tray, tray_ids) = build_tray();

        Self {
            page: Page::Dashboard,
            signals,
            dashboard: Default::default(),
            matches: Default::default(),
            summoner: Default::default(),
            champ_select: Default::default(),
            live: Default::default(),
            custom: Default::default(),
            damage: Default::default(),
            vision: Default::default(),
            surrender: Default::default(),
            collect: Default::default(),
            startup_on: install::startup_registered(),
            update_ready,
            tray,
            tray_ids,
        }
    }

    fn handle_tray(&mut self, ctx: &egui::Context) {
        if self.tray.is_none() {
            return;
        }
        // 트레이 아이콘 클릭 — 창을 되살린다.
        while tray_icon::TrayIconEvent::receiver().try_recv().is_ok() {
            show_window(ctx);
        }
        while let Ok(event) = tray_icon::menu::MenuEvent::receiver().try_recv() {
            let id = Some(event.id.clone());
            if id == self.tray_ids.open {
                show_window(ctx);
            } else if id == self.tray_ids.website {
                install::open_url("https://lol.gijun.net");
            } else if id == self.tray_ids.startup {
                let next = !self.startup_on;
                if install::set_startup(next).is_ok() {
                    self.startup_on = next;
                }
            } else if id == self.tray_ids.quit {
                ctx.send_viewport_cmd(egui::ViewportCommand::Close);
            }
        }
    }

    /// 닫기 버튼은 종료가 아니라 트레이로 보낸다. 게임 종료 후 자동 수집이
    /// 계속 돌아야 하기 때문이다. 진짜 종료는 트레이 메뉴에서 한다.
    fn hide_to_tray_on_close(&self, ctx: &egui::Context) {
        if self.tray.is_none() {
            return;
        }
        if ctx.input(|i| i.viewport().close_requested()) {
            ctx.send_viewport_cmd(egui::ViewportCommand::CancelClose);
            ctx.send_viewport_cmd(egui::ViewportCommand::Visible(false));
        }
    }

    fn titlebar(&mut self, ui: &mut egui::Ui) {
        let ctx = ui.ctx().clone();
        egui::Panel::top("titlebar")
            .exact_size(TITLEBAR_H)
            .show_separator_line(false)
            .frame(
                egui::Frame::new()
                    .fill(GRAY_0)
                    .inner_margin(egui::Margin::symmetric(12, 0)),
            )
            .show(ui, |ui| {
                let bar = ui.horizontal_centered(|ui| {
                    ui.label(ui::txt("LoL 수집기", 14.0, ui::W::Bold, GRAY_900));
                    ui.label(ui::txt(
                        format!("v{}", crate::APP_VERSION),
                        11.0,
                        ui::W::Regular,
                        GRAY_400,
                    ));
                    ui.add_space(10.0);

                    let status = self.signals.lcu_status.lock();
                    let (dot, text, color) = if status.connected {
                        (BLUE_500, status.display_name(), GRAY_700)
                    } else {
                        (
                            GRAY_300,
                            status
                                .reason
                                .clone()
                                .unwrap_or_else(|| "미연결".to_owned()),
                            GRAY_500,
                        )
                    };
                    drop(status);
                    let (rect, _) =
                        ui.allocate_exact_size(egui::vec2(8.0, 8.0), egui::Sense::hover());
                    ui.painter().circle_filled(rect.center(), 4.0, dot);
                    ui.label(ui::txt(text, 12.0, ui::W::Semibold, color));

                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        if window_button(ui, WindowButton::Close).clicked() {
                            ctx.send_viewport_cmd(egui::ViewportCommand::Close);
                        }
                        if window_button(ui, WindowButton::Maximize).clicked() {
                            let maximized = ctx.input(|i| i.viewport().maximized.unwrap_or(false));
                            ctx.send_viewport_cmd(egui::ViewportCommand::Maximized(!maximized));
                        }
                        if window_button(ui, WindowButton::Minimize).clicked() {
                            ctx.send_viewport_cmd(egui::ViewportCommand::Minimized(true));
                        }
                    });
                });

                // 빈 곳을 끌면 창이 움직인다.
                let drag = ui.interact(
                    bar.response.rect,
                    ui.id().with("titlebar-drag"),
                    egui::Sense::click_and_drag(),
                );
                if drag.drag_started() {
                    ctx.send_viewport_cmd(egui::ViewportCommand::StartDrag);
                }
                if drag.double_clicked() {
                    let maximized = ctx.input(|i| i.viewport().maximized.unwrap_or(false));
                    ctx.send_viewport_cmd(egui::ViewportCommand::Maximized(!maximized));
                }

                ui.painter().hline(
                    ui.max_rect().x_range(),
                    ui.max_rect().bottom() - 0.5,
                    egui::Stroke::new(1.0, GRAY_200),
                );
            });
    }

    fn sidebar(&mut self, ui: &mut egui::Ui) {
        egui::Panel::left("nav")
            .exact_size(SIDEBAR_W)
            .resizable(false)
            .show_separator_line(false)
            .frame(
                egui::Frame::new()
                    .fill(GRAY_0)
                    .inner_margin(egui::Margin::symmetric(12, 16)),
            )
            .show(ui, |ui| {
                // 브랜드 — 웹의 `.t-brand` 와 같은 파란 마크 + 이름.
                ui.horizontal(|ui| {
                    let (rect, _) =
                        ui.allocate_exact_size(egui::vec2(26.0, 26.0), egui::Sense::hover());
                    ui.painter().rect_filled(rect, cr(R_MD), BLUE_500);
                    nav_icon::draw(
                        ui.painter(),
                        rect.shrink(6.0),
                        Icon::Swords,
                        egui::Color32::WHITE,
                    );
                    ui.label(ui::txt("LoL 내전", 17.0, ui::W::Bold, GRAY_900));
                });
                ui.add_space(14.0);

                for (group, items) in NAV {
                    if let Some(label) = group {
                        ui.add_space(10.0);
                        ui.label(ui::txt(label, 12.0, ui::W::Semibold, GRAY_500));
                        ui.add_space(4.0);
                    }
                    for page in items {
                        if nav_item(ui, *page, self.page == *page) {
                            self.page = *page;
                        }
                    }
                }

                ui.with_layout(egui::Layout::bottom_up(egui::Align::Min), |ui| {
                    ui.add_space(4.0);
                    let mut on = self.startup_on;
                    if ui
                        .add(egui::Checkbox::new(
                            &mut on,
                            ui::txt("시작 프로그램 등록", 12.0, ui::W::Regular, GRAY_600),
                        ))
                        .changed()
                        && install::set_startup(on).is_ok()
                    {
                        self.startup_on = on;
                    }
                });
            });
    }

    fn content(&mut self, ui: &mut egui::Ui) {
        egui::CentralPanel::default()
            .frame(egui::Frame::new().fill(BG_BASE))
            .show(ui, |ui| {
                if let Some(version) = self.update_ready.get() {
                    ui.add_space(8.0);
                    ui.horizontal(|ui| {
                        ui.add_space(12.0);
                        ui.allocate_ui(egui::vec2(ui.available_width() - 24.0, 0.0), |ui| {
                            ui::banner(
                                ui,
                                &format!("새 버전 v{version} 준비됨 — 다시 켜면 적용됩니다"),
                                BLUE_600,
                                BLUE_50,
                                |ui| {
                                    if ui::secondary_button(ui, "지금 재시작", true).clicked() {
                                        restart_now(ui.ctx());
                                    }
                                },
                            );
                        });
                    });
                }

                match self.page {
                    Page::Dashboard => self.dashboard.ui(ui),
                    Page::Matches => self.matches.ui(ui),
                    Page::Summoner => self.summoner.ui(ui),
                    Page::ChampSelect => self.champ_select.ui(ui, &self.signals),
                    Page::Live => self.live.ui(ui, &self.signals),
                    Page::Custom => self.custom.ui(ui, &self.signals),
                    Page::Damage => self.damage.ui(ui),
                    Page::Vision => self.vision.ui(ui),
                    Page::Surrender => self.surrender.ui(ui),
                    Page::Collect => self.collect.ui(ui, &self.signals),
                }
            });
    }
}

impl eframe::App for App {
    // egui 메모리(스크롤 위치 등)를 디스크에 남기지 않는다.
    // 다음에 켤 때 화면 중간에서 시작하는 것보다 맨 위에서 시작하는 편이 낫다.
    fn persist_egui_memory(&self) -> bool {
        false
    }

    fn clear_color(&self, _visuals: &egui::Visuals) -> [f32; 4] {
        BG_BASE.to_normalized_gamma_f32()
    }

    fn ui(&mut self, ui: &mut egui::Ui, _frame: &mut eframe::Frame) {
        let ctx = ui.ctx().clone();
        crate::icons::pump(&ctx);
        self.handle_tray(&ctx);
        self.hide_to_tray_on_close(&ctx);

        // 게임이 막 시작됐으면 실시간 탭으로 넘긴다.
        if *self.signals.jump_to_live.lock() {
            self.signals.jump_to_live.set(false);
            self.page = Page::Live;
            show_window(&ctx);
        }

        self.titlebar(ui);
        self.sidebar(ui);
        self.content(ui);
    }
}

fn theme_install(ctx: &egui::Context) {
    crate::theme::install(ctx);
}

fn nav_item(ui: &mut egui::Ui, page: Page, selected: bool) -> bool {
    let height = 38.0;
    let (rect, response) = ui.allocate_exact_size(
        egui::vec2(ui.available_width(), height),
        egui::Sense::click(),
    );

    let (bg, fg) = if selected {
        (BLUE_50, BLUE_600)
    } else if response.hovered() {
        (GRAY_50, GRAY_900)
    } else {
        (egui::Color32::TRANSPARENT, GRAY_700)
    };
    ui.painter().rect_filled(rect, cr(R_MD), bg);

    let icon_rect = egui::Rect::from_center_size(
        egui::pos2(rect.left() + 22.0, rect.center().y),
        egui::vec2(17.0, 17.0),
    );
    nav_icon::draw(ui.painter(), icon_rect, page.icon(), fg);

    ui.painter().text(
        egui::pos2(rect.left() + 40.0, rect.center().y),
        egui::Align2::LEFT_CENTER,
        page.label(),
        egui::FontId::new(
            14.0,
            crate::theme::family(if selected { F_SEMIBOLD } else { F_REGULAR }),
        ),
        fg,
    );

    response.clicked()
}

enum WindowButton {
    Minimize,
    Maximize,
    Close,
}

fn window_button(ui: &mut egui::Ui, kind: WindowButton) -> egui::Response {
    let (rect, response) =
        ui.allocate_exact_size(egui::vec2(40.0, TITLEBAR_H - 8.0), egui::Sense::click());
    let hovered = response.hovered();
    let (bg, fg) = match (&kind, hovered) {
        (WindowButton::Close, true) => (LOSS, egui::Color32::WHITE),
        (_, true) => (GRAY_100, GRAY_900),
        (_, false) => (egui::Color32::TRANSPARENT, GRAY_600),
    };
    ui.painter().rect_filled(rect, cr(R_SM), bg);

    let c = rect.center();
    let stroke = egui::Stroke::new(1.4, fg);
    match kind {
        WindowButton::Minimize => {
            ui.painter()
                .hline((c.x - 5.0)..=(c.x + 5.0), c.y + 4.0, stroke);
        }
        WindowButton::Maximize => {
            ui.painter().rect_stroke(
                egui::Rect::from_center_size(c, egui::vec2(10.0, 10.0)),
                cr(2),
                stroke,
                egui::StrokeKind::Middle,
            );
        }
        WindowButton::Close => {
            ui.painter()
                .line_segment([c + egui::vec2(-5.0, -5.0), c + egui::vec2(5.0, 5.0)], stroke);
            ui.painter()
                .line_segment([c + egui::vec2(5.0, -5.0), c + egui::vec2(-5.0, 5.0)], stroke);
        }
    }
    response
}

fn show_window(ctx: &egui::Context) {
    ctx.send_viewport_cmd(egui::ViewportCommand::Visible(true));
    ctx.send_viewport_cmd(egui::ViewportCommand::Minimized(false));
    ctx.send_viewport_cmd(egui::ViewportCommand::Focus);
}

/// 받아 둔 새 버전을 지금 끼우고 재시작한다.
fn restart_now(ctx: &egui::Context) {
    match updater::apply_staged() {
        Ok(true) => ctx.send_viewport_cmd(egui::ViewportCommand::Close),
        // 실패해도 다음 실행 때 다시 시도한다. 여기서 앱을 죽이지는 않는다.
        Ok(false) | Err(_) => {}
    }
}

fn spawn_update_check(ctx: egui::Context, ready: crate::net::Shared<Option<String>>) {
    crate::net::spawn(async move {
        // 창이 뜨자마자 네트워크를 때리면 첫 화면이 느려진다. 조금 미룬다.
        tokio::time::sleep(Duration::from_secs(5)).await;
        loop {
            if let Ok(Some(version)) = updater::check_and_stage(crate::APP_VERSION).await {
                ready.set(Some(version));
                ctx.request_repaint();
            }
            // 하루에 한 번이면 충분하다. 오래 켜 두는 앱이라 한 번은 돌게 된다.
            tokio::time::sleep(Duration::from_secs(6 * 60 * 60)).await;
        }
    });
}

fn build_tray() -> (Option<tray_icon::TrayIcon>, TrayIds) {
    use tray_icon::menu::{Menu, MenuItem, PredefinedMenuItem};

    let Ok(icon) = tray_image() else {
        return (None, TrayIds::default());
    };

    let menu = Menu::new();
    let open = MenuItem::new("창 열기", true, None);
    let website = MenuItem::new("웹사이트 열기", true, None);
    let startup = MenuItem::new("시작 프로그램 등록/해제", true, None);
    let quit = MenuItem::new("종료", true, None);
    let ids = TrayIds {
        open: Some(open.id().clone()),
        website: Some(website.id().clone()),
        startup: Some(startup.id().clone()),
        quit: Some(quit.id().clone()),
    };
    let separator = PredefinedMenuItem::separator();
    if menu
        .append_items(&[&open, &website, &startup, &separator, &quit])
        .is_err()
    {
        return (None, TrayIds::default());
    }

    let tray = tray_icon::TrayIconBuilder::new()
        .with_menu(Box::new(menu))
        .with_tooltip(format!("LoL 수집기 v{}", crate::APP_VERSION))
        .with_icon(icon)
        .build()
        .ok();

    if tray.is_none() {
        return (None, TrayIds::default());
    }
    (tray, ids)
}

fn tray_image() -> Result<tray_icon::Icon, String> {
    let img = image::load_from_memory(include_bytes!("../assets/icon.png"))
        .map_err(|e| e.to_string())?
        .into_rgba8();
    let (w, h) = img.dimensions();
    tray_icon::Icon::from_rgba(img.into_raw(), w, h).map_err(|e| e.to_string())
}
