import { COMMENT_MARKS, CommentKind } from './comment';
import styles from './Annotation.module.css';

interface Comment {
  author: string;
  body: string;
}

interface Props {
  kind: CommentKind;
  heading: string;
  comments: readonly Comment[];
  dim: boolean;
}

/** What has been said on a line, set under it: a thread's comments, or your own pending comment. A resolved thread is dimmed. */
export const Annotation = ({ kind, heading, comments, dim }: Props) => {
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
      </article>
    </li>
  );
};
