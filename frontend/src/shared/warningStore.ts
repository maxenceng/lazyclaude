import type { Client, WarningsView } from '../protocol';
import { createStore, type Store } from './store';

export interface ShownWarning {
  text: string;
  count: number;
}

export interface WarningStore {
  readonly warning: Store<ShownWarning | undefined>;
  watch(): Promise<void>;
}

const shown = (view: WarningsView): ShownWarning | undefined => (view.kind === 'warning' ? { text: view.text, count: view.count } : undefined);

/** The warning the status line shows: `warning/watch`'s answer, then every `warning/changed`. */
export const createWarningStore = (client: Client): WarningStore => {
  const warning = createStore<ShownWarning | undefined>(undefined);
  client.subscribe('warning/changed', (view) => warning.set(shown(view)));
  return {
    warning,
    async watch() {
      try {
        warning.set(shown(await client.call('warning/watch', {})));
      } catch {
        warning.set(undefined);
      }
    },
  };
};
