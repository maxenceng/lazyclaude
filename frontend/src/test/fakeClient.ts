import type { Client, Status } from '../protocol';
import { Ready } from '../protocol';
import { Store } from '../shared/store';

type Handler = (params: never) => void;

interface Waiting {
  method: string;
  resolve(value: unknown): void;
  reject(error: Error): void;
}

/** A client whose calls wait for the test to answer them, in the order they were made. */
export class FakeClient implements Client {
  readonly status: Store<Status>;
  readonly calls: Array<{ method: string; params: unknown }> = [];
  readonly sent: Array<{ input: string; params: unknown }> = [];
  restarts = 0;
  private readonly waiting: Waiting[] = [];
  private readonly handlers = new Map<string, Set<Handler>>();

  constructor(initial: Status = new Ready()) {
    this.status = new Store<Status>(initial);
  }

  connect(): Promise<void> {
    return Promise.resolve();
  }

  restart(): Promise<void> {
    this.restarts += 1;
    return Promise.resolve();
  }

  call<R>(method: string, params: unknown): Promise<R> {
    this.calls.push({ method, params });
    return new Promise<unknown>((resolve, reject) => this.waiting.push({ method, resolve, reject })) as Promise<R>;
  }

  send(input: string, params: unknown): void {
    this.sent.push({ input, params });
  }

  subscribe(name: string, handler: Handler): () => void {
    const set = this.handlers.get(name) ?? new Set();
    set.add(handler);
    this.handlers.set(name, set);
    return () => set.delete(handler);
  }

  answerNext(result: unknown): void {
    this.waiting.shift()?.resolve(result);
  }

  answerLast(result: unknown): void {
    this.waiting.pop()?.resolve(result);
  }

  answer(method: string, result: unknown): void {
    this.take(method)?.resolve(result);
  }

  fail(method: string, error: Error): void {
    this.take(method)?.reject(error);
  }

  failNext(error: Error): void {
    this.waiting.shift()?.reject(error);
  }

  push(name: string, params: unknown): void {
    this.handlers.get(name)?.forEach((handler) => handler(params as never));
  }

  setStatus(next: Status): void {
    this.status.set(next);
  }

  private take(method: string): Waiting | undefined {
    const at = this.waiting.findIndex((call) => call.method === method);
    return at === -1 ? undefined : this.waiting.splice(at, 1)[0];
  }
}

export const fakeClient = (initial: Status = new Ready()): FakeClient => new FakeClient(initial);
