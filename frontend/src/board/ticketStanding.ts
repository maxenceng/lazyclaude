/** What the ticket's pull request lookup answered: still reading, could not be read, no open pull request heads its branch, or one does. */
export enum PullRequestRead {
  Reading = 'reading',
  Unavailable = 'unavailable',
  None = 'none',
  Open = 'open',
}

export interface PullRequestStanding {
  read: PullRequestRead;
  /** The open pull request's number, given with `Open` and with nothing else. */
  number: number | undefined;
}

/** What a ticket's facts line says beyond the ticket itself, joined by the window from `session` and `review`: whether a pull request is open on it. */
export interface TicketStanding {
  pullRequest: PullRequestStanding;
}
