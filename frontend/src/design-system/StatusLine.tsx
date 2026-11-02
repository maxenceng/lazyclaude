import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './StatusLine.module.css';

interface Props {
  tiles: ReactNode;
  counts: ReactNode;
  project: string;
}

/** The andon board across the top: a lamp tile per live session, folded to marker and count when narrow. */
export const StatusLine = ({ tiles, counts, project }: Props) => {
  const { t } = useTranslation('designSystem');
  return (
    <header className={styles.line}>
      <div className={styles.tiles} role="group" aria-label={t('statusLine.tiles')}>
        {tiles}
      </div>
      <div className={styles.counts} aria-label={t('statusLine.counts')}>
        {counts}
      </div>
      <div className={styles.project}>{project}</div>
    </header>
  );
};
