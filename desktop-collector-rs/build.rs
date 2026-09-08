fn main() {
    if std::env::var("CARGO_CFG_TARGET_OS").as_deref() == Ok("windows") {
        let mut res = winresource::WindowsResource::new();
        res.set_icon("assets/icon.ico");
        res.set("ProductName", "LoL 수집기");
        res.set("FileDescription", "LoL 내전 수집기");
        res.set("CompanyName", "gijun.net");
        res.set("LegalCopyright", "gijun.net");
        let _ = res.compile();
    }
}
