import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Heartbeat } from './heartbeat';

describe('the page\'s heartbeat', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('beats at once and then every interval', () => {
    const beat = vi.fn(() => Promise.resolve());
    new Heartbeat(beat, 1000);
    expect(beat).toHaveBeenCalledTimes(1);
    vi.advanceTimersByTime(3000);
    expect(beat).toHaveBeenCalledTimes(4);
  });

  it('stops beating once stopped', () => {
    const beat = vi.fn(() => Promise.resolve());
    new Heartbeat(beat, 1000).stop();
    vi.advanceTimersByTime(5000);
    expect(beat).toHaveBeenCalledTimes(1);
  });

  it('goes on beating after a beat that failed', async () => {
    const beat = vi.fn().mockRejectedValueOnce(new Error('no window')).mockResolvedValue(undefined);
    new Heartbeat(beat, 1000);
    await vi.advanceTimersByTimeAsync(2000);
    expect(beat).toHaveBeenCalledTimes(3);
  });
});
