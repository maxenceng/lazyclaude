import { useTranslation } from 'react-i18next';
import { type Ci, CiGlyph } from './CiGlyph';
import { Lamp, type LampColour, LampSize } from './Lamp';
import { type TicketTypeTag, TypeTag } from './TypeTag';
import styles from './TicketCard.module.css';

/** A ticket's pull request as its card shows it: a dim mark for a draft or one without checks, then its CI result. */
export interface PullRequestCells {
  marked: boolean;
  ci: Ci | undefined;
}

interface Props {
  id: string;
  title: string;
  /** The ticket's type, in its own colour after the number. */
  type?: TicketTypeTag | undefined;
  /** What else the footer names after the type: the other labels. */
  meta: string[];
  selected: boolean;
  done: boolean;
  lamp: LampColour | undefined;
  pullRequest?: PullRequestCells | undefined;
  onSelect: () => void;
}

/** One ticket on a status column: its title in full up to three lines, over a footer of its number and labels, its lamp and its pull request's marks. The selected card is a light tile. */
export const TicketCard = ({ id, title, type, meta, selected, done, lamp, pullRequest, onSelect }: Props) => {
  const { t } = useTranslation('designSystem');
  const classes = [styles.card, selected ? styles.selected : '', done ? styles.done : ''].join(' ');
  return (
    <li className={styles.slot} data-row="">
      <button type="button" className={classes} aria-current={selected ? 'true' : undefined} onClick={onSelect}>
        <span className={styles.title}>{title}</span>
        <span className={styles.footer}>
          <span className={styles.id}>{id}</span>
          {type === undefined ? null : <TypeTag name={type.name} colour={type.colour} />}
          {meta.map((word) => (
            <span key={word} className={styles.meta}>
              {word}
            </span>
          ))}
          <span className={styles.marks}>
            {lamp === undefined ? null : <Lamp colour={lamp} size={LampSize.Marker} />}
            {pullRequest === undefined ? null : (
              <>
                {pullRequest.marked ? (
                  <span className={styles.mark} role="img" aria-label={t('pullRequestMark.label')}>
                    ⊙
                  </span>
                ) : null}
                <CiGlyph ci={pullRequest.ci} onLight={selected} />
              </>
            )}
          </span>
        </span>
      </button>
    </li>
  );
};
