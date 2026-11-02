import type { ReactNode } from 'react';
import styles from './TicketList.module.css';

/** The tickets of one status. */
export const TicketList = ({ children }: { children: ReactNode }) => {
  return <ul className={styles.list}>{children}</ul>;
};
