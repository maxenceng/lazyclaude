import { useTranslation } from 'react-i18next';
import styles from './PanelTabs.module.css';

export interface PanelTab {
  id: string;
  label: string;
  count: number;
}

export interface PanelTabsProps {
  tabs: readonly PanelTab[];
  selected: string;
  onSelect: (id: string) => void;
}

/** The faces of a column, one shown at a time, named in its title row each with its count: the shown one lifted, the rest dim. */
export const PanelTabs = ({ tabs, selected, onSelect }: PanelTabsProps) => {
  const { t } = useTranslation('designSystem');
  return (
    <div className={styles.tabs} role="tablist" aria-label={t('panelTabs.label')}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={tab.id === selected}
          className={tab.id === selected ? `${styles.tab} ${styles.selected}` : styles.tab}
          onClick={() => onSelect(tab.id)}
        >
          {tab.label}{' '}
          <small className={styles.count}>{tab.count}</small>
        </button>
      ))}
    </div>
  );
};
