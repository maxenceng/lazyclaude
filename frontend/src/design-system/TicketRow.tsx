import { useTranslation } from 'react-i18next';
import { Ci, CiGlyph } from './CiGlyph';
import { Lamp, type LampColour, LampSize } from './Lamp';
import styles from './TicketRow.module.css';

/** A ticket's pull request as the row shows it: a dim mark for a draft or one without checks, then its CI result. */
export interface PullRequestCells {
  marked: boolean;
  ci: Ci | undefined;
}

interface Props {
  id: string;
  title: string;
  selected: boolean;
  done: boolean;
  lamp: LampColour | undefined;
  pullRequest?: PullRequestCells | undefined;
  onSelect: () => void;
}

/** One ticket: its lamp, its number, its title. The selected row is a light bar; a press on it again opens it. */
export const TicketRow = ({ id, title, selected, done, lamp, pullRequest, onSelect }: Props) => {
  const { t } = useTranslation('designSystem');
  const classes = [styles.row, selected ? styles.selected : '', done ? styles.done : ''].join(' ');
  return (
    <li>
      <button type="button" className={classes} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        {lamp === undefined ? <span className={styles.noLamp} /> : <Lamp colour={lamp} size={LampSize.Marker} />}
        <span className={styles.id}>{id}</span>
        <span className={styles.title}>{title}</span>
        {pullRequest === undefined ? null : (
          <>
            {pullRequest.marked ? (
              <span className={styles.mark} role="img" aria-label={t('pullRequestMark.draft')}>
                ⊙
              </span>
            ) : (
              <span className={styles.mark} />
            )}
            <CiGlyph ci={pullRequest.ci} onLight={selected} />
          </>
        )}
      </button>
    </li>
  );
};
