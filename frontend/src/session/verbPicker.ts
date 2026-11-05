import type { Verb } from '../protocol';

/**
 * The verb picker over one ticket: its verbs and the cursor.
 */
// deferred: the cursor starts on the first verb, not on the verb the backend would preselect — close it when the protocol carries the verb choice.
export class VerbPicker {
  constructor(
    readonly ticket: string,
    readonly branch: string,
    readonly verbs: readonly Verb[],
    private readonly cursor: number = 0,
  ) {}

  selected(): Verb | undefined {
    return this.verbs[this.cursor];
  }

  cursorAt(): number {
    return this.cursor;
  }

  down(): VerbPicker {
    return this.at(this.cursor + 1);
  }

  up(): VerbPicker {
    return this.at(this.cursor - 1);
  }

  at(index: number): VerbPicker {
    return new VerbPicker(this.ticket, this.branch, this.verbs, Math.min(this.verbs.length - 1, Math.max(0, index)));
  }
}
