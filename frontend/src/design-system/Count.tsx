import { useTranslation } from 'react-i18next';
import type { LampColour } from './Lamp';
import styles from './Count.module.css';

const WORDS = {
  permission: 'count.permission',
  input: 'count.input',
  turn: 'count.turn',
  off: 'count.off',
} as const satisfies Record<LampColour, string>;

/** How many sessions are in one state: the number, then what it counts, in the lamp's colour, dim while there are none. */
export const Count = ({ colour, count }: { colour: LampColour; count: number }) => {
  const { t } = useTranslation('designSystem');
  return (
    <span className={styles.count} data-lamp={count === 0 ? undefined : colour}>
      <b>{count}</b> <small className={styles.word}>{t(WORDS[colour])}</small>
    </span>
  );
};
