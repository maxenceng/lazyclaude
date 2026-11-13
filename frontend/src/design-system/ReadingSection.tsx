import { useId, type ReactNode } from 'react';
import styles from './ReadingSection.module.css';

/** A titled part of what a pane reads out, such as a ticket's sessions: its name as a small dim label over what it holds. */
export const ReadingSection = ({ title, children }: { title: string; children: ReactNode }) => {
  const heading = useId();
  return (
    <section className={styles.section} aria-labelledby={heading}>
      <h3 id={heading} className={styles.title}>
        {title}
      </h3>
      {children}
    </section>
  );
};
