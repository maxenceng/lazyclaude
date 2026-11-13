import { useEffect, useRef } from 'react';
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
  /** The ticket's type: the card's left edge and its footer's word, in the type's own colour. */
  type?: TicketTypeTag | undefined;
  selected: boolean;
  /** The strongest marker of the ticket's sessions, said in words in the footer; the card is tinted in its colour. */
  lamp: LampColour | undefined;
  pullRequest?: PullRequestCells | undefined;
  onSelect: () => void;
}

/** One ticket on a status column, the same in every column: its title up to three lines over a footer of its number and type, its pull request's marks and the pill of a session waiting on you. Its left edge is its type's colour; a waiting session tints it. The selected card is lifted and its border brightened, fully only in the focused pane. */
export const TicketCard = ({ id, title, type, selected, lamp, pullRequest, onSelect }: Props) => {
  const { t } = useTranslation('designSystem');
  const card = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (selected) {
      card.current?.scrollIntoView({ block: 'nearest' });
    }
  }, [selected]);
  const classes = [styles.card, selected ? styles.selected : ''].join(' ');
  return (
    <li className={styles.slot} data-row="">
      <button
        ref={card}
        type="button"
        className={classes}
        aria-current={selected ? 'true' : undefined}
        data-type={type === undefined ? undefined : (type.colour ?? 'other')}
        data-lamp={lamp}
        onClick={onSelect}
      >
        <span className={styles.title}>{title}</span>
        <span className={styles.footer}>
          <span className={styles.id}>{id}</span>
          {type === undefined ? null : <TypeTag name={type.name} colour={type.colour} />}
          <span className={styles.marks}>
            {pullRequest === undefined ? null : (
              <>
                {pullRequest.marked ? (
                  <span className={styles.mark} role="img" aria-label={t('pullRequestMark.label')}>
                    ⊙
                  </span>
                ) : null}
                <CiGlyph ci={pullRequest.ci} />
              </>
            )}
            {lamp === undefined ? null : <Lamp colour={lamp} size={LampSize.Pill} />}
          </span>
        </span>
      </button>
    </li>
  );
};
