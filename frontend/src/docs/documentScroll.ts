/** How far a key scrolls the shown document. */
export enum ScrollStep {
  LineDown = 'lineDown',
  LineUp = 'lineUp',
  HalfPageDown = 'halfPageDown',
  HalfPageUp = 'halfPageUp',
}

/** One scroll the shown document is asked for: `at` tells two equal steps apart, so a repeated key scrolls again. */
export class ScrollRequest {
  constructor(
    readonly step: ScrollStep | undefined,
    readonly at: number,
  ) {}

  static none(): ScrollRequest {
    return new ScrollRequest(undefined, 0);
  }

  next(step: ScrollStep): ScrollRequest {
    return new ScrollRequest(step, this.at + 1);
  }
}
