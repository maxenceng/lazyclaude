import styles from './DiffRule.module.css';

export enum RuleKind {
  File = 'file',
  Hunk = 'hunk',
}

interface Props {
  kind: RuleKind;
  text: string;
}

/** A line of a diff that is not code: the rule that opens a file, naming it, or a hunk's header. */
export const DiffRule = ({ kind, text }: Props) => {
  return <li>{kind === RuleKind.File ? <h3 className={styles.file}>{text}</h3> : <div className={styles.hunk}>{text}</div>}</li>;
};
