import type { ReactNode } from 'react';
import styles from './AppFrame.module.css';

export type Arrangement = 'full' | 'noDock' | 'noMain' | 'zoomed';

interface Props {
  statusLine: ReactNode;
  keyBar: ReactNode;
  board: ReactNode;
  docs: ReactNode;
  /** A short panel under the left column's others, as a review's pending review stands under Files and Comments. */
  foot?: ReactNode;
  main: ReactNode;
  dock: ReactNode;
  arrangement: Arrangement;
}

/** The window: status line over the panes over the key bar, panes divided by seams; the board over the docs on the left, the main view over the dock on the right. */
export const AppFrame = ({ statusLine, keyBar, board, docs, foot, main, dock, arrangement }: Props) => {
  return (
    <div className={styles.frame}>
      {statusLine}
      <main className={`${styles.panes} ${styles[arrangement]}`}>
        {arrangement === 'zoomed' ? null : <div className={styles.left}>
            <div className={styles.board}>{board}</div>
            {docs === null ? null : <div className={styles.docs}>{docs}</div>}
            {foot === undefined ? null : <div className={styles.foot}>{foot}</div>}
          </div>}
        {arrangement === 'noMain' || arrangement === 'zoomed' ? null : <div className={styles.main}>{main}</div>}
        {arrangement === 'noDock' ? null : <div className={styles.dock}>{dock}</div>}
      </main>
      {keyBar}
    </div>
  );
};
