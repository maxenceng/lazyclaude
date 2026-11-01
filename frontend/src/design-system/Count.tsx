import { Lamp, type LampColour } from './Lamp';
import styles from './Count.module.css';

/** How many sessions are in one state: the lamp's marker and a number. */
export function Count({ colour, count }: { colour: LampColour; count: number }) {
  return (
    <span className={styles.count}>
      <Lamp colour={colour} size="marker" /> {count}
    </span>
  );
}
