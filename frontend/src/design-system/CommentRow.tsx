import { COMMENT_MARKS, CommentKind } from './comment';
import styles from './CommentRow.module.css';

interface Props {
  kind: CommentKind;
  open: boolean;
  where: string;
  dim: boolean;
  selected: boolean;
  onSelect: () => void;
}

/** One entry of Comments: a thread's mark while it still waits for an answer, or a pending comment's, and where it hangs. The selected row is a light bar; a press on it again goes to its line. */
export const CommentRow = ({ kind, open, where, dim, selected, onSelect }: Props) => {
  const mark = kind === CommentKind.Pending || open ? COMMENT_MARKS[kind] : '';
  const classes = [styles.row, selected ? styles.selected : '', dim ? styles.dim : ''].join(' ');
  return (
    <li>
      <button type="button" className={classes} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        <span className={[styles.mark, styles[kind]].join(' ')}>{mark}</span>
        <span className={styles.where}>{where}</span>
      </button>
    </li>
  );
};
