import type { Binding } from './chords';

export enum ConfigurationKind {
  Reading = 'reading',
  Read = 'read',
  Fault = 'fault',
}

/** `session/configuration` has not answered yet; no chord is reserved meanwhile. */
export class ReadingConfiguration {
  readonly kind = ConfigurationKind.Reading;

  reserved(): readonly Binding[] {
    return [];
  }
}

/** The configuration was read: its chords are reserved. */
export class ReadConfiguration {
  readonly kind = ConfigurationKind.Read;

  constructor(readonly bindings: readonly Binding[]) {}

  reserved(): readonly Binding[] {
    return this.bindings;
  }
}

/** The configuration could not be read; the window refuses to start until a retry reads it. */
export class FaultedConfiguration {
  readonly kind = ConfigurationKind.Fault;

  constructor(readonly message: string) {}

  reserved(): readonly Binding[] {
    return [];
  }
}

export type ConfigurationState = ReadingConfiguration | ReadConfiguration | FaultedConfiguration;
