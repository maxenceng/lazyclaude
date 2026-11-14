import { Tone } from './Text';
import styles from './FactsLine.module.css';

export interface Fact {
  label: string;
  value: string;
  /** `Dim` for a fact that says there is nothing here; every other fact reads plain. */
  tone?: Tone;
}

/** Where what is selected stands, on one line: each fact its label over its value, wrapping when the pane is narrow. */
export const FactsLine = ({ facts }: { facts: readonly Fact[] }) => {
  return (
    <dl className={styles.line}>
      {facts.map((fact) => (
        <div key={fact.label} className={styles.fact}>
          <dt className={styles.label}>{fact.label}</dt>
          <dd className={fact.tone === Tone.Dim ? styles.dim : styles.value}>{fact.value}</dd>
        </div>
      ))}
    </dl>
  );
};
