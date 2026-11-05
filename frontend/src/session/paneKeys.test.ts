import { describe, expect, it } from 'vitest';
import { PaneKey, type PaneKeyPress, paneKeyOf } from './paneKeys';

const press = (key: string, held: Partial<PaneKeyPress> = {}): PaneKeyPress => ({
  key,
  ctrlKey: false,
  altKey: false,
  metaKey: false,
  shiftKey: false,
  ...held,
});

const none = () => false;

describe('what a pane does with a key', () => {
  it('keeps a reserved chord from claude', () => {
    expect(paneKeyOf(press('h', { altKey: true }), (candidate) => candidate.altKey)).toBe(PaneKey.Reserved);
  });

  it('takes a reserved chord before the pane takes a key of its own', () => {
    expect(paneKeyOf(press('C', { ctrlKey: true, shiftKey: true }), () => true)).toBe(PaneKey.Reserved);
  });

  it('keeps ctrl+z from claude, since nothing could resume a suspended claude under a pty', () => {
    expect(paneKeyOf(press('z', { ctrlKey: true }), none)).toBe(PaneKey.Suspend);
  });

  it('lets ctrl+c and a plain ctrl+v reach claude', () => {
    expect(paneKeyOf(press('c', { ctrlKey: true }), none)).toBe(PaneKey.Terminal);
    expect(paneKeyOf(press('v', { ctrlKey: true }), none)).toBe(PaneKey.Terminal);
  });

  it('copies on ctrl+shift+c, which a shifted press spells as an upper case letter', () => {
    expect(paneKeyOf(press('C', { ctrlKey: true, shiftKey: true }), none)).toBe(PaneKey.Copy);
  });

  it('pastes on ctrl+shift+v and on shift+insert', () => {
    expect(paneKeyOf(press('V', { ctrlKey: true, shiftKey: true }), none)).toBe(PaneKey.Paste);
    expect(paneKeyOf(press('Insert', { shiftKey: true }), none)).toBe(PaneKey.Paste);
  });

  it('leaves a key with another modifier to the terminal', () => {
    expect(paneKeyOf(press('Insert'), none)).toBe(PaneKey.Terminal);
    expect(paneKeyOf(press('C', { ctrlKey: true, shiftKey: true, altKey: true }), none)).toBe(PaneKey.Terminal);
    expect(paneKeyOf(press('z', { ctrlKey: true, shiftKey: true }), none)).toBe(PaneKey.Terminal);
    expect(paneKeyOf(press('z', { ctrlKey: true, metaKey: true }), none)).toBe(PaneKey.Terminal);
  });

  it('leaves a plain letter to the terminal', () => {
    expect(paneKeyOf(press('a'), none)).toBe(PaneKey.Terminal);
  });
});
