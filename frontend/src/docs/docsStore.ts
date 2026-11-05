import type { Client, DocsView } from '../protocol';
import { Store } from '../shared/store';
import { type DocsState, EmptyDocs, FaultedDocs, LoadingDocs, PopulatedDocs, UnavailableDocs } from './docsState';

const stateOf = (view: DocsView): DocsState => {
  switch (view.kind) {
    case 'docsTree':
      return view.documents.length === 0 && view.folders.length === 0 ? new EmptyDocs() : new PopulatedDocs(view);
    case 'unavailableDocs':
      return new UnavailableDocs(view.reason);
  }
};

/** The Docs panel's one read: `docs/watch`'s answer, then every `docs/changed` after it. */
export class DocsStore {
  readonly state: Store<DocsState> = new Store<DocsState>(new LoadingDocs());

  constructor(private readonly client: Client) {
    client.subscribe('docs/changed', (view) => this.state.set(stateOf(view)));
  }

  async load(root: string): Promise<void> {
    this.state.set(new LoadingDocs());
    try {
      this.state.set(stateOf(await this.client.call('docs/watch', { root })));
    } catch (error) {
      this.state.set(new FaultedDocs(error instanceof Error ? error.message : String(error)));
    }
  }
}
