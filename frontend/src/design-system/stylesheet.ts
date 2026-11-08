import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const WHITESPACE_RUN = /\s+/g;
const DESIGN_SYSTEM = join(process.cwd(), 'src', 'design-system');

/** The text of a design-system stylesheet, for a test to pin a rule jsdom cannot lay out. */
export const stylesheet = (name: string): string => readFileSync(join(DESIGN_SYSTEM, name), 'utf8');

/** The body of the first rule whose selector is exactly `selector`, whitespace collapsed. */
export const ruleOf = (sheet: string, selector: string): string => {
  const start = sheet.indexOf(`${selector} {`);
  if (start < 0) {
    throw new Error(`no rule for ${selector}`);
  }
  const open = sheet.indexOf('{', start);
  return sheet.slice(open + 1, sheet.indexOf('}', open)).replace(WHITESPACE_RUN, ' ').trim();
};
