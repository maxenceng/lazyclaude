export enum CaptureStateKind {
  Closed = 'closed',
  Composing = 'composing',
  Creating = 'creating',
  Refused = 'refused',
}

/** No popup. */
export class ClosedCapture {
  readonly kind = CaptureStateKind.Closed;

  isOpen(): boolean {
    return false;
  }
}

/** The popup is open and the title is being typed. */
export class ComposingCapture {
  readonly kind = CaptureStateKind.Composing;

  constructor(readonly title: string) {}

  isOpen(): boolean {
    return true;
  }
}

/** The title was sent; the popup holds still until GitHub answers. */
export class CreatingCapture {
  readonly kind = CaptureStateKind.Creating;

  constructor(readonly title: string) {}

  isOpen(): boolean {
    return true;
  }
}

/** The create was refused, or the call failed: the popup keeps the title and says why. */
export class RefusedCapture {
  readonly kind = CaptureStateKind.Refused;

  constructor(
    readonly title: string,
    readonly reason: string,
  ) {}

  isOpen(): boolean {
    return true;
  }
}

export type CaptureState = ClosedCapture | ComposingCapture | CreatingCapture | RefusedCapture;
