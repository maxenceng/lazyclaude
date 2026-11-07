import type { MouseEvent } from 'react';
import styles from './PaneAction.module.css';

/** What a pane's action does, which picks its icon: close what the pane shows, fold a pane to its strip, or unfold it. */
export enum PaneActionKind {
  Close = 'close',
  Minimise = 'minimise',
  Restore = 'restore',
}

export interface PaneActionProps {
  kind: PaneActionKind;
  /** What the button does, for a screen reader and the tooltip, since its icon says nothing. */
  label: string;
  onClick: () => void;
}

const PATH: Readonly<Record<PaneActionKind, string>> = {
  [PaneActionKind.Close]: 'M2 2 L10 10 M10 2 L2 10',
  [PaneActionKind.Minimise]: 'M2 4 L6 8 L10 4',
  [PaneActionKind.Restore]: 'M2 8 L6 4 L10 8',
};

/** A click keeps the focus where the window's panes put it; the button is reached by its key, not by tabbing. */
const keepFocus = (event: MouseEvent) => event.preventDefault();

/** The one button a pane's title row or a dock strip carries: a square with a hairline edge, lit when hovered. */
export const PaneAction = ({ kind, label, onClick }: PaneActionProps) => {
  return (
    <button type="button" className={styles.action} aria-label={label} title={label} onMouseDown={keepFocus} onClick={onClick}>
      <svg className={styles.icon} viewBox="0 0 12 12" aria-hidden="true">
        <path d={PATH[kind]} />
      </svg>
    </button>
  );
};
