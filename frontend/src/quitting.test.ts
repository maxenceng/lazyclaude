import { describe, expect, it } from 'vitest';
import { busyOrWaitingOf } from './quitting';
import { hostedFixture, sessionsFixture } from './session/fixtures';

describe('the hosted sessions a quit asks about', () => {
  it('counts the hosted sessions busy or waiting', () => {
    const sessions = { ...sessionsFixture([]), hosted: [hostedFixture('h1', { busyOrWaiting: true }), hostedFixture('h2')] };
    expect(busyOrWaitingOf(sessions, 2)).toBe(1);
  });

  it('counts every hosted pane while the session watch has not answered, so the quit still asks', () => {
    expect(busyOrWaitingOf(undefined, 2)).toBe(2);
  });
});
