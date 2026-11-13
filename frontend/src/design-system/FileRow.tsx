import { useTranslation } from 'react-i18next';
import { Badge, BadgeTone } from './Badge';
import styles from './FileRow.module.css';

export enum FileChange {
  Added = 'added',
  Modified = 'modified',
  Deleted = 'deleted',
  Renamed = 'renamed',
}

interface Props {
  letter: string;
  change: FileChange;
  name: string;
  /** Where the file lives, dimmed under its name. */
  directory: string;
  added: number;
  removed: number;
  threads: number;
  pending: number;
  selected: boolean;
  onSelect: () => void;
}

/**
 * One file of a pull request in the rail beside its diff: the letter that says what it does to the file, its name and how many lines
 * it adds and removes, and under them where it lives and a word badge for its threads and for your pending comments. The selected
 * row is lifted; a press on it goes to its first line.
 */
export const FileRow = ({ letter, change, name, directory, added, removed, threads, pending, selected, onSelect }: Props) => {
  const { t } = useTranslation('designSystem');
  return (
    <li data-row="">
      <button type="button" className={[styles.row, selected ? styles.selected : ''].join(' ')} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        <span className={[styles.letter, styles[change]].join(' ')} title={t(`fileRow.change.${change}`)}>
          {letter}
        </span>
        <span className={styles.name}>{name}</span>
        <span className={styles.delta}>
          <b className={styles.plus}>+{added}</b> <b className={styles.minus}>−{removed}</b>
        </span>
        <span className={styles.directory}>{directory}</span>
        <span className={styles.badges}>
          {threads === 0 ? null : <Badge tone={BadgeTone.Blue}>{t('fileRow.threads', { count: threads })}</Badge>}
          {pending === 0 ? null : <Badge tone={BadgeTone.Amber}>{t('fileRow.pending', { count: pending })}</Badge>}
        </span>
      </button>
    </li>
  );
};
