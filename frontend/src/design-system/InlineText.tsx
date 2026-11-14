import { runs } from './inlineRuns';
import { inlineOf } from './markdown';
import styles from './InlineText.module.css';

/**
 * One text's inline markdown, set in the type around it: code in code type, strong and emphasis; a link reads as its words, since
 * nothing here follows it. For a title or a comment, where a document's blocks and reading measure do not belong.
 */
// deferred: a fenced block reads as one run of code, its language word included, and a link cannot be followed — set comments as documents when one needs either.
export const InlineText = ({ source }: { source: string }) => <span className={styles.inline}>{runs(inlineOf(source), undefined)}</span>;

