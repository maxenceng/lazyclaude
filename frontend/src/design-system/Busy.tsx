import styles from './Busy.module.css';

/** Something is being read; announced politely. */
export function Busy({ children }: { children: string }) {
  return (
    <p role="status" className={styles.busy}>
      {children}
    </p>
  );
}
