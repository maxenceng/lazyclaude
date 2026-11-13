import { Children, type ReactNode } from 'react';
import { useWholeRows } from './wholeRows';
import styles from './Columns.module.css';

/** Columns side by side that share the width equally, so a status keeps its place whatever it holds. */
export const Columns = ({ children }: { children: ReactNode }) => {
  return <div className={styles.columns}>{children}</div>;
};

interface ColumnProps {
  label: string;
  /** The column's heading, kept above whatever it holds. */
  heading?: ReactNode;
  /** What the column says while it holds nothing. */
  empty: string;
  children?: ReactNode;
}

/** One column: a heading over what it holds, or over a word saying it holds nothing. */
export const Column = ({ label, heading, empty, children }: ColumnProps) => {
  const column = useWholeRows<HTMLDivElement>();
  return (
    <div ref={column} role="group" aria-label={label} className={styles.column}>
      {heading}
      {Children.count(children) === 0 ? <p className={styles.empty}>{empty}</p> : <ul className={styles.list}>{children}</ul>}
    </div>
  );
};
