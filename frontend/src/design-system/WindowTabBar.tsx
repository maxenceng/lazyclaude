import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './WindowTabBar.module.css';

export interface WindowTabEntry {
  id: string;
  label: string;
  /** What the tab reports of its own screen, beside its name. */
  finding?: ReactNode;
}

interface Props {
  tabs: readonly WindowTabEntry[];
  selected: string;
  onSelect: (id: string) => void;
}

/** What the window shows, chosen from the left of the status line: the selected tab on the terminal ground like a dock tab, the rest dim. */
export const WindowTabBar = ({ tabs, selected, onSelect }: Props) => {
  const { t } = useTranslation('designSystem');
  return (
    <div className={styles.tabs} role="tablist" aria-label={t('windowTabs.label')}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={tab.id === selected}
          className={tab.id === selected ? `${styles.tab} ${styles.selected}` : styles.tab}
          onClick={() => onSelect(tab.id)}
        >
          {tab.label}
          {tab.finding === undefined ? null : <span className={styles.finding}>{tab.finding}</span>}
        </button>
      ))}
    </div>
  );
};
