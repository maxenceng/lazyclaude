/** How long ago something was, in the largest unit it fills. */
export enum Elapsed {
  Now = 'now',
  Minutes = 'minutes',
  Hours = 'hours',
  Days = 'days',
}

export interface Age {
  unit: Elapsed;
  count: number;
}

const MINUTE_MS = 60_000;
const HOUR_MS = 60 * MINUTE_MS;
const DAY_MS = 24 * HOUR_MS;

/** The time between an instant `review` sent and `now`, as elapsed time and not calendar days, so a clock change does not move it. */
export const ageOf = (openedAt: string, now: Date): Age => {
  const elapsed = now.getTime() - Date.parse(openedAt);
  if (elapsed >= DAY_MS) {
    return { unit: Elapsed.Days, count: Math.floor(elapsed / DAY_MS) };
  }
  if (elapsed >= HOUR_MS) {
    return { unit: Elapsed.Hours, count: Math.floor(elapsed / HOUR_MS) };
  }
  if (elapsed >= MINUTE_MS) {
    return { unit: Elapsed.Minutes, count: Math.floor(elapsed / MINUTE_MS) };
  }
  return { unit: Elapsed.Now, count: 0 };
};
