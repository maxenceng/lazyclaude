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
import { type Attached, type Relay, type RelayMessage, RelayMessageKind } from './relay';
import { Connecting, Exited, Ready, Refused, type Status, StatusKind } from './status';
import { Store } from '../shared/store';

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
    super(status.kind === StatusKind.Refused ? 'the sidecar speaks another protocol' : 'the sidecar is not running');
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
const IDS_PER_MILLISECOND = 1000;

export interface Client {
  readonly status: Store<Status>;
  connect(): Promise<void>;
  restart(): Promise<void>;
  call<M extends MethodName>(method: M, params: Params<M>): Promise<Result<M>>;
  send<I extends InputName>(input: I, params: InputParams<I>): void;
  subscribe<N extends NotificationName>(name: N, handler: (params: NotificationParams<N>) => void): () => void;
}

/** The sidecar's JSON-RPC over the relay: calls answered by id, inputs sent, pushes handed to their subscribers, and the status. */
export class ProtocolClient implements Client {
  readonly status: Store<Status> = new Store<Status>(new Connecting());
  private readonly pending = new Map<number, Pending>();
  private readonly waiting: Array<() => void> = [];
  private readonly handlers = new Map<string, Set<(params: never) => void>>();
  // A page reloaded onto a running sidecar starts past the ids the page before it may still have in flight.
  private nextId = Date.now() * IDS_PER_MILLISECOND;

  constructor(private readonly relay: Relay) {}

  async connect(): Promise<void> {
    this.onAttached(await this.relay.attach((message) => this.onMessage(message)));
  }

  async restart(): Promise<void> {
    this.failAll();
    this.status.set(new Connecting());
    this.onAttached(await this.relay.restart((message) => this.onMessage(message)));
  }

  call<M extends MethodName>(method: M, params: Params<M>): Promise<Result<M>> {
    const current = this.status.get();
    if (current.isClosed()) {
      return Promise.reject(new SidecarUnavailableError(current));
    }
    const id = this.nextId++;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      this.deliver(() => {
        this.write({ id, method, params }).catch((error: unknown) => {
          this.pending.delete(id);
          reject(error instanceof Error ? error : new Error(String(error)));
        });
      });
    });
  }

  send<I extends InputName>(input: I, params: InputParams<I>): void {
    this.deliver(() => void this.write({ method: input, params }));
  }

  subscribe<N extends NotificationName>(name: N, handler: (params: NotificationParams<N>) => void): () => void {
    const set = this.handlers.get(name) ?? new Set();
    set.add(handler);
    this.handlers.set(name, set);
    return () => set.delete(handler);
  }

  private failAll(): void {
    const error = new SidecarUnavailableError(this.status.get());
    this.pending.forEach((call) => call.reject(error));
    this.pending.clear();
    this.waiting.length = 0;
  }

  private deliver(write: () => void): void {
    const current = this.status.get();
    if (current.isReady()) {
      write();
    } else if (!current.isClosed()) {
      this.waiting.push(write);
    }
  }

  private write(message: object): Promise<void> {
    return this.relay.send(JSON.stringify({ jsonrpc: '2.0', ...message }));
  }

  private onReady(params: unknown): void {
    const actual = (params as { protocol: number }).protocol;
    if (actual !== PROTOCOL_VERSION) {
      this.status.set(new Refused(PROTOCOL_VERSION, actual));
      this.failAll();
      return;
    }
    this.status.set(new Ready());
    this.waiting.splice(0).forEach((send) => send());
  }

  private onLine(line: string): void {
    let message: Incoming;
    try {
      message = JSON.parse(line) as Incoming;
    } catch {
      return;
    }
    if (message.id !== undefined) {
      const call = this.pending.get(message.id);
      this.pending.delete(message.id);
      if (message.error) {
        call?.reject(new RpcError(message.error.code, message.error.message));
      } else {
        call?.resolve(message.result as never);
      }
    } else if (message.method === READY) {
      this.onReady(message.params);
    } else if (message.method !== undefined) {
      const handlers = this.handlers.get(message.method);
      // A push waits one microtask, so a caller awaiting the answer on an earlier line of its batch sees that answer first.
      queueMicrotask(() => handlers?.forEach((handler) => handler(message.params as never)));
    }
  }

  private onExit(code: number | null, stderr: string[]): void {
    this.status.set(new Exited(code, stderr));
    this.failAll();
  }

  private onMessage(message: RelayMessage): void {
    if (message.kind === RelayMessageKind.Lines) {
      message.lines.forEach((line) => this.onLine(line));
    } else {
      this.onExit(message.code, message.stderr);
    }
  }

  private onAttached(attached: Attached): void {
    if (attached.ready !== null) {
      this.onLine(attached.ready);
    }
    if (attached.exit !== null) {
      this.onExit(attached.exit.code, attached.exit.stderr);
    }
  }
}
