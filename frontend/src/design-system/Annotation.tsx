import type { MouseEvent } from 'react';
import { COMMENT_MARKS, CommentKind } from './comment';
import styles from './Annotation.module.css';

interface Comment {
  author: string;
  body: string;
}

export interface AnnotationReply {
  /** The button's text. */
  text: string;
  /** What the button does, for a screen reader, since the text alone does not say which thread. */
  label: string;
  onReply: () => void;
}

interface Props {
  kind: CommentKind;
  heading: string;
  comments: readonly Comment[];
  dim: boolean;
  reply?: AnnotationReply;
}

/** A click keeps the focus where the window's panes put it. */
const keepFocus = (event: MouseEvent) => event.preventDefault();

/** What has been said on a line, set under it: a thread's comments, or your own pending comment. A resolved thread is dimmed. A thread ends with a quiet Reply, lit when hovered; your own comment has none. */
export const Annotation = ({ kind, heading, comments, dim, reply }: Props) => {
  return (
    <li>
      <article className={[styles.annotation, styles[kind], dim ? styles.dim : ''].join(' ')}>
        <p className={styles.heading}>
          <span className={styles.mark}>{COMMENT_MARKS[kind]}</span> {heading}
        </p>
        {comments.map((comment, index) => (
          <p key={index} className={index === 0 ? undefined : styles.reply}>
            <strong className={styles.author}>{comment.author}</strong> {comment.body}
          </p>
        ))}
        {reply === undefined ? null : (
          <button type="button" className={styles.replyAction} aria-label={reply.label} onMouseDown={keepFocus} onClick={reply.onReply}>
            {reply.text}
          </button>
        )}
      </article>
    </li>
  );
};
