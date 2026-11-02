pub mod launch;
pub mod lines;
mod relay;

use std::sync::Arc;

use relay::{Attached, OnMessage, Relay, RelayMessage, Sidecar};
use tauri::ipc::Channel;
use tauri::{Manager, State, WindowEvent};

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

pub fn run() {
    let launch_dir = std::env::current_dir().unwrap_or_default();
    let jar = launch::jar(std::env::var("LAZYCLAUDE_SIDECAR").ok(), &launch_dir);
    let project = launch::project(std::env::args().nth(1), launch_dir);
    tauri::Builder::default()
        .manage(Relay::new(Sidecar::java(jar, project)))
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
