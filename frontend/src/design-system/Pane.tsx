import { useId, useRef, type ReactNode } from 'react';
import styles from './Pane.module.css';
import { useWholeRows } from './wholeRows';
import { usePressFocus } from './pressFocus';
import { PaneAction, type PaneActionProps } from './PaneAction';
import type { StatusColour } from './StatusHeading';
import { PanelTabs, type PanelTabsProps } from './PanelTabs';

export { PaneActionKind } from './PaneAction';

export interface PaneChip {
  label: string;
  colour?: StatusColour | undefined;
}

/** `Fill` hands the pane's whole height to its content, which scrolls itself; `Flow` scrolls the pane's body under its title row. */
export enum PaneLayout {
  Flow = 'flow',
  Fill = 'fill',
}

interface Props {
  title: string;
  focused: boolean;
  layout?: PaneLayout;
  /** The faces the pane shares with another, named in its title row in place of its title, which then only labels the pane. */
  tabs?: PanelTabsProps;
  /** Words about what the pane shows, beside its title; the title row stays one line, so chips that do not fit are clipped. */
  chips?: readonly PaneChip[];
  /** What the pane names beside its title, in its title row, such as the dock's tabs; it takes the room the actions leave. */
  beside?: ReactNode;
  /** Buttons in the fixed slot at the right end of the title row; screens give them, they do not place their own. */
  actions?: readonly PaneActionProps[];
  /** Pinned under the body, whatever its scroll. */
  foot?: ReactNode;
  /** A press anywhere in the pane, its title row and body included, hands it focus; the window's keys then act on it. */
  onFocus?: () => void;
  children: ReactNode;
}

/** A region of the screen between hairline seams; the focused one lights its title and top edge, and only it gives its cursor row the bright fill. */
export const Pane = ({ title, focused, layout = PaneLayout.Flow, actions = [], tabs, chips = [], beside, foot, onFocus, children }: Props) => {
  const heading = useId();
  const body = useWholeRows<HTMLDivElement>();
  const pane = useRef<HTMLElement>(null);
  usePressFocus(pane, onFocus);
  return (
    <section className={[styles.pane, styles[layout], focused ? styles.focused : ''].join(' ')} aria-labelledby={tabs === undefined ? heading : undefined} aria-label={tabs === undefined ? undefined : title} ref={pane}>
      <div className={styles.head}>
        {tabs === undefined ? (
          <h2 id={heading} className={styles.title}>
            {title}
          </h2>
        ) : (
          <PanelTabs {...tabs} />
        )}
        {chips.length === 0 ? null : (
          <ul className={styles.chips} aria-label={title}>
            {chips.map((chip) => (
              <li key={chip.label} className={styles.chip} data-status={chip.colour}>
                {chip.label}
              </li>
            ))}
          </ul>
        )}
        {beside === undefined ? null : <div className={styles.beside}>{beside}</div>}
        {actions.length === 0 ? null : (
          <div className={styles.slot} data-pane-actions="">
            {actions.map((action) => (
              <PaneAction key={action.kind} {...action} />
            ))}
          </div>
        )}
      </div>
      <div ref={layout === PaneLayout.Flow ? body : undefined} className={styles.body} data-pane-body="">{children}</div>
      {foot === undefined ? null : <div className={styles.foot} data-pane-foot="">{foot}</div>}
    </section>
  );
};
