import type { MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { COMMENT_MARKS, CommentKind } from './comment';
import { LEFT_BUTTON } from './mouse';
import styles from './DiffLine.module.css';

export enum LineChange {
  Added = 'added',
  Removed = 'removed',
  Context = 'context',
}

interface Props {
  change: LineChange;
  /** The one number the line wears: its new number, its old one on a removed line. */
  number: number;
  text: string;
  index: number;
  cursor: boolean;
  inRange: boolean;
  /** What hangs from this line, marked in its gutter by a glyph and an edge: the thread, or your pending comment when only that is there. */
  hangs: CommentKind | undefined;
  onPress: (index: number) => void;
  /** A press on the gutter's number: the comment box opens on the line. */
  onComment: (index: number) => void;
}

const SIGNS: Readonly<Record<LineChange, string>> = { added: '+', removed: '-', context: ' ' };

/** A press on the gutter belongs to the gutter: it neither moves the cursor by itself nor starts a drag. */
const ownPress = (event: MouseEvent) => event.stopPropagation();

/**
 * One line of a diff behind one gutter of numbers: the sign carries what changed, the pane's muted tint only backs it. A long line wraps
 * under a hanging indent instead of being cut. The cursor's line is lifted and edged, never the focused title row's inverse, and the range being
 * marked a faint band. A left press on it reports its index; where a drag ends is the screen's to read, and the gutter's number is a button
 * that asks for the comment box.
 */
export const DiffLine = ({ change, number, text, index, cursor, inRange, hangs, onPress, onComment }: Props) => {
  const { t } = useTranslation('designSystem');
  const classes = [styles.line, styles[change], inRange ? styles.range : '', cursor ? styles.cursor : '', hangs === undefined ? '' : styles[hangs]].join(' ');
  return (
    <li>
      <div
        className={classes}
        role="presentation"
        data-line={index}
        aria-current={cursor ? 'true' : undefined}
        onMouseDown={(event) => {
          if (event.button === LEFT_BUTTON) {
            onPress(index);
          }
        }}
      >
        <span className={styles.mark} role={hangs === undefined ? undefined : 'img'} aria-label={hangs === undefined ? undefined : t(`diffLine.hangs.${hangs}`)}>
          {hangs === undefined ? '' : COMMENT_MARKS[hangs]}
        </span>
        <button type="button" className={styles.number} aria-label={t('diffLine.comment', { number })} onMouseDown={ownPress} onClick={() => onComment(index)}>
          {number}
        </button>
        <span className={styles.sign}>{SIGNS[change]}</span>
        <span className={styles.text}>{text}</span>
      </div>
    </li>
  );
};
