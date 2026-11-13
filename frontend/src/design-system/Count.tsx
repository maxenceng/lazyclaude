import { useTranslation } from 'react-i18next';
import { SHORT_MEANING, type LampColour } from './Lamp';
import styles from './Count.module.css';

/** How many sessions are in one state: the number, then what it counts, in the lamp's colour, dim while there are none. */
export const Count = ({ colour, count }: { colour: LampColour; count: number }) => {
  const { t } = useTranslation('designSystem');
  return (
    <span className={styles.count} data-lamp={count === 0 ? undefined : colour}>
      <b>{count}</b> <small className={styles.word}>{t(SHORT_MEANING[colour])}</small>
    </span>
  );
};
