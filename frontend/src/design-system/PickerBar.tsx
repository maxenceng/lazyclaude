import { type ReactNode, useRef } from 'react';
import { useDismissal } from './dismissal';
import styles from './PickerBar.module.css';

interface Props {
  label: string;
  shortcut: string;
  /** What the picker is called, for a screen reader. */
  popupLabel: string;
  selected: boolean;
  open: boolean;
  onPress: () => void;
  onDismiss: () => void;
  /** The picker's content, shown while `open`, growing upward from the bar. */
  children: ReactNode;
}

/**
 * A bar pinned at the foot of a pane, full width, with a picker that grows upward from it over the pane's body while open.
 * A click outside the bar and its picker dismisses it, as `esc` does through the window's keys.
 */
export const PickerBar = ({ label, shortcut, popupLabel, selected, open, onPress, onDismiss, children }: Props) => {
  const wrap = useRef<HTMLDivElement>(null);
  useDismissal(wrap, open, onDismiss, false);
  return (
    <div ref={wrap} className={styles.wrap}>
      {open ? (
        <div className={styles.popup} role="group" aria-label={popupLabel}>
          {children}
        </div>
      ) : null}
      <button
        type="button"
        className={selected ? `${styles.bar} ${styles.selected}` : styles.bar}
        aria-current={selected ? 'true' : undefined}
        aria-expanded={open}
        onMouseDown={(event) => event.preventDefault()}
        onClick={onPress}
      >
        <svg className={styles.icon} viewBox="0 0 12 12" aria-hidden="true">
          <path d="M6 2 L6 10 M2 6 L10 6" />
        </svg>
        <span className={styles.label}>{label}</span>
        <kbd className={styles.key}>{shortcut}</kbd>
      </button>
    </div>
  );
};
