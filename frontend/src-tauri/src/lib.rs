pub mod launch;
pub mod lines;
pub mod opening;
mod pulse;
mod relay;

use std::sync::Arc;
use std::time::{Duration, Instant};

use pulse::{Pulse, Verdict, SILENCE_LIMIT};
use relay::{Attached, OnMessage, Relay, RelayMessage};
use tauri::ipc::{CapabilityBuilder, Channel};
use tauri::menu::{MenuBuilder, MenuItemBuilder};
use tauri::{AppHandle, Manager, State, WindowEvent};

#[tauri::command(async)]
fn relay_attach(relay: State<Relay>, on_message: Channel<RelayMessage>) -> Attached {
    relay.attach(to_page(on_message))
}

#[tauri::command(async)]
fn relay_restart(relay: State<Relay>, on_message: Channel<RelayMessage>) -> Attached {
    relay.restart(to_page(on_message))
}

fn to_page(channel: Channel<RelayMessage>) -> OnMessage {
    Arc::new(move |message| {
        let _ = channel.send(message);
    })
}

#[tauri::command(async)]
fn relay_send(relay: State<Relay>, line: String) -> Result<(), String> {
    relay.send(&line)
}

#[tauri::command]
fn page_alive(pulse: State<Pulse>) {
    pulse.beat(Instant::now());
}

const RELOAD: &str = "reload";
const WATCH_EVERY: Duration = Duration::from_secs(1);

/// A packaged build cannot reload by key or menu (ADR 0057), so a page that stops answering is offered Reload in a menu of the window's own.
fn offer_reload_when_silent(app: AppHandle) {
    std::thread::spawn(move || loop {
        std::thread::sleep(WATCH_EVERY);
        let Some(window) = app.get_webview_window("main") else {
            continue;
        };
        match app.state::<Pulse>().check(Instant::now()) {
            Verdict::OfferReload => {
                let menu = MenuItemBuilder::with_id(RELOAD, "Reload")
                    .build(&app)
                    .and_then(|item| MenuBuilder::new(&app).item(&item).build());
                if let Ok(menu) = menu {
                    let _ = window.set_menu(menu);
                }
            }
            Verdict::WithdrawReload => {
                let _ = window.remove_menu();
            }
            Verdict::Nothing => {}
        }
    });
}

pub fn run() {
    let launch_dir = std::env::current_dir().unwrap_or_default();
    let named = std::env::var("LAZYCLAUDE_SIDECAR").ok();
    let project = launch::project(std::env::args().nth(1), launch_dir.clone());
    let openable = opening::scope(&project);
    tauri::Builder::default()
        .plugin(tauri_plugin_clipboard_manager::init())
        .plugin(tauri_plugin_opener::init())
        .manage(Pulse::new(Instant::now(), SILENCE_LIMIT))
        .setup(move |app| {
            app.manage(Relay::new(launch::sidecar(
                named,
                &launch_dir,
                cfg!(debug_assertions),
                app.path().resource_dir().map_err(|error| error.to_string()),
                project,
            )));
            app.add_capability(
                CapabilityBuilder::new("project-paths")
                    .window("main")
                    .permission_scoped(
                        "opener:allow-open-path",
                        vec![serde_json::json!({ "path": openable })],
                        Vec::<serde_json::Value>::new(),
                    ),
            )?;
            if !cfg!(debug_assertions) {
                offer_reload_when_silent(app.handle().clone());
            }
            Ok(())
        })
        .on_menu_event(|app, event| {
            if event.id() == RELOAD {
                if let Some(window) = app.get_webview_window("main") {
                    app.state::<Pulse>().reloaded(Instant::now());
                    let _ = window.remove_menu();
                    let _ = window.reload();
                }
            }
        })
        .invoke_handler(tauri::generate_handler![
            relay_attach,
            relay_restart,
            relay_send,
            page_alive
        ])
        .on_window_event(|window, event| {
            if matches!(
                event,
                WindowEvent::CloseRequested { .. } | WindowEvent::Destroyed
            ) {
                window.state::<Relay>().close();
            }
        })
        .run(tauri::generate_context!())
        .expect("the lazyclaude window failed to start");
}
