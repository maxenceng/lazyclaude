import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './StatusLine.module.css';

interface Props {
  menu: ReactNode;
  tabs: ReactNode;
  tiles: ReactNode;
  counts: ReactNode;
  project: string;
  warning?: { text: string; count: number };
  notice?: string;
}

/**
 * Across the top: the window menu, the window tabs and the project, then the window's notice and the backend's warning, each when
 * there is one; at the right, the sessions counted by state and the andon board, a lamp tile per live session, dropped when narrow.
 */
export const StatusLine = ({ menu, tabs, tiles, counts, project, warning, notice }: Props) => {
  const { t } = useTranslation('designSystem');
  return (
    <header className={styles.line}>
      {menu}
      {tabs}
      <div className={styles.project}>{project}</div>
      {notice === undefined ? null : (
        <div className={styles.notice} role="status">
          {t('statusLine.notice', { text: notice })}
        </div>
      )}
      {warning === undefined ? null : (
        <div className={styles.warning} role="status">
          {warning.count > 1 ? t('statusLine.warningCounted', { count: warning.count, text: warning.text }) : t('statusLine.warning', { text: warning.text })}
        </div>
      )}
      <div className={styles.counts} aria-label={t('statusLine.counts')}>
        {counts}
      </div>
      <span className={styles.divider} />
      <div className={styles.tiles} role="group" aria-label={t('statusLine.tiles')}>
        {tiles}
      </div>
    </header>
  );
};
