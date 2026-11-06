import type { ReviewDiffFileView } from '../protocol';

export enum DiffStateKind {
  Loading = 'loading',
  Unavailable = 'unavailable',
  Fault = 'fault',
  Empty = 'empty',
  Populated = 'populated',
}

/** The diff has not been answered yet. */
export class LoadingDiff {
  readonly kind = DiffStateKind.Loading;
}

/** The sidecar answered that the pull request or its diff cannot be read, and why. */
export class UnavailableDiff {
  readonly kind = DiffStateKind.Unavailable;

  constructor(
    readonly reason: string,
    readonly tooLarge: boolean,
  ) {}
}

/** The read itself failed: a JSON-RPC error, or a sidecar that is not there. */
export class FaultedDiff {
  readonly kind = DiffStateKind.Fault;

  constructor(readonly message: string) {}
}

/** The pull request changes no file. */
export class EmptyDiff {
  readonly kind = DiffStateKind.Empty;
}

/** The diff with at least one file. */
export class PopulatedDiff {
  readonly kind = DiffStateKind.Populated;

  constructor(readonly files: ReviewDiffFileView[]) {}
}

export type DiffState = LoadingDiff | UnavailableDiff | FaultedDiff | EmptyDiff | PopulatedDiff;
