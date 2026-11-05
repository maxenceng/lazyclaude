import { useTranslation } from 'react-i18next';
import styles from './FileRow.module.css';

interface Props {
  letter: string;
  change: 'added' | 'modified' | 'deleted' | 'renamed';
  name: string;
  openThreads: number;
  pending: number;
  selected: boolean;
  onSelect: () => void;
}

/** One file of a pull request: the letter that says what it does to the file, its name, and the open threads and pending comments on it. The selected row is a light bar; a press on it again goes to its first line. */
export const FileRow = ({ letter, change, name, openThreads, pending, selected, onSelect }: Props) => {
  const { t } = useTranslation('designSystem');
  return (
    <li>
      <button type="button" className={[styles.row, selected ? styles.selected : ''].join(' ')} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        <span className={[styles.letter, styles[change]].join(' ')}>{letter}</span>
        <span className={styles.name}>{name}</span>
        {openThreads === 0 ? null : (
          <span className={styles.threads} aria-label={t('fileRow.openThreads', { count: openThreads })}>
            ◆{openThreads}
          </span>
        )}
        {pending === 0 ? null : (
          <span className={styles.pending} aria-label={t('fileRow.pending', { count: pending })}>
            ✎{pending}
          </span>
        )}
      </button>
    </li>
  );
};
