import { useTranslation } from 'react-i18next';
import styles from './CiGlyph.module.css';

/** A pull request's CI result: its worst check's outcome. */
export enum Ci {
  Failed = 'failed',
  Pending = 'pending',
  Passed = 'passed',
}

const GLYPH: Record<Ci, string> = { failed: '✗', pending: '◌', passed: '✓' };
const NONE_GLYPH = '–';
const MEANING = {
  failed: 'ci.failed',
  pending: 'ci.pending',
  passed: 'ci.passed',
} as const satisfies Record<Ci, string>;

interface Props {
  ci: Ci | undefined;
  /** The result in words, set beside the glyph, in its colour; for a pull request with no result it is the dim reason, after a dash. */
  word?: string;
}

/** The glyph of a CI result in its colour; a blank cell of the same width where there is none, so the rows keep their columns. */
export const CiGlyph = ({ ci, word }: Props) => {
  const { t } = useTranslation('designSystem');
  if (ci === undefined) {
    return word === undefined ? (
      <span className={styles.glyph} />
    ) : (
      <span className={[styles.cell, styles.none].join(' ')}>
        <span className={styles.glyph}>{NONE_GLYPH}</span>
        {word}
      </span>
    );
  }
  const glyph = (
    <span className={[styles.glyph, styles[ci]].join(' ')} role="img" aria-label={t(MEANING[ci])}>
      {GLYPH[ci]}
    </span>
  );
  return word === undefined ? (
    glyph
  ) : (
    <span className={[styles.cell, styles[ci]].join(' ')}>
      {glyph}
      {word}
    </span>
  );
};
