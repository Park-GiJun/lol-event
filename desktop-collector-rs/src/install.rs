//! 설치 · 바로가기 · 시작 프로그램.
//!
//! 설치 프로그램(MSI/NSIS)을 쓰지 않는 게 핵심이다. exe 하나가 처음 실행될 때
//! 스스로를 `%LOCALAPPDATA%\LoL-Collector` 로 복사하고 바로가기를 만든다.
//! Program Files 를 건드리지 않으니 관리자 권한이 필요 없고, 그래서 설치할 때도
//! 업데이트할 때도 UAC 프롬프트가 뜨지 않는다.

use std::path::{Path, PathBuf};

pub const APP_NAME: &str = "LoL 수집기";
pub const EXE_NAME: &str = "LoL-Collector.exe";
pub const INSTALL_DIR_NAME: &str = "LoL-Collector";

/// 업데이트가 밀어낸 이전 버전. 다음 실행 때 지운다.
pub const OLD_EXE_NAME: &str = "LoL-Collector.old.exe";

pub fn exe_path() -> PathBuf {
    std::env::current_exe().unwrap_or_else(|_| PathBuf::from(EXE_NAME))
}

pub fn install_dir() -> PathBuf {
    let base = std::env::var("LOCALAPPDATA").unwrap_or_else(|_| ".".to_owned());
    Path::new(&base).join(INSTALL_DIR_NAME)
}

pub fn installed_exe() -> PathBuf {
    install_dir().join(EXE_NAME)
}

/// 설치 위치에서 돌고 있는가.
pub fn running_from_install_dir() -> bool {
    let (Ok(a), Ok(b)) = (exe_path().canonicalize(), installed_exe().canonicalize()) else {
        return false;
    };
    a == b
}

/// 업데이트가 남긴 이전 exe 를 치운다. 실행 중이던 파일이라 바로는 못 지우고,
/// 다음 실행에서야 지워진다. 실패해도 무시한다 — 다음 기회에 또 시도한다.
pub fn cleanup_old_exe() {
    let old = install_dir().join(OLD_EXE_NAME);
    if old.exists() {
        let _ = std::fs::remove_file(old);
    }
}

/// 설치 위치가 아닌 곳에서 실행됐다면 스스로를 복사하고 그쪽을 띄운다.
///
/// `Ok(true)` 면 새 프로세스를 띄웠으니 이 프로세스는 그대로 끝내야 한다.
pub fn ensure_installed() -> std::io::Result<bool> {
    if running_from_install_dir() {
        return Ok(false);
    }

    let dir = install_dir();
    std::fs::create_dir_all(&dir)?;
    let target = installed_exe();

    // 이미 같은 내용이 깔려 있으면 복사를 건너뛴다 (USB 등에서 두 번 실행한 경우).
    let src = exe_path();
    let needs_copy = match (std::fs::metadata(&src), std::fs::metadata(&target)) {
        (Ok(a), Ok(b)) => a.len() != b.len(),
        _ => true,
    };
    if needs_copy {
        // 대상이 실행 중이면 덮어쓸 수 없다. 옆으로 밀어 두고 새로 쓴다.
        if target.exists() {
            let old = dir.join(OLD_EXE_NAME);
            let _ = std::fs::remove_file(&old);
            let _ = std::fs::rename(&target, &old);
        }
        std::fs::copy(&src, &target)?;
    }

    create_shortcuts(&target);

    std::process::Command::new(&target)
        .current_dir(&dir)
        .spawn()?;
    Ok(true)
}

/// 바탕화면 · 시작 메뉴 바로가기.
///
/// COM 을 직접 부르는 대신 PowerShell 의 WScript.Shell 을 쓴다. 의존성을 하나도
/// 늘리지 않으면서 같은 일을 한다. 실패해도 앱 자체는 멀쩡히 돌아가므로 무시한다.
pub fn create_shortcuts(target: &Path) {
    let Some(target) = target.to_str() else { return };
    let script = format!(
        r#"$ws = New-Object -ComObject WScript.Shell
foreach ($dir in @($ws.SpecialFolders('Desktop'), (Join-Path $ws.SpecialFolders('StartMenu') 'Programs'))) {{
  if (-not (Test-Path $dir)) {{ continue }}
  $lnk = $ws.CreateShortcut((Join-Path $dir '{APP_NAME}.lnk'))
  $lnk.TargetPath = '{target}'
  $lnk.WorkingDirectory = Split-Path '{target}'
  $lnk.IconLocation = '{target},0'
  $lnk.Description = 'LoL 내전 수집기'
  $lnk.Save()
}}"#
    );
    run_powershell(&script);
}

fn run_powershell(script: &str) {
    #[cfg(windows)]
    use std::os::windows::process::CommandExt;

    let mut cmd = std::process::Command::new("powershell");
    cmd.args(["-NoProfile", "-NonInteractive", "-Command", script]);
    #[cfg(windows)]
    cmd.creation_flags(CREATE_NO_WINDOW);
    let _ = cmd.status();
}

/// 콘솔 창이 번쩍이지 않게 한다.
#[cfg(windows)]
const CREATE_NO_WINDOW: u32 = 0x0800_0000;

// ── 시작 프로그램 ────────────────────────────────

const RUN_KEY: &str = r"Software\Microsoft\Windows\CurrentVersion\Run";

fn run_key() -> std::io::Result<winreg::RegKey> {
    winreg::RegKey::predef(winreg::enums::HKEY_CURRENT_USER)
        .open_subkey_with_flags(RUN_KEY, winreg::enums::KEY_READ | winreg::enums::KEY_WRITE)
}

pub fn startup_registered() -> bool {
    run_key()
        .and_then(|k| k.get_value::<String, _>(APP_NAME))
        .is_ok()
}

pub fn set_startup(enabled: bool) -> std::io::Result<()> {
    let key = run_key()?;
    if enabled {
        let path = installed_exe();
        key.set_value(APP_NAME, &format!("\"{}\"", path.display()))
    } else {
        match key.delete_value(APP_NAME) {
            Ok(()) => Ok(()),
            // 이미 없으면 성공으로 친다.
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(()),
            Err(e) => Err(e),
        }
    }
}

/// 기본 브라우저로 URL 을 연다.
pub fn open_url(url: &str) {
    #[cfg(windows)]
    use std::os::windows::process::CommandExt;

    // `start` 는 cmd 내장 명령이라 cmd 를 거쳐야 한다. 빈 문자열은 창 제목 자리다.
    let mut cmd = std::process::Command::new("cmd");
    cmd.args(["/C", "start", "", url]);
    #[cfg(windows)]
    cmd.creation_flags(CREATE_NO_WINDOW);
    let _ = cmd.spawn();
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn install_dir_sits_under_local_appdata() {
        let dir = install_dir();
        assert!(dir.ends_with(INSTALL_DIR_NAME));
        // 관리자 권한이 필요한 곳에 깔면 UAC 가 되살아난다.
        let s = dir.to_string_lossy().to_lowercase();
        assert!(!s.contains("program files"), "설치 경로가 Program Files 다: {s}");
    }

    #[test]
    fn installed_exe_is_inside_install_dir() {
        assert_eq!(installed_exe().parent().unwrap(), install_dir());
        assert!(installed_exe().ends_with(EXE_NAME));
    }
}
