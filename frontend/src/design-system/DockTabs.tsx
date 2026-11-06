import { useTranslation } from 'react-i18next';
import { Lamp, type LampColour, LampSize } from './Lamp';
import styles from './DockTabs.module.css';

export interface Tab {
  id: string;
  label: string;
  lamp: LampColour;
}

interface Props {
  tabs: readonly Tab[];
  selected: string | undefined;
  onSelect: (id: string) => void;
}

/** One tab per hosted session: its lamp, the same as beside its ticket and on the andon board, and its name. */
export const DockTabs = ({ tabs, selected, onSelect }: Props) => {
  const { t } = useTranslation('designSystem');
  return (
    <div className={styles.tabs} role="tablist" aria-label={t('dockTabs.label')}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={tab.id === selected}
          className={tab.id === selected ? `${styles.tab} ${styles.selected}` : styles.tab}
          onClick={() => onSelect(tab.id)}
        >
          <Lamp colour={tab.lamp} size={LampSize.Marker} />
          {tab.label}
        </button>
      ))}
    </div>
  );
};
