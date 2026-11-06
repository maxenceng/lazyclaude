import type { BoardBoardView } from '../protocol';

export enum BoardStateKind {
  Loading = 'loading',
  Empty = 'empty',
  Unavailable = 'unavailable',
  Fault = 'fault',
  Populated = 'populated',
}

export enum UnavailableCause {
  NoProject = 'noProject',
  UnavailableBoard = 'unavailableBoard',
}

/** `board/watch` has not answered yet. */
export class LoadingBoard {
  readonly kind = BoardStateKind.Loading;

  shown(): BoardBoardView | undefined {
    return undefined;
  }
}

/** The board was read and holds no ticket, readable or not. */
export class EmptyBoard {
  readonly kind = BoardStateKind.Empty;

  shown(): BoardBoardView | undefined {
    return undefined;
  }
}

/** The sidecar answered that there is no board to read here, and why. */
export class UnavailableBoard {
  readonly kind = BoardStateKind.Unavailable;

  constructor(
    readonly cause: UnavailableCause,
    readonly reason: string,
  ) {}

  shown(): BoardBoardView | undefined {
    return undefined;
  }
}

/** The read itself failed: a JSON-RPC error, or a sidecar that is not there. */
export class FaultedBoard {
  readonly kind = BoardStateKind.Fault;

  constructor(readonly message: string) {}

  shown(): BoardBoardView | undefined {
    return undefined;
  }
}

/** The board with at least one ticket on it. */
export class PopulatedBoard {
  readonly kind = BoardStateKind.Populated;

  constructor(readonly view: BoardBoardView) {}

  shown(): BoardBoardView | undefined {
    return this.view;
  }
}

export type BoardState = LoadingBoard | EmptyBoard | UnavailableBoard | FaultedBoard | PopulatedBoard;
