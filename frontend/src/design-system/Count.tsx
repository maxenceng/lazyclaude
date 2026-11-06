import { Lamp, type LampColour, LampSize } from './Lamp';
import styles from './Count.module.css';

/** How many sessions are in one state: the lamp's marker and a number. */
export const Count = ({ colour, count }: { colour: LampColour; count: number }) => {
  return (
    <span className={styles.count}>
      <Lamp colour={colour} size={LampSize.Marker} /> {count}
    </span>
  );
};
