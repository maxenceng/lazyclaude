import type { ReactNode } from 'react';
import styles from './Button.module.css';

/** `Affirm` is the green button of a pane's one action that commits something. */
export enum ButtonTone {
  Plain = 'plain',
  Affirm = 'affirm',
}

interface Props {
  onClick: () => void;
  tone?: ButtonTone;
  /** The key that does the same, set in a chip after the label: the chord as bound. */
  chip?: string;
  children: ReactNode;
}

export const Button = ({ onClick, tone = ButtonTone.Plain, chip, children }: Props) => {
  return (
    <button type="button" className={[styles.button, styles[tone]].join(' ')} aria-keyshortcuts={chip} onClick={onClick}>
      {children}
      {chip === undefined ? null : <kbd className={styles.chip} aria-hidden="true">{chip}</kbd>}
    </button>
  );
};
