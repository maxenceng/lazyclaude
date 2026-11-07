import { COMMENT_MARKS, CommentKind } from './comment';
import styles from './CommentRow.module.css';

interface Props {
  kind: CommentKind;
  open: boolean;
  where: string;
  side: string;
  preview: string;
  dim: boolean;
  selected: boolean;
  onSelect: () => void;
}

/** One entry of Comments, in two lines that never wrap: the mark and where it hangs with who spoke last on the right, and under it, dimmer, the first line of what was said. The selected row is a light bar; a press on it goes to its line, and a second opens it. */
export const CommentRow = ({ kind, open, where, side, preview, dim, selected, onSelect }: Props) => {
  const mark = kind === CommentKind.Pending || open ? COMMENT_MARKS[kind] : '';
  const classes = [styles.row, selected ? styles.selected : '', dim ? styles.dim : ''].join(' ');
  return (
    <li>
      <button type="button" className={classes} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        <span className={styles.head}>
          <span className={[styles.mark, styles[kind]].join(' ')}>{mark}</span>
          <span className={styles.where}>{where}</span>
          <span className={styles.side}>{side}</span>
        </span>
        <span className={styles.preview}>{preview}</span>
      </button>
    </li>
  );
};
