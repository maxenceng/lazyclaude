import { describe, expect, it } from 'vitest';
import { hostedOf } from './quitting';
import { hostedFixture, sessionsFixture } from './session/fixtures';

describe('the hosted sessions a quit asks about', () => {
  it('counts every session the sidecar hosts, idle or not', () => {
    const sessions = { ...sessionsFixture([]), hosted: [hostedFixture('h1'), hostedFixture('h2')] };
    expect(hostedOf(sessions, 0)).toBe(2);
  });

  it('counts the window\'s own panes while the session watch has not answered, so the quit still asks', () => {
    expect(hostedOf(undefined, 2)).toBe(2);
  });
});
