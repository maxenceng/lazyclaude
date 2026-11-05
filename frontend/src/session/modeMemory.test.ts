import { beforeEach, describe, expect, it } from 'vitest';
import { KeptModes } from './privateModes';
import { createModeMemory } from './modeMemory';

const kept = () => {
  const modes = new KeptModes();
  modes.set(2004, true);
  return modes;
};

describe('the modes kept per session in sessionStorage', () => {
  beforeEach(() => sessionStorage.clear());

  it('has nothing for a session it never saw', () => {
    expect(createModeMemory(sessionStorage).recall('s1')).toBeUndefined();
  });

  it('recalls what was remembered, for a page loaded afterwards', () => {
    createModeMemory(sessionStorage).remember('s1', kept());
    expect(createModeMemory(sessionStorage).recall('s1')?.restoration()).toBe('\x1b[?2004h');
  });

  it('keeps one entry per session', () => {
    const memory = createModeMemory(sessionStorage);
    memory.remember('s1', kept());
    expect(memory.recall('s2')).toBeUndefined();
  });

  it('forgets a session', () => {
    const memory = createModeMemory(sessionStorage);
    memory.remember('s1', kept());
    memory.forget('s1');
    expect(memory.recall('s1')).toBeUndefined();
  });

  it('treats an entry it cannot read as none', () => {
    sessionStorage.setItem('lazyclaude.modes.s1', '{not json');
    expect(createModeMemory(sessionStorage).recall('s1')).toBeUndefined();
    sessionStorage.setItem('lazyclaude.modes.s1', '{"modes":"x"}');
    expect(createModeMemory(sessionStorage).recall('s1')).toBeUndefined();
  });
});
