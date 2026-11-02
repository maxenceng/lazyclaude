import styles from './KeyBar.module.css';

/** The keys that work where focus is, along the bottom. */
export const KeyBar = ({ keys }: { keys: readonly string[] }) => {
  return (
    <footer className={styles.bar}>
      {keys.map((key) => (
        <span key={key} className={styles.key}>
          {key}
        </span>
      ))}
    </footer>
  );
};
