import styles from './DiffLine.module.css';

interface Props {
  change: 'added' | 'removed' | 'context';
  oldNumber: number | undefined;
  newNumber: number | undefined;
  text: string;
  index: number;
  cursor: boolean;
  inRange: boolean;
  onPress: (index: number) => void;
}

const SIGNS = { added: '+', removed: '-', context: ' ' } as const;

/**
 * One line of a diff between its two number gutters: the sign carries what changed, the pane's muted tint only backs it. The cursor's
 * line is a light bar and the range being marked a faint band. A left press on it reports its index; where a drag ends is the screen's to read.
 */
export const DiffLine = ({ change, oldNumber, newNumber, text, index, cursor, inRange, onPress }: Props) => {
  const classes = [styles.line, styles[change], inRange ? styles.range : '', cursor ? styles.cursor : ''].join(' ');
  return (
    <li>
      <button
        type="button"
        className={classes}
        data-line={index}
        aria-current={cursor ? 'true' : undefined}
        onMouseDown={(event) => {
          if (event.button === 0) {
            onPress(index);
          }
        }}
      >
        <span className={styles.number}>{oldNumber}</span>
        <span className={styles.number}>{newNumber}</span>
        <span className={styles.sign}>{SIGNS[change]}</span>
        <span className={styles.text}>{text}</span>
      </button>
    </li>
  );
};
