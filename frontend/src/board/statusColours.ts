import { StatusColour } from '../design-system';
import type { BoardStatus } from '../protocol';

const COLOUR_OF: Readonly<Record<BoardStatus, StatusColour>> = {
  IN_PROGRESS: StatusColour.InProgress,
  IN_REVIEW: StatusColour.InReview,
  TODO: StatusColour.Todo,
  DRAFT: StatusColour.Draft,
  DONE: StatusColour.Done,
};

/** A status's colour, on its heading and wherever the window counts tickets of it. */
export const statusColourOf = (status: BoardStatus): StatusColour => COLOUR_OF[status];
