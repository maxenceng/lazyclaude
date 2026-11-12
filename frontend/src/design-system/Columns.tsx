import type { ReactNode } from 'react';
import { useWholeRows } from './wholeRows';
import styles from './Columns.module.css';

interface ColumnsProps {
  /** A click that lands on no button: a column's empty space or its heading. */
  onBackground?: () => void;
  children: ReactNode;
}

/** Columns side by side that share the width; they scroll sideways when a half tile cannot hold them. */
export const Columns = ({ onBackground, children }: ColumnsProps) => {
  return (
    <div
      className={styles.columns}
      role="presentation"
      onClick={(event) => {
        if (onBackground !== undefined && !(event.target instanceof Element && event.target.closest('button') !== null)) {
          onBackground();
        }
      }}
    >
      {children}
    </div>
  );
};

interface ColumnProps {
  label: string;
  children: ReactNode;
}

/** One column: a status's heading over its rows. */
export const Column = ({ label, children }: ColumnProps) => {
  const column = useWholeRows<HTMLDivElement>();
  return (
    <div ref={column} role="group" aria-label={label} className={styles.column}>
      {children}
    </div>
  );
};
