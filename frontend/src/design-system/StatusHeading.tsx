import styles from './StatusHeading.module.css';

interface Props {
  status: string;
  count: number;
}

/** A status's name over its tickets, with how many there are. */
export function StatusHeading({ status, count }: Props) {
  return (
    <div className={styles.heading}>
      <span>{status}</span>
      <span className={styles.count}>{count}</span>
    </div>
  );
}
