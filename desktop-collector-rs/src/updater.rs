//! 자동 업데이트.
//!
//! 설치 프로그램이 없으므로 업데이트는 "exe 를 바꿔 끼우는 것"이 전부다.
//! msiexec 도, 관리자 권한 승격도, 그래서 보안 경고도 없다.
//!
//! 순서는 이렇다.
//! 1. 앱이 뜨면 백그라운드로 GitHub Releases 를 확인한다.
//! 2. 새 버전이 있으면 조용히 `LoL-Collector.new.exe` 로 받아 둔다 (스테이징).
//! 3. 다음 실행 때 [`apply_staged`] 가 그걸 제자리에 끼우고 재시작한다.
//!    사용자가 배너의 "지금 재시작"을 누르면 그 시점에 바로 한다.
//!
//! 윈도우는 실행 중인 exe 를 지우지는 못해도 이름은 바꿀 수 있다. 그래서
//! 현재 exe 를 `.old` 로 밀어내고 새 파일을 원래 이름에 놓는 방식이 성립한다.

use serde::Deserialize;

use crate::install;
use crate::net::http;

const RELEASES_URL: &str = "https://api.github.com/repos/Park-GiJun/lol-event/releases";

/// 본체 릴리즈 태그 접두사. 런처 릴리즈(`launcher-v`)와 섞이지 않게 한다.
const TAG_PREFIX: &str = "desktop-v";

/// 릴리즈에 올라가는 자산 이름.
pub const ASSET_NAME: &str = "LoL-Collector.exe";

/// 받아만 두고 아직 끼우지 않은 새 버전.
const STAGED_EXE_NAME: &str = "LoL-Collector.new.exe";

#[derive(Debug, Deserialize)]
struct GithubAsset {
    name: String,
    browser_download_url: String,
}

#[derive(Debug, Deserialize)]
struct GithubRelease {
    #[serde(default)]
    tag_name: String,
    #[serde(default)]
    draft: bool,
    #[serde(default)]
    prerelease: bool,
    #[serde(default)]
    assets: Vec<GithubAsset>,
}

/// `1.2.3` 형태를 숫자로 비교한다. `remote` 가 더 높으면 true.
pub fn is_newer(remote: &str, local: &str) -> bool {
    fn parts(v: &str) -> Vec<u64> {
        v.trim()
            .trim_start_matches('v')
            .split('.')
            .map(|p| {
                p.chars()
                    .take_while(char::is_ascii_digit)
                    .collect::<String>()
                    .parse()
                    .unwrap_or(0)
            })
            .collect()
    }
    let (r, l) = (parts(remote), parts(local));
    for i in 0..r.len().max(l.len()) {
        let rv = r.get(i).copied().unwrap_or(0);
        let lv = l.get(i).copied().unwrap_or(0);
        if rv != lv {
            return rv > lv;
        }
    }
    false
}

pub fn staged_path() -> std::path::PathBuf {
    install::install_dir().join(STAGED_EXE_NAME)
}

pub fn has_staged_update() -> bool {
    staged_path().exists()
}

/// 받아 둔 새 버전을 제자리에 끼우고 재시작한다.
///
/// `Ok(true)` 면 새 프로세스를 띄웠으니 이 프로세스는 끝내야 한다.
pub fn apply_staged() -> std::io::Result<bool> {
    let staged = staged_path();
    if !staged.exists() {
        return Ok(false);
    }
    // 설치 위치에서 돌고 있을 때만 바꿔 끼운다. 아니면 어디를 갈아끼울지 알 수 없다.
    if !install::running_from_install_dir() {
        return Ok(false);
    }

    let dir = install::install_dir();
    let current = install::installed_exe();
    let old = dir.join(install::OLD_EXE_NAME);

    let _ = std::fs::remove_file(&old);
    std::fs::rename(&current, &old)?;
    if let Err(e) = std::fs::rename(&staged, &current) {
        // 새 파일을 못 놓았으면 원래 exe 를 되돌려 놓는다. 앱이 사라지는 게 최악이다.
        let _ = std::fs::rename(&old, &current);
        return Err(e);
    }

    std::process::Command::new(&current)
        .current_dir(&dir)
        .spawn()?;
    Ok(true)
}

/// 최신 릴리즈의 (버전, 다운로드 URL).
async fn latest_release() -> Result<Option<(String, String)>, String> {
    let releases: Vec<GithubRelease> = http()
        .get(RELEASES_URL)
        .header("Accept", "application/vnd.github+json")
        .send()
        .await
        .map_err(|e| format!("릴리즈 조회 실패: {e}"))?
        .json()
        .await
        .map_err(|e| format!("릴리즈 해석 실패: {e}"))?;

    let Some(release) = releases
        .into_iter()
        .find(|r| !r.draft && !r.prerelease && r.tag_name.starts_with(TAG_PREFIX))
    else {
        return Ok(None);
    };
    let version = release
        .tag_name
        .trim_start_matches(TAG_PREFIX)
        .trim()
        .to_owned();
    let Some(asset) = release
        .assets
        .into_iter()
        .find(|a| a.name.eq_ignore_ascii_case(ASSET_NAME))
    else {
        return Ok(None);
    };
    Ok(Some((version, asset.browser_download_url)))
}

/// 새 버전이 있으면 받아서 스테이징한다. 받아 둔 버전 문자열을 돌려준다.
pub async fn check_and_stage(current_version: &str) -> Result<Option<String>, String> {
    let Some((version, url)) = latest_release().await? else {
        return Ok(None);
    };
    if !is_newer(&version, current_version) {
        return Ok(None);
    }

    let bytes = http()
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("다운로드 실패: {e}"))?
        .bytes()
        .await
        .map_err(|e| format!("다운로드 실패: {e}"))?;

    // exe 라기엔 너무 작으면 받다 만 것이다. 그런 걸 끼우면 앱이 죽는다.
    if bytes.len() < 1_000_000 {
        return Err(format!("받은 파일이 너무 작습니다 ({}바이트)", bytes.len()));
    }
    if !bytes.starts_with(b"MZ") {
        return Err("받은 파일이 실행 파일이 아닙니다".to_owned());
    }

    let dir = install::install_dir();
    std::fs::create_dir_all(&dir).map_err(|e| format!("설치 폴더 생성 실패: {e}"))?;
    // 부분 파일이 스테이징으로 남지 않도록 임시 이름으로 쓰고 원자적으로 옮긴다.
    let tmp = dir.join("LoL-Collector.download");
    std::fs::write(&tmp, &bytes).map_err(|e| format!("저장 실패: {e}"))?;
    let staged = staged_path();
    let _ = std::fs::remove_file(&staged);
    std::fs::rename(&tmp, &staged).map_err(|e| format!("저장 실패: {e}"))?;

    Ok(Some(version))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn compares_versions_numerically() {
        assert!(is_newer("2.0.10", "2.0.9"));
        assert!(is_newer("2.1.0", "2.0.99"));
        assert!(is_newer("3.0.0", "2.9.9"));
        assert!(!is_newer("2.0.0", "2.0.0"));
        assert!(!is_newer("1.9.9", "2.0.0"));
    }

    #[test]
    fn tolerates_v_prefix_and_missing_parts() {
        assert!(is_newer("v2.1", "2.0.9"));
        assert!(!is_newer("2", "2.0.1"));
        assert!(is_newer("2.0.1", "2"));
    }

    #[test]
    fn ignores_trailing_junk_in_version_parts() {
        assert!(is_newer("2.0.2-beta", "2.0.1"));
        assert!(!is_newer("2.0.1-beta", "2.0.1"));
    }
}
