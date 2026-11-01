pub mod launch;
pub mod lines;
mod relay;

use relay::{Attached, Relay, RelayMessage};
use tauri::ipc::Channel;
use tauri::{Manager, State, WindowEvent};

#[tauri::command]
fn relay_attach(relay: State<Relay>, on_message: Channel<RelayMessage>) -> Attached {
    relay.attach(on_message)
}

#[tauri::command]
fn relay_restart(relay: State<Relay>, on_message: Channel<RelayMessage>) -> Attached {
    relay.restart(on_message)
}

#[tauri::command]
fn relay_send(relay: State<Relay>, line: String) -> Result<(), String> {
    relay.send(&line)
}

pub fn run() {
    let jar = launch::jar(std::env::var("LAZYCLAUDE_SIDECAR").ok());
    let launch_dir = std::env::current_dir().unwrap_or_default();
    let project = launch::project(std::env::args().nth(1), launch_dir);
    tauri::Builder::default()
        .manage(Relay::new(jar, project))
        .invoke_handler(tauri::generate_handler![
            relay_attach,
            relay_restart,
            relay_send
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
