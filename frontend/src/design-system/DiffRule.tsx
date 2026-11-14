import { useTranslation } from 'react-i18next';
import type { FileChange } from './FileRow';
import { LEFT_BUTTON } from './mouse';
import styles from './DiffRule.module.css';

export enum RuleKind {
  File = 'file',
  Hunk = 'hunk',
}

interface Props {
  kind: RuleKind;
  text: string;
  /** How many lines a file adds and removes, at the right end of its rule. */
  delta?: { added: number; removed: number };
  /** What the diff does to the file, its letter before the path in the colour of the change, as in the files rail. */
  change?: { letter: string; kind: FileChange };
  /** Whether the cursor stands on a file's rule, lifted and edged as a diff line under the cursor is. */
  cursor?: boolean;
  /** A press on a file's rule, which moves the cursor to it; a rule without one takes no press. */
  onPress?: () => void;
}

const DIRECTORY_AND_NAME = /^(.*\/)?([^/]*)$/;
const HUNK_RANGE_AND_CONTEXT = /^(@@[^@]*@@)\s*(.*)$/;

/** A line of a diff that is not code: the rule that opens a file, its change's letter, its directory dimmed before its bold name and held at the top while the file scrolls, or a hunk's header, its range apart from the context it names. */
export const DiffRule = ({ kind, text, delta, change, cursor = false, onPress }: Props) => {
  const { t } = useTranslation('designSystem');
  if (kind === RuleKind.Hunk) {
    const [, range = '', context = ''] = HUNK_RANGE_AND_CONTEXT.exec(text) ?? [];
    return (
      <li>
        <div className={styles.hunk}>
          <span className={styles.range}>{range === '' ? text : range}</span>
          {context === '' ? null : <span className={styles.context}>{context}</span>}
        </div>
      </li>
    );
  }
  const [, directory = '', name = text] = DIRECTORY_AND_NAME.exec(text) ?? [];
  return (
    <li className={styles.sticky}>
      <div
        role="presentation"
        onMouseDown={(event) => {
          if (event.button === LEFT_BUTTON) {
            onPress?.();
          }
        }}
      >
      <h3 className={[styles.file, cursor ? styles.cursor : ''].join(' ')} aria-current={cursor ? 'true' : undefined}>
        {change === undefined ? null : (
          <span className={[styles.letter, styles[change.kind]].join(' ')} title={t(`fileRow.change.${change.kind}`)}>
            {change.letter}
          </span>
        )}
        <span className={styles.path}>
          {directory}
          <b>{name}</b>
        </span>
        {delta === undefined ? null : (
          <span className={styles.delta}>
            <b className={styles.plus}>+{delta.added}</b> <b className={styles.minus}>−{delta.removed}</b>
          </span>
        )}
      </h3>
      </div>
    </li>
  );
};
