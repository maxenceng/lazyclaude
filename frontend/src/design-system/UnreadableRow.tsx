import styles from './UnreadableRow.module.css';

/** A ticket that could not be read: its id, then why. */
export function UnreadableRow({ id, reason }: { id: string; reason: string }) {
  return <li className={styles.row}>{`${id} — ${reason}`}</li>;
}
