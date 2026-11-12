import type { CSSProperties } from 'react';
import styles from './DocsTreeRow.module.css';

interface Props {
  name: string;
  depth: number;
  selected: boolean;
  onSelect: () => void;
  /** A folder's header carries how many documents it holds and whether it is folded; a document carries neither. */
  folder?: { count: number; folded: boolean };
}

/** One line of the Docs panel: a document's file name, or a folder's header with its count. The selected row is a light bar; a press on it again opens it. */
export const DocsTreeRow = ({ name, depth, selected, onSelect, folder }: Props) => {
  const classes = [styles.row, selected ? styles.selected : '', folder === undefined ? '' : styles.folder].join(' ');
  return (
    <li data-row="">
      <button
        type="button"
        className={classes}
        style={{ '--depth': depth } as CSSProperties}
        aria-current={selected ? 'true' : undefined}
        aria-expanded={folder === undefined ? undefined : !folder.folded}
        onClick={onSelect}
      >
        <span className={styles.name}>{name}</span>
        {folder === undefined ? null : <span className={styles.count}>{folder.count}</span>}
      </button>
    </li>
  );
};
