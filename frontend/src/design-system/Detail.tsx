import type { ReactNode } from 'react';
import styles from './Detail.module.css';
import { Markdown } from './Markdown';

interface Props {
  facts: readonly string[];
  title: string;
  /** Under the title, across the pane: where it stands. */
  summary?: ReactNode;
  /** Under the summary, across the pane: what is done with it, before its text. */
  lead?: ReactNode;
  body: string | undefined;
  onLink: (href: string) => void;
}

/** What is selected: its facts in a row, its title, where it stands, what is done with it, then its text typeset as markdown. Facts and title sit on the column the text is set in; the summary and the lead run across the pane, and the text keeps its reading measure, centred. */
export const Detail = ({ facts, title, summary, lead, body, onLink }: Props) => {
  return (
    <div className={styles.frame}>
      <article className={styles.detail}>
        <div className={styles.facts}>
          {facts.map((fact) => (
            <span key={fact}>{fact}</span>
          ))}
        </div>
        <h3 className={styles.title}>{title}</h3>
        {summary === undefined ? null : <div className={styles.across}>{summary}</div>}
        {lead === undefined ? null : <div className={styles.across}>{lead}</div>}
        {body === undefined ? null : (
          <div className={styles.body}>
            <Markdown source={body} onLink={onLink} />
          </div>
        )}
      </article>
    </div>
  );
};
