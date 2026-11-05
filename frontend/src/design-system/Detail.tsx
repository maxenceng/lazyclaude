import styles from './Detail.module.css';
import { Markdown } from './Markdown';

interface Props {
  facts: readonly string[];
  title: string;
  body: string | undefined;
  onLink: (href: string) => void;
}

/** What is selected: its facts in a row, its title, its text typeset as markdown. */
export const Detail = ({ facts, title, body, onLink }: Props) => {
  return (
    <article className={styles.detail}>
      <div className={styles.facts}>
        {facts.map((fact) => (
          <span key={fact}>{fact}</span>
        ))}
      </div>
      <h3 className={styles.title}>{title}</h3>
      {body === undefined ? null : <Markdown source={body} onLink={onLink} />}
    </article>
  );
};
