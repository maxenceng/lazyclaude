import type { MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './PaneAction.module.css';

/** What a pane's action does, which picks its icon, one mark per kind: close what the pane shows, hide the pane, fold a pane to its strip, unfold it, fill the window with it, or add to it. */
export enum PaneActionKind {
  Close = 'close',
  Hide = 'hide',
  Minimise = 'minimise',
  Restore = 'restore',
  Zoom = 'zoom',
  Add = 'add',
}

export interface PaneActionProps {
  kind: PaneActionKind;
  /** What the button does, for a screen reader and the tooltip, since its icon says nothing. */
  label: string;
  /** The key that does the same, named beside the label; none while no key is bound to it. */
  shortcut: string | undefined;
  onClick: () => void;
}

const PATH: Readonly<Record<PaneActionKind, string>> = {
  [PaneActionKind.Close]: 'M2 2 L10 10 M10 2 L2 10',
  [PaneActionKind.Hide]: 'M2 2 H10 V10 H2 Z M7.5 2 V10',
  [PaneActionKind.Minimise]: 'M2 4 L6 8 L10 4',
  [PaneActionKind.Restore]: 'M2 8 L6 4 L10 8',
  [PaneActionKind.Zoom]: 'M2 5 V2 H5 M7 2 H10 V5 M10 7 V10 H7 M5 10 H2 V7',
  [PaneActionKind.Add]: 'M6 2 L6 10 M2 6 L10 6',
};

/** A click keeps the focus where the window's panes put it; the button is reached by its key, not by tabbing. */
const keepFocus = (event: MouseEvent) => event.preventDefault();

/** The one button a pane's title row or a dock strip carries: a square that lights when hovered. */
export const PaneAction = ({ kind, label, shortcut, onClick }: PaneActionProps) => {
  const { t } = useTranslation('designSystem');
  const named = shortcut === undefined ? label : t('paneAction.label', { action: label, key: shortcut });
  return (
    <button type="button" className={styles.action} aria-label={named} title={named} onMouseDown={keepFocus} onClick={onClick}>
      <svg className={styles.icon} viewBox="0 0 12 12" aria-hidden="true">
        <path d={PATH[kind]} />
      </svg>
    </button>
  );
};
