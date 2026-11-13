import type { ReactNode } from 'react';
import styles from './PaneFoot.module.css';

interface Props {
  /** What the foot is, for a screen reader. */
  label: string;
  /** A badge saying what state the foot reports. */
  badge: ReactNode;
  /** What the foot's action does, a button. */
  action: ReactNode;
  /** Words about the state, under the badge. */
  children: ReactNode;
}

/** A short footing for a pane, pinned under its body: the state it reports in a badge and words, and the one action that goes with it. */
export const PaneFoot = ({ label, badge, action, children }: Props) => {
  return (
    <section className={styles.foot} aria-label={label}>
      {badge}
      <div className={styles.words}>{children}</div>
      <div className={styles.action}>{action}</div>
    </section>
  );
};
