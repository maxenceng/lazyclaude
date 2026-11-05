import { Markdown, type Target } from './Markdown';
import styles from './Document.module.css';

interface Props {
  source: string;
  onLink: (href: string) => void;
  target: Target;
}

/** A document filling the main view: its markdown, set in from the pane's edges. */
export const Document = ({ source, onLink, target }: Props) => {
  return (
    <div className={styles.document}>
      <Markdown source={source} onLink={onLink} target={target} />
    </div>
  );
};
