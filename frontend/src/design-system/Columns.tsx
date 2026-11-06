import type { ReactNode } from 'react';
import styles from './Columns.module.css';

/** Columns side by side that share the width, a narrow one keeping to its heading; they scroll sideways when a half tile cannot hold them. */
export const Columns = ({ children }: { children: ReactNode }) => {
  return <div className={styles.columns}>{children}</div>;
};

interface ColumnProps {
  label: string;
  narrow: boolean;
  children: ReactNode;
}

/** One column: a status's heading over its rows. */
export const Column = ({ label, narrow, children }: ColumnProps) => {
  return (
    <div role="group" aria-label={label} className={narrow ? styles.narrow : styles.column}>
      {children}
    </div>
  );
};
