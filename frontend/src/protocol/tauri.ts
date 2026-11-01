import { Channel, invoke } from '@tauri-apps/api/core';
import type { Attached, Relay, RelayMessage } from './relay';

function attachWith(command: string) {
  return (onMessage: (message: RelayMessage) => void) => {
    const channel = new Channel<RelayMessage>();
    channel.onmessage = onMessage;
    return invoke<Attached>(command, { onMessage: channel });
  };
}

/** The relay the Tauri side serves: batched lines in through a Channel, one line out per command. */
export const tauriRelay: Relay = {
  attach: attachWith('relay_attach'),
  restart: attachWith('relay_restart'),
  send: (line) => invoke('relay_send', { line }),
};
