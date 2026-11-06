import { useTranslation } from 'react-i18next';
import styles from './CiGlyph.module.css';

/** A pull request's CI result: its worst check's outcome. */
export enum Ci {
  Failed = 'failed',
  Pending = 'pending',
  Passed = 'passed',
}

const GLYPH: Record<Ci, string> = { failed: '✗', pending: '◌', passed: '✓' };
const MEANING = {
  failed: 'ci.failed',
  pending: 'ci.pending',
  passed: 'ci.passed',
} as const satisfies Record<Ci, string>;

interface Props {
  ci: Ci | undefined;
  /** The glyph stands on a selected row's light bar, so it takes the darker colour. */
  onLight?: boolean;
}

/** The glyph of a CI result in its colour; a blank cell of the same width where there is none, so the rows keep their columns. */
export const CiGlyph = ({ ci, onLight = false }: Props) => {
  const { t } = useTranslation('designSystem');
  if (ci === undefined) {
    return <span className={styles.glyph} />;
  }
  return (
    <span className={[styles.glyph, styles[ci], onLight ? styles.onLight : ''].join(' ')} role="img" aria-label={t(MEANING[ci])}>
      {GLYPH[ci]}
    </span>
  );
};
