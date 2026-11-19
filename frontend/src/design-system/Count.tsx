import { useTranslation } from 'react-i18next';
import { SHORT_MEANING, type LampColour } from './Lamp';
import styles from './Count.module.css';

interface Props {
  colour: LampColour;
  count: number;
  /** Given, the count is a button: the status line's count of one state. */
  onClick?: () => void;
  /** A button with nowhere to go: its number dims and its colour stays. */
  disabled?: boolean;
}

/** How many sessions are in one state: the number, then what it counts, in the lamp's colour, dim while there are none; a button where it is given a click. */
export const Count = ({ colour, count, onClick, disabled = false }: Props) => {
  const { t } = useTranslation('designSystem');
  const inside = (
    <>
      <b>{count}</b> <small className={styles.word}>{t(SHORT_MEANING[colour])}</small>
    </>
  );
  if (onClick !== undefined) {
    return (
      <button
        type="button"
        className={`${styles.count} ${styles.button}`}
        data-lamp={colour}
        disabled={disabled}
        onMouseDown={(event) => event.preventDefault()}
        onClick={onClick}
      >
        {inside}
      </button>
    );
  }
  return (
    <span className={styles.count} data-lamp={count === 0 ? undefined : colour}>
      {inside}
    </span>
  );
};
