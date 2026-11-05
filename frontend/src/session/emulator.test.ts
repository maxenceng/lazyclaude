import { Terminal } from '@xterm/xterm';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fromBase64, toBase64, bytesOfText } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { Emulator, type EmulatorDeps } from './emulator';
import { createModeMemory } from './modeMemory';
import { CLAUDE_START, CLAUDE_STOPS_REPORTING_THE_MOUSE } from './recordings';

const written = (term: Terminal) => new Promise<void>((done) => term.write('', done));

const textOf = (sent: unknown): string => new TextDecoder().decode(fromBase64((sent as { data: string }).data));

const emulate = (overrides: Partial<EmulatorDeps> = {}) => {
  const client = fakeClient();
  const memory = createModeMemory(sessionStorage);
  const fit = { fit: vi.fn() };
  const deps: EmulatorDeps = { client, memory, fit, reserved: () => undefined, clipboard: { readText: () => Promise.resolve(''), writeText: () => Promise.resolve() }, ...overrides };
  const emulator = new Emulator('s1', new Terminal(), deps);
  emulator.terminal.open(document.body.appendChild(document.createElement('div')));
  return { client, memory, fit, emulator, term: emulator.terminal };
};

const encoded = (text: string) => toBase64(bytesOfText(text));

describe('a hosted session\'s emulator', () => {
  beforeEach(() => sessionStorage.clear());

  it('is written the output it is given, decoded from base64', async () => {
    const { emulator, term } = emulate();
    emulator.write(encoded('mine'));
    await written(term);
    expect(term.buffer.active.getLine(0)?.translateToString(true)).toBe('mine');
  });

  it('sends the keys xterm.js encodes as pty/input bytes', () => {
    const { client, term } = emulate();
    term.input('é');
    expect(client.sent).toEqual([{ input: 'pty/input', params: { session: 's1', data: toBase64(bytesOfText('é')) } }]);
  });

  it('sends a binary report, a mouse past column 223, as one byte per character', () => {
    const { client, emulator } = emulate();
    emulator.binary('\x1b[M\xe9');
    expect(client.sent).toEqual([{ input: 'pty/input', params: { session: 's1', data: toBase64(Uint8Array.from([27, 91, 77, 233])) } }]);
  });

  it('keeps its modes in the memory as claude sets them', async () => {
    const { emulator, memory, term } = emulate();
    emulator.write(encoded(CLAUDE_START));
    await written(term);
    expect(memory.recall('s1')?.restoration()).toContain('\x1b[?2004h');
  });

  it('writes the kept modes into the emulator of a reloaded page and then asks for a repaint', async () => {
    const before = emulate();
    before.emulator.write(encoded(CLAUDE_START));
    await written(before.term);
    const after = emulate();
    expect(after.client.sent.filter((sent) => sent.input === 'session/repaint')).toEqual([]);
    await written(after.term);
    expect(after.term.modes.bracketedPasteMode).toBe(true);
    expect(after.term.buffer.active.type).toBe('alternate');
    expect(after.client.sent.filter((sent) => sent.input === 'session/repaint')).toEqual([{ input: 'session/repaint', params: { session: 's1' } }]);
  });

  it('sends a paste bracketed after a reload, the way it was before', async () => {
    const before = emulate();
    before.emulator.write(encoded(CLAUDE_START));
    await written(before.term);
    const after = emulate();
    await written(after.term);
    after.client.sent.length = 0;
    after.term.paste('two\nlines');
    expect(textOf(after.client.sent[0]?.params)).toBe('\x1b[200~two\rlines\x1b[201~');
  });

  it('asks for no repaint when it has no modes to restore', async () => {
    const { client, term } = emulate();
    await written(term);
    expect(client.sent).toEqual([]);
  });

  it('fits the emulator first and sends the size it came to', () => {
    const fit = { fit: vi.fn() };
    const { client, term, emulator } = emulate({ fit });
    fit.fit.mockImplementation(() => {
      expect(client.sent).toEqual([]);
      term.resize(100, 30);
    });
    emulator.resize();
    expect(client.sent).toEqual([{ input: 'pty/resize', params: { session: 's1', cols: 100, rows: 30 } }]);
  });

  it('tells while claude reports the mouse and when it stops', async () => {
    const { term, emulator } = emulate();
    emulator.write(encoded(CLAUDE_START));
    await written(term);
    expect(emulator.mouseReported.get()).toBe(true);
    emulator.write(encoded(CLAUDE_STOPS_REPORTING_THE_MOUSE));
    await written(term);
    expect(emulator.mouseReported.get()).toBe(false);
  });

  it('forgets the modes of a session that ended', async () => {
    const { memory, term, emulator } = emulate();
    emulator.write(encoded(CLAUDE_START));
    await written(term);
    emulator.end();
    expect(memory.recall('s1')).toBeUndefined();
  });
});

describe('the keys of a hosted session\'s pane', () => {
  beforeEach(() => sessionStorage.clear());

  const keydown = (key: string, init: KeyboardEventInit = {}) => new KeyboardEvent('keydown', { key, cancelable: true, ...init });

  it('keeps a reserved chord from claude, acts on it and cancels the webview\'s own action', () => {
    const act = vi.fn();
    const { emulator } = emulate({ reserved: (press) => (press.altKey ? act : undefined) });
    const event = keydown('h', { altKey: true });
    expect(emulator.handleKey(event)).toBe(false);
    expect(act).toHaveBeenCalledTimes(1);
    expect(event.defaultPrevented).toBe(true);
  });

  it('acts on a reserved chord once, on its key down and not on its key up', () => {
    const act = vi.fn();
    const { emulator } = emulate({ reserved: () => act });
    emulator.handleKey(new KeyboardEvent('keyup', { key: 'h', altKey: true }));
    expect(act).not.toHaveBeenCalled();
  });

  it('keeps ctrl+z from claude and does nothing with it', () => {
    const { client, emulator } = emulate();
    expect(emulator.handleKey(keydown('z', { ctrlKey: true }))).toBe(false);
    expect(client.sent).toEqual([]);
  });

  it('leaves ctrl+c and a plain ctrl+v to the terminal, which sends them to claude', () => {
    const { emulator } = emulate();
    expect(emulator.handleKey(keydown('c', { ctrlKey: true }))).toBe(true);
    expect(emulator.handleKey(keydown('v', { ctrlKey: true }))).toBe(true);
  });

  it('copies the selection on ctrl+shift+c', () => {
    const writeText = vi.fn(() => Promise.resolve());
    const { term, emulator } = emulate({ clipboard: { readText: () => Promise.resolve(''), writeText } });
    term.write('selected');
    return written(term).then(() => {
      term.select(0, 0, 8);
      expect(emulator.handleKey(keydown('C', { ctrlKey: true, shiftKey: true }))).toBe(false);
      expect(writeText).toHaveBeenCalledWith('selected');
    });
  });

  it('copies nothing when nothing is selected', () => {
    const writeText = vi.fn(() => Promise.resolve());
    const { emulator } = emulate({ clipboard: { readText: () => Promise.resolve(''), writeText } });
    emulator.handleKey(keydown('C', { ctrlKey: true, shiftKey: true }));
    expect(writeText).not.toHaveBeenCalled();
  });

  it.each([
    ['ctrl+shift+v', 'V', { ctrlKey: true, shiftKey: true }],
    ['shift+insert', 'Insert', { shiftKey: true }],
  ])('pastes the clipboard through term.paste() on %s, bracketed while claude asked for it', async (_name, key, held) => {
    const { client, term, emulator } = emulate({ clipboard: { readText: () => Promise.resolve('pasted'), writeText: () => Promise.resolve() } });
    emulator.write(encoded('\x1b[?2004h'));
    await written(term);
    const event = keydown(key, held);
    expect(emulator.handleKey(event)).toBe(false);
    expect(event.defaultPrevented).toBe(true);
    await vi.waitFor(() => expect(client.sent).toHaveLength(1));
    expect(textOf(client.sent[0]?.params)).toBe('\x1b[200~pasted\x1b[201~');
  });

  it('pastes unbracketed while claude did not ask for bracketed paste', async () => {
    const { client, emulator } = emulate({ clipboard: { readText: () => Promise.resolve('pasted'), writeText: () => Promise.resolve() } });
    emulator.handleKey(keydown('V', { ctrlKey: true, shiftKey: true }));
    await vi.waitFor(() => expect(client.sent).toHaveLength(1));
    expect(textOf(client.sent[0]?.params)).toBe('pasted');
  });

  it('drops a paste the clipboard refuses, sending nothing', async () => {
    const { client, emulator } = emulate({ clipboard: { readText: () => Promise.reject(new Error('denied')), writeText: () => Promise.resolve() } });
    emulator.handleKey(keydown('V', { ctrlKey: true, shiftKey: true }));
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(client.sent).toEqual([]);
  });

  it('drops a copy the clipboard refuses', async () => {
    const { term, emulator } = emulate({ clipboard: { readText: () => Promise.resolve(''), writeText: () => Promise.reject(new Error('denied')) } });
    term.write('selected');
    await written(term);
    term.select(0, 0, 8);
    expect(emulator.handleKey(keydown('C', { ctrlKey: true, shiftKey: true }))).toBe(false);
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
});
