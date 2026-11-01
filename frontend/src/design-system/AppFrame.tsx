import type { ReactNode } from 'react';
import styles from './AppFrame.module.css';

interface Props {
  statusLine: ReactNode;
  keyBar: ReactNode;
  children: ReactNode;
}

/** The window: status line over the panes over the key bar, panes divided by seams. */
export function AppFrame({ statusLine, keyBar, children }: Props) {
  return (
    <div className={styles.frame}>
      {statusLine}
      <main className={styles.panes}>{children}</main>
      {keyBar}
    </div>
  );
}
