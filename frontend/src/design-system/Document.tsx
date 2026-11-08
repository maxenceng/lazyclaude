import { Markdown, type Target } from './Markdown';
import styles from './Document.module.css';
export type { Target };

interface Props {
  source: string;
  onLink: (href: string) => void;
  target: Target;
}

/** A document filling the main view: its markdown, set in from the pane's edges, its reading column widening with the pane. */
export const Document = ({ source, onLink, target }: Props) => {
  return (
    <div className={styles.frame}>
      <Markdown source={source} onLink={onLink} target={target} />
    </div>
  );
};
