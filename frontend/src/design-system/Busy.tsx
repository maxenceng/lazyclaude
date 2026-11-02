import styles from './Busy.module.css';

/** Something is being read; announced politely. */
export const Busy = ({ children }: { children: string }) => {
  return (
    <p role="status" className={styles.busy}>
      {children}
    </p>
  );
};
