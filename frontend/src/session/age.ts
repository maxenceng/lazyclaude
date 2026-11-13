export enum AgeUnit {
  Now = 'now',
  Minutes = 'minutes',
  Hours = 'hours',
  Days = 'days',
}

export interface Age {
  unit: AgeUnit;
  count: number;
}

const MINUTE_MS = 60_000;
const MINUTES_PER_HOUR = 60;
const HOURS_PER_DAY = 24;

/** How long ago `since` was, as elapsed time in its largest whole unit, so a day across a clock change is still 24 hours; none when it cannot be read. */
export const ageOf = (since: string, nowMs: number): Age | undefined => {
  const then = Date.parse(since);
  if (Number.isNaN(then)) {
    return undefined;
  }
  const minutes = Math.floor((nowMs - then) / MINUTE_MS);
  const hours = Math.floor(minutes / MINUTES_PER_HOUR);
  const days = Math.floor(hours / HOURS_PER_DAY);
  if (days >= 1) {
    return { unit: AgeUnit.Days, count: days };
  }
  if (hours >= 1) {
    return { unit: AgeUnit.Hours, count: hours };
  }
  return minutes >= 1 ? { unit: AgeUnit.Minutes, count: minutes } : { unit: AgeUnit.Now, count: 0 };
};
