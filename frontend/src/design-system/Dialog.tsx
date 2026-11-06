import { type ReactNode, useId } from 'react';
import styles from './Dialog.module.css';

interface Props {
  title: string;
  hint: string;
  children: ReactNode;
}

/** A popup over the window that takes the keys until it is answered: a title, what it asks, and the keys that answer it. */
export const Dialog = ({ title, hint, children }: Props) => {
  const heading = useId();
  return (
    <div className={styles.scrim}>
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
