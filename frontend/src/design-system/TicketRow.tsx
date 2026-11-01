import { Lamp, type LampColour } from './Lamp';
import styles from './TicketRow.module.css';

interface Props {
  id: string;
  title: string;
  selected: boolean;
  done: boolean;
  lamp: LampColour | undefined;
  onSelect: () => void;
}

/** One ticket: its lamp, its number, its title. The selected row is a light bar; a press on it again opens it. */
export function TicketRow({ id, title, selected, done, lamp, onSelect }: Props) {
  const classes = [styles.row, selected ? styles.selected : '', done ? styles.done : ''].join(' ');
  return (
    <li>
      <button type="button" className={classes} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        {lamp === undefined ? <span className={styles.noLamp} /> : <Lamp colour={lamp} size="marker" />}
        <span className={styles.id}>{id}</span>
        <span className={styles.title}>{title}</span>
      </button>
    </li>
  );
}
