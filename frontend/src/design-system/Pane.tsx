import { useId, type ReactNode } from 'react';
import styles from './Pane.module.css';

interface Props {
  title: string;
  focused: boolean;
  children: ReactNode;
}

/** A region of the screen between hairline seams; the focused one has an edge. */
export function Pane({ title, focused, children }: Props) {
  const heading = useId();
  return (
    <section className={focused ? `${styles.pane} ${styles.focused}` : styles.pane} aria-labelledby={heading}>
      <h2 id={heading} className={styles.title}>
        {title}
      </h2>
      {children}
    </section>
  );
}
