import {
  type InputName,
  type InputParams,
  type MethodName,
  type NotificationName,
  type NotificationParams,
  type Params,
  PROTOCOL_VERSION,
  type Result,
} from './generated/protocol';
import type { Attached, Relay, RelayMessage } from './relay';
import { createStore, type Store } from './store';

export type Status =
  | { kind: 'connecting' }
  | { kind: 'ready' }
  | { kind: 'refused'; expected: number; actual: number }
  | { kind: 'exited'; code: number | null; stderr: string[] };

export class RpcError extends Error {
  constructor(
    readonly code: number,
    message: string,
  ) {
    super(message);
    this.name = 'RpcError';
  }
}

export class SidecarUnavailableError extends Error {
  constructor(status: Status) {
    super(status.kind === 'refused' ? 'the sidecar speaks another protocol' : 'the sidecar is not running');
    this.name = 'SidecarUnavailableError';
  }
}

interface Pending {
  resolve(result: never): void;
  reject(error: Error): void;
}

interface Incoming {
  id?: number;
  method?: string;
  params?: unknown;
  result?: unknown;
  error?: { code: number; message: string };
}

const READY = 'ready';

export interface Client {
  readonly status: Store<Status>;
  connect(): Promise<void>;
  restart(): Promise<void>;
  call<M extends MethodName>(method: M, params: Params<M>): Promise<Result<M>>;
  send<I extends InputName>(input: I, params: InputParams<I>): void;
  subscribe<N extends NotificationName>(name: N, handler: (params: NotificationParams<N>) => void): () => void;
}

export function createClient(relay: Relay): Client {
  const status = createStore<Status>({ kind: 'connecting' });
  const pending = new Map<number, Pending>();
  const waiting: Array<() => void> = [];
  const handlers = new Map<string, Set<(params: never) => void>>();
  let nextId = 1;

  const failAll = () => {
    const error = new SidecarUnavailableError(status.get());
    pending.forEach((call) => call.reject(error));
    pending.clear();
    waiting.length = 0;
  };

  const deliver = (write: () => void) => {
    const current = status.get();
    if (current.kind === 'ready') {
      write();
    } else if (current.kind === 'connecting') {
      waiting.push(write);
    }
  };

  const write = (message: object) => relay.send(JSON.stringify({ jsonrpc: '2.0', ...message }));

  const onReady = (params: unknown) => {
    const actual = (params as { protocol: number }).protocol;
    if (actual !== PROTOCOL_VERSION) {
      status.set({ kind: 'refused', expected: PROTOCOL_VERSION, actual });
      failAll();
      return;
    }
    status.set({ kind: 'ready' });
    waiting.splice(0).forEach((send) => send());
  };

  const onLine = (line: string) => {
    let message: Incoming;
    try {
      message = JSON.parse(line) as Incoming;
    } catch {
      return;
    }
    if (message.id !== undefined) {
      const call = pending.get(message.id);
      pending.delete(message.id);
      if (message.error) {
        call?.reject(new RpcError(message.error.code, message.error.message));
      } else {
        call?.resolve(message.result as never);
      }
    } else if (message.method === READY) {
      onReady(message.params);
    } else if (message.method !== undefined) {
      handlers.get(message.method)?.forEach((handler) => handler(message.params as never));
    }
  };

  const onExit = (code: number | null, stderr: string[]) => {
    status.set({ kind: 'exited', code, stderr });
    failAll();
  };

  const onMessage = (message: RelayMessage) => {
    if (message.kind === 'lines') {
      message.lines.forEach(onLine);
    } else {
      onExit(message.code, message.stderr);
    }
  };

  const onAttached = (attached: Attached) => {
    if (attached.ready !== null) {
      onLine(attached.ready);
    }
    if (attached.exit !== null) {
      onExit(attached.exit.code, attached.exit.stderr);
    }
  };

  return {
    status,
    async connect() {
      onAttached(await relay.attach(onMessage));
    },
    async restart() {
      failAll();
      status.set({ kind: 'connecting' });
      onAttached(await relay.restart(onMessage));
    },
    call(method, params) {
      const current = status.get();
      if (current.kind === 'refused' || current.kind === 'exited') {
        return Promise.reject(new SidecarUnavailableError(current));
      }
      const id = nextId++;
      return new Promise((resolve, reject) => {
        pending.set(id, { resolve, reject });
        deliver(() => {
          write({ id, method, params }).catch((error: unknown) => {
            pending.delete(id);
            reject(error instanceof Error ? error : new Error(String(error)));
          });
        });
      });
    },
    send(input, params) {
      deliver(() => void write({ method: input, params }));
    },
    subscribe(name, handler) {
      const set = handlers.get(name) ?? new Set();
      set.add(handler);
      handlers.set(name, set);
      return () => set.delete(handler);
    },
  };
}
