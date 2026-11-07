import type { ReactNode } from 'react';
import { Pane } from './Pane';
import styles from './Popup.module.css';

interface Props {
  title: string;
  children: ReactNode;
}

/** A pane floated over the window with no scrim, so what is behind stays legible; it carries the focus edge because it holds the keys. */
export const Popup = ({ title, children }: Props) => {
  return (
    <div className={styles.popup} role="dialog" aria-modal="true" aria-label={title}>
      <Pane title={title} focused>
        <div className={styles.content}>{children}</div>
      </Pane>
    </div>
  );
};
