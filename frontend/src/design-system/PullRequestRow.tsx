import { Ci, CiGlyph } from './CiGlyph';
import styles from './PullRequestRow.module.css';

interface Props {
  ci: Ci | undefined;
  number: number;
  title: string;
  head: string;
  selected: boolean;
  onSelect: () => void;
}

/** One open pull request: its CI glyph, its number, its title and its head branch dimmed. The selected row is a light bar; a press on it opens its review. */
export const PullRequestRow = ({ ci, number, title, head, selected, onSelect }: Props) => {
  return (
    <li>
      <button type="button" className={selected ? `${styles.row} ${styles.selected}` : styles.row} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        <CiGlyph ci={ci} onLight={selected} />
        <span className={styles.number}>#{number}</span>
        <span className={styles.title}>{title}</span>
        <span className={styles.head}>{head}</span>
      </button>
    </li>
  );
};
