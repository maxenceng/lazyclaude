import { useId, type ReactNode } from 'react';
import styles from './Pane.module.css';

interface Props {
  title: string;
  focused: boolean;
  /** `fill` hands the pane's whole height to its content, which scrolls itself; `flow` scrolls the pane. */
  layout?: 'flow' | 'fill';
  children: ReactNode;
}

/** A region of the screen between hairline seams; the focused one has an edge. */
export const Pane = ({ title, focused, layout = 'flow', children }: Props) => {
  const heading = useId();
  return (
    <section className={[styles.pane, styles[layout], focused ? styles.focused : ''].join(' ')} aria-labelledby={heading}>
      <h2 id={heading} className={styles.title}>
        {title}
      </h2>
      {children}
    </section>
  );
};
