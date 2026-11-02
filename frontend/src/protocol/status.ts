export enum StatusKind {
  Connecting = 'connecting',
  Ready = 'ready',
  Refused = 'refused',
  Exited = 'exited',
}

/** The sidecar is starting, or restarting; calls wait for it. */
export class Connecting {
  readonly kind = StatusKind.Connecting;

  isReady(): boolean {
    return false;
  }

  isClosed(): boolean {
    return false;
  }
}

/** The sidecar said `ready` on this window's protocol; calls go out. */
export class Ready {
  readonly kind = StatusKind.Ready;

  isReady(): boolean {
    return true;
  }

  isClosed(): boolean {
    return false;
  }
}

/** The sidecar said `ready` on another protocol; nothing is sent to it. */
export class Refused {
  readonly kind = StatusKind.Refused;

  constructor(
    readonly expected: number,
    readonly actual: number,
  ) {}

  isReady(): boolean {
    return false;
  }

  isClosed(): boolean {
    return true;
  }
}

/** The sidecar ended unasked: its exit code, when it had one, and its last stderr lines. */
export class Exited {
  readonly kind = StatusKind.Exited;

  constructor(
    readonly code: number | null,
    readonly stderr: readonly string[],
  ) {}

  isReady(): boolean {
    return false;
  }

  isClosed(): boolean {
    return true;
  }
}

export type Status = Connecting | Ready | Refused | Exited;

export type Unready = Exclude<Status, Ready>;
