import { useEffect, useRef } from 'react';
import { Lamp, type LampColour, LampSize } from './Lamp';
import styles from './TicketRow.module.css';

interface Props {
  id: string;
  title: string;
  selected: boolean;
  done: boolean;
  lamp: LampColour | undefined;
  onSelect: () => void;
}

/**
 * One ticket: its lamp, its number, its title. The selected row is a light bar; a press on it again opens it. A selected row
 * scrolls into its pane's view, since the done rows come last and a long board puts them below the fold.
 */
export const TicketRow = ({ id, title, selected, done, lamp, onSelect }: Props) => {
  const row = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (selected) {
      row.current?.scrollIntoView({ block: 'nearest' });
    }
  }, [selected]);
  const classes = [styles.row, selected ? styles.selected : '', done ? styles.done : ''].join(' ');
  return (
    <li>
      <button ref={row} type="button" className={classes} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        {lamp === undefined ? <span className={styles.noLamp} /> : <Lamp colour={lamp} size={LampSize.Marker} />}
        <span className={styles.id}>{id}</span>
        <span className={styles.title}>{title}</span>
      </button>
    </li>
  );
};
