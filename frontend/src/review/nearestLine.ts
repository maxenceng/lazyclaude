export interface LineSpan {
  line: number;
  top: number;
  bottom: number;
}

const distance = (span: LineSpan, y: number): number => (y < span.top ? span.top - y : y > span.bottom ? y - span.bottom : 0);

/** The diff line whose rows are nearest to height `y`: the one it falls in, else the closest above or below. */
export const nearestLine = (spans: readonly LineSpan[], y: number): number | undefined =>
  spans.reduce<LineSpan | undefined>((best, span) => (best === undefined || distance(span, y) < distance(best, y) ? span : best), undefined)?.line;
