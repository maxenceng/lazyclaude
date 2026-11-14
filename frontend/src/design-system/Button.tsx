import type { MouseEvent, ReactNode } from 'react';
import styles from './Button.module.css';

/** `Affirm` is the green button of a pane's one action that commits something. */
export enum ButtonTone {
  Plain = 'plain',
  Affirm = 'affirm',
  /** A way out beside an affirm: a seam border on no fill. */
  Quiet = 'quiet',
}

/** A press keeps the focus where it was, so a popup's input still takes the keys after one of its buttons is pressed. */
const keepFocus = (event: MouseEvent) => event.preventDefault();

interface Props {
  onClick: () => void;
  tone?: ButtonTone;
  /** The key that does the same, set in a chip after the label: the chord as bound. */
  chip?: string;
  children: ReactNode;
}

export const Button = ({ onClick, tone = ButtonTone.Plain, chip, children }: Props) => {
  return (
    <button type="button" className={[styles.button, styles[tone]].join(' ')} aria-keyshortcuts={chip} onMouseDown={keepFocus} onClick={onClick}>
      {children}
      {chip === undefined ? null : <kbd className={styles.chip} aria-hidden="true">{chip}</kbd>}
    </button>
  );
};
