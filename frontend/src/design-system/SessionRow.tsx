import { useEffect, useRef } from 'react';
import { Lamp, type LampColour, LampSize } from './Lamp';
import styles from './SessionRow.module.css';

interface Props {
  label: string;
  /** What a press on the row does, with the key that does it: the row is the one control, and says both. */
  action?: { label: string; shortcut: string } | undefined;
  selected: boolean;
  lamp: LampColour | undefined;
  onSelect: () => void;
}

/**
 * One line of a ticket's sessions, or one verb to start it with: its lamp, its words, and what a press on it does. A selected row
 * scrolls into its pane's view, since a long ticket puts the sessions and the verb picker below the fold.
 */
export const SessionRow = ({ label, action, selected, lamp, onSelect }: Props) => {
  const row = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (selected) {
      row.current?.scrollIntoView({ block: 'nearest' });
    }
  }, [selected]);
  return (
    <li data-row="">
      <button ref={row} type="button" className={selected ? `${styles.row} ${styles.selected}` : styles.row} aria-current={selected ? 'true' : undefined} data-lamp={lamp} onClick={onSelect}>
        {lamp === undefined ? <span className={styles.noLamp} /> : <Lamp colour={lamp} size={LampSize.Marker} />}
        <span className={styles.label}>{label}</span>
        {action === undefined ? null : (
          <span className={styles.action}>
            {action.label}
            <kbd className={styles.key}>{action.shortcut}</kbd>
          </span>
        )}
      </button>
    </li>
  );
};
