import type { ReactNode } from 'react';
import styles from './AppFrame.module.css';

/** Which panes the window shows: every one, all but the dock, all but the main view, the dock minimised to its strip under the main view, the dock zoomed over the rest, a tab's screen over a dock folded to a strip, or a tab's screen with its preview beside it over that strip. */
export enum Arrangement {
  Full = 'full',
  NoDock = 'noDock',
  NoMain = 'noMain',
  Zoomed = 'zoomed',
  Tab = 'tab',
  TabPreview = 'tabPreview',
  Minimised = 'minimised',
}

interface Props {
  statusLine: ReactNode;
  keyBar: ReactNode;
  board: ReactNode;
  /** Under the board: the Docs panel. */
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
        {arrangement === Arrangement.Zoomed ? null : <div className={styles.left}>
            <div className={styles.board}>{board}</div>
            {docs === null ? null : <div className={styles.docs}>{docs}</div>}
            {foot === undefined ? null : <div className={styles.foot}>{foot}</div>}
          </div>}
        {arrangement === Arrangement.NoMain || arrangement === Arrangement.Zoomed || arrangement === Arrangement.Tab ? null : <div className={styles.main}>{main}</div>}
        {arrangement === Arrangement.NoDock ? null : <div className={styles.dock}>{dock}</div>}
      </main>
      {keyBar}
    </div>
  );
};
