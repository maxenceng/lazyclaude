import type { IDisposable, Terminal } from '@xterm/xterm';

const CSI = '\x1b[';
const MOUSE_REPORTING = [9, 1000, 1002, 1003];
const KITTY_SET_ALL = 1;
const KITTY_SET_BITS = 2;
const KITTY_CLEAR_BITS = 3;

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
    if (how === KITTY_SET_BITS) {
      this.flags.push(current | flags);
    } else if (how === KITTY_CLEAR_BITS) {
      this.flags.push(current & ~flags);
    } else {
      this.flags.push(how === KITTY_SET_ALL ? flags : current);
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
    const modes = [...this.modes].map(([mode, on]) => `${CSI}?${mode}${on ? 'h' : 'l'}`);
    const keyboard = this.flags.map((flags) => `${CSI}>${flags}u`);
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
 * `changed` is told after each. Returns how to stop.
 */
export const trackModes = (term: Terminal, kept: KeptModes, changed: () => void): IDisposable => {
  const handle = (act: () => void) => () => {
    act();
    changed();
    return false;
  };
  const registrations = [
    term.parser.registerCsiHandler({ prefix: '?', final: 'h' }, (params) => handle(() => numbers(params).forEach((mode) => kept.set(mode, true)))()),
    term.parser.registerCsiHandler({ prefix: '?', final: 'l' }, (params) => handle(() => numbers(params).forEach((mode) => kept.set(mode, false)))()),
    term.parser.registerCsiHandler({ prefix: '>', final: 'u' }, (params) => handle(() => kept.pushKeyboard(numbers(params)[0] ?? 0))()),
    term.parser.registerCsiHandler({ prefix: '<', final: 'u' }, (params) => handle(() => kept.popKeyboard(Math.max(1, numbers(params)[0] ?? 1)))()),
    term.parser.registerCsiHandler({ prefix: '=', final: 'u' }, (params) => {
      const [flags = 0, how = 1] = numbers(params);
      return handle(() => kept.setKeyboard(flags, how))();
    }),
    term.parser.registerCsiHandler({ intermediates: '!', final: 'p' }, handle(() => kept.clear())),
    term.parser.registerEscHandler({ final: 'c' }, handle(() => kept.clear())),
  ];
  return { dispose: () => registrations.forEach((registration) => registration.dispose()) };
};
