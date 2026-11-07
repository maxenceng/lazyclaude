import { type MouseEvent, type ReactNode, useId } from 'react';
import styles from './Dialog.module.css';

interface Props {
  title: string;
  hint: string;
  children: ReactNode;
  /** A press outside the popup: given, it leaves the popup as its own cancel does; absent, the press does nothing. */
  onDismiss?: () => void;
}

/** A press on the scrim itself, not on the popup inside it. */
const dismissOn =
  (onDismiss: (() => void) | undefined) =>
  (event: MouseEvent): void => {
    if (event.target === event.currentTarget) {
      onDismiss?.();
    }
  };

/** A popup over the window that takes the keys until it is answered: a title, what it asks, and the keys that answer it. A press, not a click, outside it dismisses it, so a selection dragged out of it does not. */
export const Dialog = ({ title, hint, children, onDismiss }: Props) => {
  const heading = useId();
  return (
    <div className={styles.scrim} role="presentation" data-modal="" onMouseDown={dismissOn(onDismiss)}>
      <section role="dialog" aria-modal="true" aria-labelledby={heading} className={styles.dialog}>
        <h2 id={heading} className={styles.title}>
          {title}
        </h2>
        {children}
        <p className={styles.hint}>{hint}</p>
      </section>
    </div>
  );
};
