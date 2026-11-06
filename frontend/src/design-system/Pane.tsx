import { useId, type ReactNode } from 'react';
import styles from './Pane.module.css';

/** `Fill` hands the pane's whole height to its content, which scrolls itself; `Flow` scrolls the pane. */
export enum PaneLayout {
  Flow = 'flow',
  Fill = 'fill',
}

interface Props {
  title: string;
  focused: boolean;
  layout?: PaneLayout;
  children: ReactNode;
}

/** A region of the screen between hairline seams; the focused one has an edge. */
export const Pane = ({ title, focused, layout = PaneLayout.Flow, children }: Props) => {
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
