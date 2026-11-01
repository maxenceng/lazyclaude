import type { ReactNode } from 'react';
import styles from './StatusLine.module.css';

interface Props {
  tiles: ReactNode;
  counts: ReactNode;
  project: string;
}

/** The andon board across the top: a lamp tile per live session, folded to marker and count when narrow. */
export function StatusLine({ tiles, counts, project }: Props) {
  return (
    <header className={styles.line}>
      <div className={styles.tiles} role="group" aria-label="live sessions">
        {tiles}
      </div>
      <div className={styles.counts} aria-label="sessions by state">
        {counts}
      </div>
      <div className={styles.project}>{project}</div>
    </header>
  );
}
