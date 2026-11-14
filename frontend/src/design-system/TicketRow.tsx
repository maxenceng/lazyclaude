import { useEffect, useRef } from 'react';
import { Lamp, type LampColour, LampSize } from './Lamp';
import { type TicketTypeTag, TypeTag } from './TypeTag';
import styles from './TicketRow.module.css';

interface Props {
  id: string;
  title: string;
  selected: boolean;
  done: boolean;
  /** The strongest marker of the ticket's sessions, said in words at the row's right end. */
  lamp: LampColour | undefined;
  /** The ticket's type, in its own colour at the row's right end. */
  type?: TicketTypeTag | undefined;
  onSelect: () => void;
}

/**
 * One ticket: its number, its title, its type, and the pill of its sessions' strongest marker. The selected row is a light bar; a press on it again opens it. A selected row
 * scrolls into its pane's view, since the done rows come last and a long board puts them below the fold.
 */
export const TicketRow = ({ id, title, selected, done, lamp, type, onSelect }: Props) => {
  const row = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (selected) {
      row.current?.scrollIntoView({ block: 'nearest' });
    }
  }, [selected]);
  const classes = [styles.row, selected ? styles.selected : '', done ? styles.done : ''].join(' ');
  return (
    <li data-row="">
      <button ref={row} type="button" className={classes} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        <span className={styles.id}>{id}</span>
        <span className={styles.title}>{title}</span>
        {type === undefined ? null : <TypeTag name={type.name} colour={type.colour} />}
        {lamp === undefined ? null : <Lamp colour={lamp} size={LampSize.Pill} />}
      </button>
    </li>
  );
};
