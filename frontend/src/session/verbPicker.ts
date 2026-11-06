import type { BoardVerb } from '../protocol';

/** The verb picker over one ticket: its verbs and the cursor, which opens on the verb `session` preselects. */
export class VerbPicker {
  constructor(
    readonly ticket: string,
    readonly branch: string,
    readonly verbs: readonly BoardVerb[],
    private readonly cursor: number = 0,
  ) {}

  static opening(ticket: string, branch: string, verbs: readonly BoardVerb[], preselected: BoardVerb): VerbPicker {
    return new VerbPicker(ticket, branch, verbs, Math.max(0, verbs.indexOf(preselected)));
  }

  selected(): BoardVerb | undefined {
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
