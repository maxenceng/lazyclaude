import type { Client } from '../protocol';
import { Store } from '../shared/store';
import { bindingsOf, type Binding } from './chords';
import { type ConfigurationState, FaultedConfiguration, ReadConfiguration, ReadingConfiguration } from './configurationState';

/** The configuration read at start-up: the chords it reserves, or why it could not be read. */
export class ConfigurationStore {
  readonly state: Store<ConfigurationState> = new Store<ConfigurationState>(new ReadingConfiguration());

  constructor(private readonly client: Client) {}

  reserved(): readonly Binding[] {
    return this.state.get().reserved();
  }

  async load(): Promise<void> {
    this.state.set(new ReadingConfiguration());
    try {
      this.state.set(new ReadConfiguration(bindingsOf(await this.client.call('session/configuration', {}))));
    } catch (error) {
      this.state.set(new FaultedConfiguration(error instanceof Error ? error.message : String(error)));
    }
  }
}
