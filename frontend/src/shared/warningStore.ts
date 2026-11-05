import type { Client, WarningsView } from '../protocol';
import { Store } from './store';

export interface ShownWarning {
  text: string;
  count: number;
}

const shown = (view: WarningsView): ShownWarning | undefined => (view.kind === 'warning' ? { text: view.text, count: view.count } : undefined);

/** The warning the status line shows: `warning/watch`'s answer, then every `warning/changed`. */
export class WarningStore {
  readonly warning: Store<ShownWarning | undefined> = new Store<ShownWarning | undefined>(undefined);

  constructor(private readonly client: Client) {
    client.subscribe('warning/changed', (view) => this.warning.set(shown(view)));
  }

  async watch(): Promise<void> {
    try {
      this.warning.set(shown(await this.client.call('warning/watch', {})));
    } catch {
      this.warning.set(undefined);
    }
  }
}
