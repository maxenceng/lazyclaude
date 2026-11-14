import styles from './KeyBar.module.css';

const SPLIT = ' ';

/** The key of one entry, `j/k` of `j/k move`, and what it does after it. */
const partsOf = (entry: string): [string, string] => {
  const at = entry.indexOf(SPLIT);
  return at < 0 ? [entry, ''] : [entry.slice(0, at), entry.slice(at + 1)];
};

const Entry = ({ entry, lead }: { entry: string; lead: boolean }) => {
  const [key, does] = partsOf(entry);
  return (
    <span className={lead ? `${styles.entry} ${styles.lead}` : styles.entry}>
      <kbd className={styles.key}>{key}</kbd> {does}
    </span>
  );
};

/**
 * The keys that work where focus is, along the bottom: each key set apart in the key type, what it does after it.
 * A `lead` entry goes first and is the one that gives way, clipped with an ellipsis, when the bar runs out of room.
 */
export const KeyBar = ({ keys, lead }: { keys: readonly string[]; lead?: string | undefined }) => {
  return (
    <footer className={styles.bar}>
      {lead === undefined ? null : <Entry entry={lead} lead />}
      {keys.map((entry) => (
        <Entry key={entry} entry={entry} lead={false} />
      ))}
    </footer>
  );
};
