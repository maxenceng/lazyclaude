import { Terminal } from '@xterm/xterm';
import { describe, expect, it, vi } from 'vitest';
import {
  CLAUDE_LEAVES_ALTERNATE_SCREEN,
  CLAUDE_POPS_KITTY_KEYBOARD,
  CLAUDE_PUSHES_ANOTHER_KITTY_KEYBOARD,
  CLAUDE_START,
  CLAUDE_STOPS_REPORTING_THE_MOUSE,
  FULL_RESET,
  SOFT_RESET,
} from './recordings';
import { KeptModes, ModeTracker } from './privateModes';

const feed = (term: Terminal, bytes: string) => new Promise<void>((done) => term.write(bytes, done));

const tracked = () => {
  const term = new Terminal();
  const kept = new KeptModes();
  const changed = vi.fn();
  new ModeTracker(term, kept, changed);
  return { term, kept, changed };
};

const restored = async (kept: KeptModes) => {
  const next = new Terminal();
  await feed(next, kept.restoration());
  return next;
};

describe('the private modes kept from what the emulator is written', () => {
  it('keeps the modes claude sets while xterm.js goes on to apply them', async () => {
    const { term, kept } = tracked();
    await feed(term, CLAUDE_START);
    expect(kept.restoration()).toContain('\x1b[?1049h');
    expect(kept.restoration()).toContain('\x1b[?2004h');
    expect(term.modes.bracketedPasteMode).toBe(true);
  });

  it.each([1049, 2004, 1004, 2031, 1000, 1002, 1003, 1006])('keeps ?%i, which claude sets at start', async (mode) => {
    const { term, kept } = tracked();
    await feed(term, CLAUDE_START);
    expect(kept.restoration()).toContain(`\x1b[?${mode}h`);
  });

  it('keeps a mode no one listed, since any other it sees is kept', async () => {
    const { term, kept } = tracked();
    await feed(term, '\x1b[?9999h');
    expect(kept.restoration()).toBe('\x1b[?9999h');
  });

  it('keeps the last word on a mode, not each word', async () => {
    const { term, kept } = tracked();
    await feed(term, CLAUDE_START + CLAUDE_LEAVES_ALTERNATE_SCREEN);
    expect(kept.restoration()).toContain('\x1b[?1049l');
    expect(kept.restoration()).not.toContain('\x1b[?1049h');
  });

  it('writes a fresh emulator into the same bracketed paste, alternate screen and focus reports', async () => {
    const { term, kept } = tracked();
    await feed(term, CLAUDE_START);
    const next = await restored(kept);
    expect(next.modes.bracketedPasteMode).toBe(true);
    expect(next.modes.sendFocusMode).toBe(true);
    expect(next.buffer.active.type).toBe('alternate');
    expect(next.modes.mouseTrackingMode).toBe(term.modes.mouseTrackingMode);
  });

  it('writes a fresh emulator out of a mode claude left again', async () => {
    const { term, kept } = tracked();
    await feed(term, CLAUDE_START + CLAUDE_LEAVES_ALTERNATE_SCREEN);
    const next = await restored(kept);
    expect(next.buffer.active.type).toBe('normal');
  });

  it('tells it changed after a mode is set', async () => {
    const { term, changed } = tracked();
    await feed(term, '\x1b[?2004h');
    expect(changed).toHaveBeenCalledTimes(1);
  });

  it('reports the mouse while claude asks for it and not once it stops', async () => {
    const { term, kept } = tracked();
    expect(kept.reportsMouse()).toBe(false);
    await feed(term, CLAUDE_START);
    expect(kept.reportsMouse()).toBe(true);
    await feed(term, CLAUDE_STOPS_REPORTING_THE_MOUSE);
    expect(kept.reportsMouse()).toBe(false);
  });

  it('keeps the kitty keyboard flags claude pushed and forgets the ones it popped', async () => {
    const { term, kept } = tracked();
    await feed(term, CLAUDE_START);
    expect(kept.restoration()).toContain('\x1b[>1u');
    await feed(term, CLAUDE_PUSHES_ANOTHER_KITTY_KEYBOARD);
    expect(kept.restoration()).toContain('\x1b[>1u\x1b[>5u');
    await feed(term, CLAUDE_POPS_KITTY_KEYBOARD);
    expect(kept.restoration()).toContain('\x1b[>1u');
    expect(kept.restoration()).not.toContain('\x1b[>5u');
  });

  it('keeps flags claude set in place, setting, adding and clearing bits', async () => {
    const { term, kept } = tracked();
    await feed(term, '\x1b[=1;1u');
    expect(kept.restoration()).toBe('\x1b[>1u');
    await feed(term, '\x1b[=4;2u');
    expect(kept.restoration()).toBe('\x1b[>5u');
    await feed(term, '\x1b[=1;3u');
    expect(kept.restoration()).toBe('\x1b[>4u');
  });

  it('keeps nothing across a soft or a full reset', async () => {
    const { term, kept } = tracked();
    await feed(term, CLAUDE_START + SOFT_RESET);
    expect(kept.restoration()).toBe('');
    await feed(term, CLAUDE_START + FULL_RESET);
    expect(kept.restoration()).toBe('');
  });

  it('stops keeping once disposed', async () => {
    const term = new Terminal();
    const kept = new KeptModes();
    new ModeTracker(term, kept, () => {}).dispose();
    await feed(term, CLAUDE_START);
    expect(kept.restoration()).toBe('');
  });

  it('survives a round trip through JSON', async () => {
    const { term, kept } = tracked();
    await feed(term, CLAUDE_START);
    const again = KeptModes.fromJSON(JSON.parse(JSON.stringify(kept)) as ReturnType<KeptModes['toJSON']>);
    expect(again.restoration()).toBe(kept.restoration());
    expect(again.reportsMouse()).toBe(true);
  });
});
