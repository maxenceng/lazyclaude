import { useId, type ReactNode } from 'react';
import styles from './Pane.module.css';
import { PaneAction, type PaneActionProps } from './PaneAction';

export { PaneActionKind } from './PaneAction';

/** `Fill` hands the pane's whole height to its content, which scrolls itself; `Flow` scrolls the pane's body under its title row. */
export enum PaneLayout {
  Flow = 'flow',
  Fill = 'fill',
}

interface Props {
  title: string;
  focused: boolean;
  layout?: PaneLayout;
  /** A button at the right of the title row; screens give it, they do not place their own. */
  action?: PaneActionProps;
  children: ReactNode;
}

/** A region of the screen between hairline seams; the focused one lights its title and top edge, and only it gives its cursor row the bright fill. */
export const Pane = ({ title, focused, layout = PaneLayout.Flow, action, children }: Props) => {
  const heading = useId();
  return (
    <section className={[styles.pane, styles[layout], focused ? styles.focused : ''].join(' ')} aria-labelledby={heading}>
      <div className={styles.head}>
        <h2 id={heading} className={styles.title}>
          {title}
        </h2>
        {action === undefined ? null : <PaneAction {...action} />}
      </div>
      <div className={styles.body}>{children}</div>
    </section>
  );
};
