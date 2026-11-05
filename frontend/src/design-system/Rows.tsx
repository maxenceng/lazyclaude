import type { ReactNode } from 'react';
import styles from './Rows.module.css';

/** A list of rows, one per line: a status's tickets, the sessions, the docs tree. */
export const Rows = ({ children }: { children: ReactNode }) => {
  return <ul className={styles.list}>{children}</ul>;
};
