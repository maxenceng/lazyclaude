/** What the ticket's pull request lookup answered: still reading, could not be read, no open pull request heads its branch, or one does. */
export enum PullRequestRead {
  Reading = 'reading',
  Unavailable = 'unavailable',
  None = 'none',
  Open = 'open',
}

/** The open pull request a ticket's facts line names, as the window joins it from `review`. */
export interface TicketPullRequest {
  read: PullRequestRead;
  /** The open pull request's number, given with `Open` and with nothing else. */
  number: number | undefined;
}
