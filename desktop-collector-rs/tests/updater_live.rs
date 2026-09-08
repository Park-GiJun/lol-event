//! 실제 GitHub Releases 를 상대로 자동 업데이트 경로를 확인한다.
//!
//! 네트워크를 타고 13MB 를 받으므로 기본 테스트에서는 빠져 있다. 릴리즈 직후
//! 한 번 돌려 "업데이터가 이 릴리즈를 볼 수 있는가"를 확인하는 용도다.
//!
//!   cargo test --test updater_live -- --ignored --nocapture
//!
//! 확인하는 것:
//!   - `desktop-v*` 태그 중 최신을 고르는가
//!   - `LoL-Collector.exe` 자산을 찾는가
//!   - 받은 파일이 실제 exe 인가 (MZ 헤더 · 크기)
//!   - 스테이징 파일이 제자리에 놓이는가

use lol_collector::{install, updater};

#[test]
#[ignore = "네트워크와 실제 릴리즈가 필요하다"]
fn stages_the_published_release() {
    let staged = updater::staged_path();
    // 앞선 실행이 남긴 게 있으면 결과를 오해하게 된다.
    let _ = std::fs::remove_file(&staged);

    let runtime = tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()
        .expect("tokio 런타임");

    // 아주 낮은 버전인 척해서 반드시 업데이트가 걸리게 한다.
    let result = runtime.block_on(updater::check_and_stage("0.0.1"));

    let version = result
        .expect("릴리즈 조회 실패")
        .expect("업데이트를 못 찾았다 — 태그 접두사나 자산 이름이 어긋났을 수 있다");
    println!("스테이징된 버전: v{version}");

    assert!(staged.exists(), "스테이징 파일이 없다: {}", staged.display());

    let bytes = std::fs::read(&staged).expect("스테이징 파일 읽기");
    assert!(
        bytes.starts_with(b"MZ"),
        "받은 파일이 실행 파일이 아니다 (앞 2바이트: {:?})",
        &bytes[..2.min(bytes.len())]
    );
    assert!(
        bytes.len() > 5_000_000,
        "받은 파일이 너무 작다: {} 바이트",
        bytes.len()
    );
    println!(
        "검증 완료: {} ({:.1}MB)",
        staged.display(),
        bytes.len() as f64 / 1_048_576.0
    );

    // 설치 폴더를 어지럽히지 않는다. 실제 적용은 앱이 다음 실행 때 한다.
    std::fs::remove_file(&staged).expect("스테이징 파일 정리");
}

#[test]
fn staged_path_matches_install_dir() {
    assert_eq!(
        updater::staged_path().parent().expect("부모 폴더"),
        install::install_dir()
    );
}
