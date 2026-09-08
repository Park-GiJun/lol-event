//! 백그라운드 실행기와 HTTP 클라이언트.
//!
//! egui 는 즉시 모드라 UI 스레드를 막으면 안 된다. 네트워크는 전부 여기 tokio
//! 런타임에서 돌리고, 결과는 [`Remote`] 슬롯에 넣은 뒤 `ctx.request_repaint()` 로
//! 화면을 깨운다.

use std::future::Future;
use std::sync::{Arc, Mutex, MutexGuard, OnceLock};
use std::time::Duration;

use reqwest::Client;
use tokio::runtime::Runtime;

fn runtime() -> &'static Runtime {
    static RT: OnceLock<Runtime> = OnceLock::new();
    RT.get_or_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .worker_threads(4)
            .enable_all()
            .build()
            .expect("tokio 런타임 생성 실패")
    })
}

/// 백그라운드에서 future 하나를 돌린다.
pub fn spawn<F>(fut: F)
where
    F: Future<Output = ()> + Send + 'static,
{
    runtime().spawn(fut);
}

/// 외부(백엔드 · Data Dragon · GitHub)용 클라이언트.
pub fn http() -> &'static Client {
    static C: OnceLock<Client> = OnceLock::new();
    C.get_or_init(|| {
        Client::builder()
            .user_agent("LoL-Collector")
            .timeout(Duration::from_secs(30))
            .build()
            .expect("HTTP 클라이언트 생성 실패")
    })
}

/// LCU 전용 클라이언트.
///
/// 라이엇 클라이언트는 자기서명 인증서를 쓴다. 상대가 항상 127.0.0.1 이고
/// 포트·비밀번호를 lockfile 에서 직접 읽어 오므로 인증서 검증을 끈다.
pub fn lcu_http() -> &'static Client {
    static C: OnceLock<Client> = OnceLock::new();
    C.get_or_init(|| {
        Client::builder()
            .danger_accept_invalid_certs(true)
            .danger_accept_invalid_hostnames(true)
            .user_agent("LoL-Collector")
            .timeout(Duration::from_secs(10))
            .build()
            .expect("LCU HTTP 클라이언트 생성 실패")
    })
}

/// 비동기로 채워지는 값 하나.
pub struct Slot<T> {
    pub value: Option<T>,
    pub error: Option<String>,
    pub loading: bool,
    /// 한 번이라도 요청했는지. [`Remote::ensure`] 가 중복 요청을 막는 데 쓴다.
    pub requested: bool,
}

impl<T> Default for Slot<T> {
    fn default() -> Self {
        Self {
            value: None,
            error: None,
            loading: false,
            requested: false,
        }
    }
}

impl<T> Slot<T> {
    /// 아직 아무것도 못 받았고 받는 중이면 true — 스켈레톤을 그릴 때.
    pub fn is_first_load(&self) -> bool {
        self.loading && self.value.is_none()
    }
}

pub struct Remote<T> {
    inner: Arc<Mutex<Slot<T>>>,
}

impl<T> Clone for Remote<T> {
    fn clone(&self) -> Self {
        Self {
            inner: Arc::clone(&self.inner),
        }
    }
}

impl<T> Default for Remote<T> {
    fn default() -> Self {
        Self {
            inner: Arc::new(Mutex::new(Slot::default())),
        }
    }
}

impl<T: Send + 'static> Remote<T> {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn lock(&self) -> MutexGuard<'_, Slot<T>> {
        self.inner.lock().unwrap_or_else(|e| e.into_inner())
    }

    /// 아직 한 번도 요청하지 않았다면 시작한다. 매 프레임 불러도 안전하다.
    pub fn ensure<F>(&self, ctx: &egui::Context, fut: F)
    where
        F: Future<Output = Result<T, String>> + Send + 'static,
    {
        if self.lock().requested {
            return;
        }
        self.reload(ctx, fut);
    }

    /// 이미 값이 있어도 다시 받아온다. 새로고침 버튼용.
    pub fn reload<F>(&self, ctx: &egui::Context, fut: F)
    where
        F: Future<Output = Result<T, String>> + Send + 'static,
    {
        {
            let mut slot = self.lock();
            if slot.loading {
                return;
            }
            slot.loading = true;
            slot.requested = true;
            slot.error = None;
        }
        let inner = Arc::clone(&self.inner);
        let ctx = ctx.clone();
        spawn(async move {
            let result = fut.await;
            {
                let mut slot = inner.lock().unwrap_or_else(|e| e.into_inner());
                slot.loading = false;
                match result {
                    Ok(v) => {
                        slot.value = Some(v);
                        slot.error = None;
                    }
                    Err(e) => slot.error = Some(e),
                }
            }
            ctx.request_repaint();
        });
    }

    /// 다음 [`Remote::ensure`] 가 다시 요청하도록 되돌린다.
    pub fn invalidate(&self) {
        self.lock().requested = false;
    }
}

/// 백그라운드에서 계속 갱신되는 값. 폴링 루프가 쓴다.
pub struct Shared<T> {
    inner: Arc<Mutex<T>>,
}

impl<T> Clone for Shared<T> {
    fn clone(&self) -> Self {
        Self {
            inner: Arc::clone(&self.inner),
        }
    }
}

impl<T> Shared<T> {
    pub fn new(v: T) -> Self {
        Self {
            inner: Arc::new(Mutex::new(v)),
        }
    }

    pub fn lock(&self) -> MutexGuard<'_, T> {
        self.inner.lock().unwrap_or_else(|e| e.into_inner())
    }

    pub fn set(&self, v: T) {
        *self.lock() = v;
    }
}

impl<T: Clone> Shared<T> {
    pub fn get(&self) -> T {
        self.lock().clone()
    }
}
