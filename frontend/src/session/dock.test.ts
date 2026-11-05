import { describe, expect, it } from 'vitest';
import { Dock } from './dock';

describe('the dock of hosted sessions', () => {
  it('has no tab and selects none while empty', () => {
    expect(new Dock().tabs).toEqual([]);
    expect(new Dock().selectedTab()).toBeUndefined();
  });

  it('selects the first tab until another is chosen', () => {
    const dock = new Dock().listing(['a', 'b']);
    expect(dock.selectedTab()).toBe('a');
    expect(dock.select('b').selectedTab()).toBe('b');
  });

  it('selects a session just started, before the sessions that list it arrive', () => {
    const dock = new Dock().listing(['a']).opened('b');
    expect(dock.tabs).toEqual(['a', 'b']);
    expect(dock.selectedTab()).toBe('b');
    expect(dock.listing(['a']).tabs).toEqual(['a', 'b']);
  });

  it('drops a tab that was listed and no longer is', () => {
    const dock = new Dock().listing(['a', 'b']).listing(['a']);
    expect(dock.tabs).toEqual(['a']);
  });

  it('keeps a session opened twice once', () => {
    expect(new Dock().opened('a').opened('a').tabs).toEqual(['a']);
  });

  it('lets the next tab take over when the selected one closes, else the last', () => {
    const three = new Dock().listing(['a', 'b', 'c']);
    expect(three.select('b').closed('b').selectedTab()).toBe('c');
    expect(three.select('c').closed('c').selectedTab()).toBe('b');
  });

  it('keeps the selection when another tab closes', () => {
    expect(new Dock().listing(['a', 'b', 'c']).select('c').closed('a').selectedTab()).toBe('c');
  });

  it('selects the previous tab and says when there is none', () => {
    const dock = new Dock().listing(['a', 'b']).select('b');
    expect(dock.previous()?.selectedTab()).toBe('a');
    expect(dock.previous()?.previous()).toBeUndefined();
  });

  it('selects the next tab and stays on the last', () => {
    const dock = new Dock().listing(['a', 'b']);
    expect(dock.next().selectedTab()).toBe('b');
    expect(dock.next().next().selectedTab()).toBe('b');
  });

  it('closes a tab the sessions still list, since its claude exited', () => {
    expect(new Dock().listing(['a', 'b']).closed('a').listing(['a', 'b']).tabs).toEqual(['b']);
  });
});
