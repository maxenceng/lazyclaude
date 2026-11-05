import type { ReactNode } from 'react';
import styles from './AppFrame.module.css';

export type Arrangement = 'full' | 'noDock' | 'noMain' | 'zoomed';

interface Props {
  statusLine: ReactNode;
  keyBar: ReactNode;
  board: ReactNode;
  main: ReactNode;
  dock: ReactNode;
  arrangement: Arrangement;
}

/** The window: status line over the panes over the key bar, panes divided by seams; the board on the left, the main view over the dock on the right. */
export const AppFrame = ({ statusLine, keyBar, board, main, dock, arrangement }: Props) => {
  return (
    <div className={styles.frame}>
      {statusLine}
      <main className={`${styles.panes} ${styles[arrangement]}`}>
        {arrangement === 'zoomed' ? null : <div className={styles.board}>{board}</div>}
        {arrangement === 'noMain' || arrangement === 'zoomed' ? null : <div className={styles.main}>{main}</div>}
        {arrangement === 'noDock' ? null : <div className={styles.dock}>{dock}</div>}
      </main>
      {keyBar}
    </div>
  );
};
