import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './WindowTabBar.module.css';
import type { StatusColour } from './StatusHeading';

export interface WindowTabEntry {
  id: string;
  label: string;
  /** What the tab reports of its own screen, beside its name. */
  finding?: ReactNode;
  /** The status whose colour tints the finding, when what it counts is tickets in that status. */
  findingColour?: StatusColour;
}

interface Props {
  tabs: readonly WindowTabEntry[];
  selected: string;
  onSelect: (id: string) => void;
}

/** What the window shows, chosen from the left of the status line: one group of tabs, the selected one lifted, the rest dim. */
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
          {tab.finding === undefined ? null : <span className={styles.finding} data-status={tab.findingColour}>
              {tab.finding}
            </span>}
        </button>
      ))}
    </div>
  );
};
