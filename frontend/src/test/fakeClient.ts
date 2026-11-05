import { type Client, Ready, type Status } from '../protocol';
import { createStore } from '../shared/store';

type Handler = (params: never) => void;

export interface FakeClient extends Client {
  calls: Array<{ method: string; params: unknown }>;
  answerNext(result: unknown): void;
  answer(method: string, result: unknown): void;
  fail(method: string, error: Error): void;
  failNext(error: Error): void;
  push(name: string, params: unknown): void;
  sent: Array<{ input: string; params: unknown }>;
  setStatus(status: Status): void;
  restarts: number;
}

/** A client whose calls wait for the test to answer them, in the order they were made. */
export const fakeClient = (initial: Status = new Ready()): FakeClient => {
  const status = createStore<Status>(initial);
  const waiting: Array<{ method: string; resolve(value: unknown): void; reject(error: Error): void }> = [];
  const take = (method: string) => {
    const at = waiting.findIndex((call) => call.method === method);
    return at === -1 ? undefined : waiting.splice(at, 1)[0];
  };
  const handlers = new Map<string, Set<Handler>>();
  const fake: FakeClient = {
    status,
    calls: [],
    sent: [],
    restarts: 0,
    connect: () => Promise.resolve(),
    restart() {
      fake.restarts += 1;
      return Promise.resolve();
    },
    call(method, params) {
      fake.calls.push({ method, params });
      return new Promise((resolve, reject) => waiting.push({ method, resolve, reject }));
    },
    send(input, params) {
      fake.sent.push({ input, params });
    },
    subscribe(name, handler) {
      const set = handlers.get(name) ?? new Set();
      set.add(handler);
      handlers.set(name, set);
      return () => set.delete(handler);
    },
    answerNext: (result) => waiting.shift()?.resolve(result),
    answer: (method, result) => take(method)?.resolve(result),
    fail: (method, error) => take(method)?.reject(error),
    failNext: (error) => waiting.shift()?.reject(error),
    push: (name, params) => handlers.get(name)?.forEach((handler) => handler(params as never)),
    setStatus: (next) => status.set(next),
  };
  return fake;
};
