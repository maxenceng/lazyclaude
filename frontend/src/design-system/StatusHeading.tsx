import styles from './StatusHeading.module.css';

/** The statuses that carry a colour of their own, each named for its token. */
export enum StatusColour {
  InProgress = 'in-progress',
  InReview = 'in-review',
  Todo = 'todo',
  Draft = 'draft',
  Done = 'done',
}

interface Props {
  status: string;
  count: number;
  colour?: StatusColour;
}

/** A status's name over its tickets, with how many there are. */
export const StatusHeading = ({ status, count, colour }: Props) => {
  return (
    <div className={styles.heading} data-status={colour} data-row="">
      <span>{status}</span>
      <span className={styles.count}>{count}</span>
    </div>
  );
};
