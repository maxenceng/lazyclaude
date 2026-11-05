import type { IDisposable, Terminal } from '@xterm/xterm';

const CSI = '\x1b[';

/** The DEC private modes `claude` sets and resets, by the number `CSI ? n h` and `CSI ? n l` carry. */
export enum PrivateMode {
  X10Mouse = 9,
  CursorVisible = 25,
  MouseButtons = 1000,
  MouseDrag = 1002,
  MouseMotion = 1003,
  FocusReports = 1004,
  SgrMouse = 1006,
  AlternateScreen = 1049,
  BracketedPaste = 2004,
  ColourSchemeReports = 2031,
}

/** The kitty keyboard protocol's flags `claude` pushes, by the bit each sets. */
export enum KittyFlag {
  Disambiguate = 1,
  ReportAlternateKeys = 4,
}

/** What comes between `CSI` and a sequence's parameters: a private mode, or one of the kitty keyboard stack's verbs. */
export enum CsiPrefix {
  PrivateMode = '?',
  KeyboardPush = '>',
  KeyboardPop = '<',
  KeyboardSet = '=',
}

/** The byte that ends a sequence, naming what it does. */
export enum CsiFinal {
  Set = 'h',
  Reset = 'l',
  Keyboard = 'u',
  SoftReset = 'p',
}

/** The intermediate byte of `CSI ! p`, the soft reset. */
export const SOFT_RESET_INTERMEDIATE = '!';

/** The final byte of `ESC c`, the full reset. */
export const FULL_RESET_FINAL = 'c';

const MOUSE_REPORTING = [PrivateMode.X10Mouse, PrivateMode.MouseButtons, PrivateMode.MouseDrag, PrivateMode.MouseMotion];

/** How `CSI = flags ; how u` changes the flags on top of the kitty keyboard stack: a number off the wire, any other keeping them. */
const KEYBOARD_SET_ALL = 1;
const KEYBOARD_SET_BITS = 2;
const KEYBOARD_CLEAR_BITS = 3;

/** The private modes an emulator was last asked for and the kitty keyboard flags it stacked: what a new emulator must be written to match it. */
export class KeptModes {
  private readonly modes = new Map<number, boolean>();
  private flags: number[] = [];

  set(mode: number, on: boolean): void {
    this.modes.set(mode, on);
  }

  pushKeyboard(flags: number): void {
    this.flags.push(flags);
  }

  popKeyboard(count: number): void {
    this.flags = this.flags.slice(0, Math.max(0, this.flags.length - count));
  }

  setKeyboard(flags: number, how: number): void {
    const current = this.flags.pop() ?? 0;
    if (how === KEYBOARD_SET_BITS) {
      this.flags.push(current | flags);
    } else if (how === KEYBOARD_CLEAR_BITS) {
      this.flags.push(current & ~flags);
    } else {
      this.flags.push(how === KEYBOARD_SET_ALL ? flags : current);
    }
  }

  clear(): void {
    this.modes.clear();
    this.flags = [];
  }

  /** Whether `claude` has asked for mouse reports, which is when the pane's own selection needs shift. */
  reportsMouse(): boolean {
    return MOUSE_REPORTING.some((mode) => this.modes.get(mode) === true);
  }

  /** The bytes that put a fresh emulator into these modes. */
  restoration(): string {
    const modes = [...this.modes].map(([mode, on]) => `${CSI}${CsiPrefix.PrivateMode}${mode}${on ? CsiFinal.Set : CsiFinal.Reset}`);
    const keyboard = this.flags.map((flags) => `${CSI}${CsiPrefix.KeyboardPush}${flags}${CsiFinal.Keyboard}`);
    return [...modes, ...keyboard].join('');
  }

  toJSON(): { modes: Array<[number, boolean]>; keyboard: number[] } {
    return { modes: [...this.modes], keyboard: this.flags };
  }

  static fromJSON(json: { modes: Array<[number, boolean]>; keyboard: number[] }): KeptModes {
    const kept = new KeptModes();
    json.modes.forEach(([mode, on]) => kept.set(mode, on));
    json.keyboard.forEach((flags) => kept.pushKeyboard(flags));
    return kept;
  }
}

const numbers = (params: ReadonlyArray<number | number[]>): number[] => params.flat();

/**
 * Records, into `kept`, every private mode and kitty keyboard flag the emulator is written, then lets xterm.js apply it as it would;
 * `changed` is told after each. `dispose` stops it.
 */
export class ModeTracker implements IDisposable {
  private readonly registrations: IDisposable[];

  constructor(
    term: Terminal,
    private readonly kept: KeptModes,
    private readonly changed: () => void,
  ) {
    const { parser } = term;
    this.registrations = [
      parser.registerCsiHandler({ prefix: CsiPrefix.PrivateMode, final: CsiFinal.Set }, (params) => this.record(() => this.setAll(params, true))),
      parser.registerCsiHandler({ prefix: CsiPrefix.PrivateMode, final: CsiFinal.Reset }, (params) => this.record(() => this.setAll(params, false))),
      parser.registerCsiHandler({ prefix: CsiPrefix.KeyboardPush, final: CsiFinal.Keyboard }, (params) =>
        this.record(() => kept.pushKeyboard(numbers(params)[0] ?? 0)),
      ),
      parser.registerCsiHandler({ prefix: CsiPrefix.KeyboardPop, final: CsiFinal.Keyboard }, (params) =>
        this.record(() => kept.popKeyboard(Math.max(1, numbers(params)[0] ?? 1))),
      ),
      parser.registerCsiHandler({ prefix: CsiPrefix.KeyboardSet, final: CsiFinal.Keyboard }, (params) => {
        const [flags = 0, how = KEYBOARD_SET_ALL] = numbers(params);
        return this.record(() => kept.setKeyboard(flags, how));
      }),
      parser.registerCsiHandler({ intermediates: SOFT_RESET_INTERMEDIATE, final: CsiFinal.SoftReset }, () => this.record(() => kept.clear())),
      parser.registerEscHandler({ final: FULL_RESET_FINAL }, () => this.record(() => kept.clear())),
    ];
  }

  dispose(): void {
    this.registrations.forEach((registration) => registration.dispose());
  }

  private setAll(params: ReadonlyArray<number | number[]>, on: boolean): void {
    numbers(params).forEach((mode) => this.kept.set(mode, on));
  }

  /** Acts, tells `changed`, and answers that xterm.js should still apply the sequence itself. */
  private record(act: () => void): boolean {
    act();
    this.changed();
    return false;
  }
}
