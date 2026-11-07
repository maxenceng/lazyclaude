import styles from './PaneAction.module.css';

/** What a pane's action does, which picks its glyph: close what the pane shows, fold a pane to its strip, or unfold it. */
export enum PaneActionKind {
  Close = 'close',
  Minimise = 'minimise',
  Restore = 'restore',
}

export interface PaneActionProps {
  kind: PaneActionKind;
  /** What the button does, for a screen reader and the tooltip, since its glyph says nothing. */
  label: string;
  onClick: () => void;
}

const GLYPH: Readonly<Record<PaneActionKind, string>> = {
  [PaneActionKind.Close]: '×',
  [PaneActionKind.Minimise]: '⌄',
  [PaneActionKind.Restore]: '⌃',
};

/** The one button a pane's title row or a dock's tab row carries, dim until hovered or focused. */
export const PaneAction = ({ kind, label, onClick }: PaneActionProps) => {
  return (
    <button type="button" className={styles.action} aria-label={label} title={label} onClick={onClick}>
      <span aria-hidden="true">{GLYPH[kind]}</span>
    </button>
  );
};
