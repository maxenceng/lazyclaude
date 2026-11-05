import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './StatusLine.module.css';

interface Props {
  tiles: ReactNode;
  counts: ReactNode;
  project: string;
  warning?: { text: string; count: number };
}

/** The andon board across the top: a lamp tile per live session, folded to marker and count when narrow; a warning, when one is raised, before the project. */
export const StatusLine = ({ tiles, counts, project, warning }: Props) => {
  const { t } = useTranslation('designSystem');
  return (
    <header className={styles.line}>
      <div className={styles.tiles} role="group" aria-label={t('statusLine.tiles')}>
        {tiles}
      </div>
      <div className={styles.counts} aria-label={t('statusLine.counts')}>
        {counts}
      </div>
      {warning === undefined ? null : (
        <div className={styles.warning} role="status">
          {warning.count > 1 ? t('statusLine.warningCounted', { count: warning.count, text: warning.text }) : t('statusLine.warning', { text: warning.text })}
        </div>
      )}
      <div className={styles.project}>{project}</div>
    </header>
  );
};
