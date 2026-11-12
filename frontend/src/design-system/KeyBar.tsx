import styles from './KeyBar.module.css';

const SPLIT = ' ';

/** The key of one entry, `j/k` of `j/k move`, and what it does after it. */
const partsOf = (entry: string): [string, string] => {
  const at = entry.indexOf(SPLIT);
  return at < 0 ? [entry, ''] : [entry.slice(0, at), entry.slice(at + 1)];
};

/** The keys that work where focus is, along the bottom: each key set apart in the key type, what it does after it. */
export const KeyBar = ({ keys }: { keys: readonly string[] }) => {
  return (
    <footer className={styles.bar}>
      {keys.map((entry) => {
        const [key, does] = partsOf(entry);
        return (
          <span key={entry} className={styles.entry}>
            <kbd className={styles.key}>{key}</kbd> {does}
          </span>
        );
      })}
    </footer>
  );
};
