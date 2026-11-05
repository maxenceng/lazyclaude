import { Lamp, type LampColour } from './Lamp';
import styles from './SessionRow.module.css';

interface Props {
  label: string;
  detail: string;
  selected: boolean;
  lamp: LampColour | undefined;
  onSelect: () => void;
}

/** One line of a ticket's sessions, or one verb to start it with: its lamp, its words, and what it is. */
export const SessionRow = ({ label, detail, selected, lamp, onSelect }: Props) => {
  return (
    <li>
      <button type="button" className={selected ? `${styles.row} ${styles.selected}` : styles.row} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        {lamp === undefined ? <span className={styles.noLamp} /> : <Lamp colour={lamp} size="marker" />}
        <span className={styles.label}>{label}</span>
        <span className={styles.detail}>{detail}</span>
      </button>
    </li>
  );
};
