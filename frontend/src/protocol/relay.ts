export interface Exit {
  code: number | null;
  stderr: string[];
}

export type RelayMessage = { kind: 'lines'; lines: string[] } | ({ kind: 'exited' } & Exit);

export interface Attached {
  ready: string | null;
  exit: Exit | null;
}

/** The two-way pipe to the sidecar's lines; the Tauri relay implements it, a test fakes it. */
export interface Relay {
  attach(onMessage: (message: RelayMessage) => void): Promise<Attached>;
  restart(onMessage: (message: RelayMessage) => void): Promise<Attached>;
  send(line: string): Promise<void>;
}
