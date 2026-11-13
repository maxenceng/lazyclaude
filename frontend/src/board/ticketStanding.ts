/** What the ticket's pull request lookup answered: still reading, could not be read, no open pull request heads its branch, or one does. */
export enum PullRequestRead {
  Reading = 'reading',
  Unavailable = 'unavailable',
  None = 'none',
  Open = 'open',
}

/** What the ticket's worktree lookup answered: still reading, could not be read, none is held, or one is. */
export enum WorktreeRead {
  Reading = 'reading',
  Unavailable = 'unavailable',
  None = 'none',
  Held = 'held',
}

export interface WorktreeStanding {
  read: WorktreeRead;
  /** The worktree's path, given with `Held` and with nothing else. */
  path: string | undefined;
}

export interface PullRequestStanding {
  read: PullRequestRead;
  /** The open pull request's number, given with `Open` and with nothing else. */
  number: number | undefined;
}

/** What a ticket's facts line says beyond the ticket itself, joined by the window from `session` and `review`: where it is worked and whether a pull request is open on it. */
export interface TicketStanding {
  worktree: WorktreeStanding;
  pullRequest: PullRequestStanding;
}
