import type { ReactNode } from 'react';
import styles from './Cards.module.css';

/** A column's cards, stacked with a gap between them. */
export const Cards = ({ children }: { children: ReactNode }) => {
  return <ul className={styles.list}>{children}</ul>;
};
