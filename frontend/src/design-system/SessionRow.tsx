import { useEffect, useRef } from 'react';
import { Lamp, LampColour, LampSize } from './Lamp';
import styles from './SessionRow.module.css';

/** What colours a worded pill: the lamp's own, which means a session's state, or the neutral clean of a thing that is simply in order. */
export enum PillInk {
  Lamp = 'lamp',
  Clean = 'clean',
}

interface Props {
  label: string;
  /** The session's state in words, which makes the row a tall one with a pill in place of the lamp marker. */
  state?: string | undefined;
  /** The ink of the state's pill; the lamp's unless given. */
  pill?: PillInk;
  /** What a press on the row does, with the key that does it: the row is the one control, and says both. */
  action?: { label: string; shortcut: string } | undefined;
  selected: boolean;
  lamp: LampColour | undefined;
  /** What a press does; a row given none is plain text, not a button. */
  onSelect?: (() => void) | undefined;
  /** The pointer moving onto the row. A row given it follows the pointer with the cursor, as in a native menu, so it draws no hover ground of its own. */
  onPoint?: (() => void) | undefined;
}

const StatePill = ({ state, pill, lamp }: { state: string; pill: PillInk; lamp: LampColour | undefined }) =>
  pill === PillInk.Clean ? (
    <span className={styles.cleanPill} role="img" aria-label={state}>
      {state}
    </span>
  ) : (
    <Lamp colour={lamp ?? LampColour.Off} size={LampSize.Pill} words={state} />
  );

/**
 * One line of a ticket's sessions, or one verb to start it with: its lamp, its words, and what a press on it does. A selected row
 * scrolls into its pane's view, since a long ticket puts the sessions and the verb picker below the fold.
 */
export const SessionRow = ({ label, state, pill = PillInk.Lamp, action, selected, lamp, onSelect, onPoint }: Props) => {
  const row = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (selected) {
      row.current?.scrollIntoView({ block: 'nearest' });
    }
  }, [selected]);
  const className = [styles.row, selected ? styles.selected : '', state === undefined ? '' : styles.tall, onPoint === undefined ? '' : styles.followsPointer].join(' ');
  const content = (
    <>
      {state !== undefined ? <StatePill state={state} pill={pill} lamp={lamp} /> : lamp === undefined ? <span className={styles.noLamp} /> : <Lamp colour={lamp} size={LampSize.Marker} />}
      <span className={styles.label}>{label}</span>
      {action === undefined ? null : (
        <span className={styles.action}>
          {action.label}
          <kbd className={styles.key}>{action.shortcut}</kbd>
        </span>
      )}
    </>
  );
  return (
    <li data-row="">
      {onSelect === undefined ? (
        <div className={className} data-lamp={lamp}>
          {content}
        </div>
      ) : (
        <button ref={row} type="button" className={className} aria-current={selected ? 'true' : undefined} data-lamp={lamp} onClick={onSelect} onPointerEnter={onPoint}>
          {content}
        </button>
      )}
    </li>
  );
};
