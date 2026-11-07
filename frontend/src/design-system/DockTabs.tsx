import { useTranslation } from 'react-i18next';
import { Lamp, type LampColour, LampSize } from './Lamp';
import { PaneAction, type PaneActionProps } from './PaneAction';
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
  /** A button at the right of the tab row: minimise the dock, or restore it. */
  action?: PaneActionProps;
}

/** One tab per hosted session: its lamp, the same as beside its ticket and on the andon board, and its name. */
export const DockTabs = ({ tabs, selected, onSelect, action }: Props) => {
  const { t } = useTranslation('designSystem');
  return (
    <div className={styles.row}>
      <div className={styles.tabs} role="tablist" aria-label={t('dockTabs.label')}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={tab.id === selected}
          className={tab.id === selected ? `${styles.tab} ${styles.selected}` : styles.tab}
          onMouseDown={(event) => event.preventDefault()}
          onClick={() => onSelect(tab.id)}
        >
          <Lamp colour={tab.lamp} size={LampSize.Marker} />
          {tab.label}
        </button>
      ))}
      </div>
      {action === undefined ? null : <PaneAction {...action} />}
    </div>
  );
};
