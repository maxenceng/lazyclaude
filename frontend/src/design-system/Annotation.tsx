import type { MouseEvent } from 'react';
import { Badge, BadgeTone } from './Badge';
import { CommentKind } from './comment';
import styles from './Annotation.module.css';

interface Comment {
  author: string;
  body: string;
}

export interface AnnotationAction {
  /** The button's text. */
  text: string;
  /** What the button does, for a screen reader, since the text alone does not say which comment. */
  label: string;
  /** The key that does the same, in a chip after the text. */
  chip: string;
  onPress: () => void;
}

interface Props {
  kind: CommentKind;
  /** Where the card hangs, in code type: the file and its line. */
  where: string;
  /** The card's state in a word, in a badge beside where it hangs. */
  tag: string;
  comments: readonly Comment[];
  dim: boolean;
  /** What can be done to the card: a thread is replied to, a pending comment edited and deleted. */
  actions: readonly AnnotationAction[];
}

/** A click keeps the focus where the window's panes put it. */
const keepFocus = (event: MouseEvent) => event.preventDefault();

/**
 * What has been said on a line, as one card across the whole diff under it: where it hangs and its state in a word, then each comment with
 * its author, and its actions. An open thread is edged blue; your pending comment is a neutral card edged and badged amber. A resolved
 * thread is dimmed.
 */
export const Annotation = ({ kind, where, tag, comments, dim, actions }: Props) => {
  return (
    <li className={styles.item}>
      <article className={[styles.card, styles[kind], dim ? styles.dim : ''].join(' ')}>
        <header className={styles.head}>
          <span className={styles.where}>{where}</span>
          <Badge tone={kind === CommentKind.Pending ? BadgeTone.Amber : BadgeTone.Blue}>{tag}</Badge>
        </header>
        {comments.map((comment, index) => (
          <div key={index} className={styles.comment}>
            <strong className={styles.author}>{comment.author}</strong>
            <p className={styles.body}>{comment.body}</p>
          </div>
        ))}
        {actions.length === 0 ? null : (
          <div className={styles.actions}>
            {actions.map((action) => (
              <button key={action.text} type="button" className={styles.action} aria-label={action.label} onMouseDown={keepFocus} onClick={action.onPress}>
                {action.text}
                <kbd className={styles.chip}>{action.chip}</kbd>
              </button>
            ))}
          </div>
        )}
      </article>
    </li>
  );
};
