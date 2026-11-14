export enum CaptureStateKind {
  Closed = 'closed',
  Composing = 'composing',
  Blank = 'blank',
  Starting = 'starting',
  Refused = 'refused',
}

/** No popup. */
export class ClosedCapture {
  readonly kind = CaptureStateKind.Closed;

  isOpen(): boolean {
    return false;
  }
}

/** The popup is open and the description is being typed. */
export class ComposingCapture {
  readonly kind = CaptureStateKind.Composing;

  constructor(readonly description: string) {}

  isOpen(): boolean {
    return true;
  }
}

/** Enter was pressed on a blank description: nothing was sent, and the popup says a description is needed. */
export class BlankCapture {
  readonly kind = CaptureStateKind.Blank;

  constructor(readonly description: string) {}

  isOpen(): boolean {
    return true;
  }
}

/** The description was sent; the popup holds still until the launch answers. */
export class StartingCapture {
  readonly kind = CaptureStateKind.Starting;

  constructor(readonly description: string) {}

  isOpen(): boolean {
    return true;
  }
}

/** The session did not start, or the call failed: the popup keeps the description and says why. */
export class RefusedCapture {
  readonly kind = CaptureStateKind.Refused;

  constructor(
    readonly description: string,
    readonly reason: string,
  ) {}

  isOpen(): boolean {
    return true;
  }
}

export type CaptureState = ClosedCapture | ComposingCapture | BlankCapture | StartingCapture | RefusedCapture;
