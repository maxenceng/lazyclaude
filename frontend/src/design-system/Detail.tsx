import styles from './Detail.module.css';

interface Props {
  facts: readonly string[];
  title: string;
  body: string | undefined;
}

/** What is selected: its facts in a row, its title, its text. */
export function Detail({ facts, title, body }: Props) {
  return (
    <article className={styles.detail}>
      <div className={styles.facts}>
        {facts.map((fact) => (
          <span key={fact}>{fact}</span>
        ))}
      </div>
      <h3 className={styles.title}>{title}</h3>
      {body === undefined ? null : <p className={styles.body}>{body}</p>}
    </article>
  );
}
