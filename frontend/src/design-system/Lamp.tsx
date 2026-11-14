import { useTranslation } from 'react-i18next';
import styles from './Lamp.module.css';

export enum LampColour {
  Permission = 'permission',
  Input = 'input',
  Turn = 'turn',
  Off = 'off',
}

const GLYPH: Record<LampColour, string> = { permission: '!', input: '?', turn: '•', off: '○' };
const MEANING = {
  permission: 'lamp.permission',
  input: 'lamp.input',
  turn: 'lamp.turn',
  off: 'lamp.off',
} as const satisfies Record<LampColour, string>;
/** What a lamp's state is called where there is room for a word or two: on a tile, and after a count. */
export const SHORT_MEANING = {
  permission: 'lamp.shortPermission',
  input: 'lamp.shortInput',
  turn: 'lamp.shortTurn',
  off: 'lamp.shortOff',
} as const satisfies Record<LampColour, string>;

/** A marker beside a session, a worded pill on a ticket's row, or a tile on the andon board. */
export enum LampSize {
  Marker = 'marker',
  Pill = 'pill',
  Tile = 'tile',
}

interface Props {
  colour: LampColour;
  size: LampSize;
  number?: string;
  /** The words a pill says in place of its lamp's own, for a state the lamp does not tell apart: busy, ended. */
  words?: string;
}

/** A session's state as a lamp: a marker beside a session, a pill saying it in words on a ticket's row, or a tile on the andon board. Its colour means session state and nothing else. */
export const Lamp = ({ colour, size, number, words }: Props) => {
  const { t } = useTranslation('designSystem');
  if (size === LampSize.Pill) {
    return (
      <span className={`${styles.pill} ${styles[colour]}`} role="img" aria-label={words ?? t(MEANING[colour])}>
        {words ?? t(SHORT_MEANING[colour])}
      </span>
    );
  }
  if (size === LampSize.Marker) {
    return (
      <span className={`${styles.marker} ${styles[colour]}`} role="img" aria-label={t(MEANING[colour])}>
        {GLYPH[colour]}
      </span>
    );
  }
  return (
    <span className={`${styles.tile} ${styles[colour]}`} role="img" aria-label={t('lamp.tile', { number: number ?? t('lamp.noTicket'), meaning: t(MEANING[colour]) })}>
      <b className={styles.number}>{number ?? t('lamp.noNumber')}</b>
      <i className={styles.meaning}>{t(SHORT_MEANING[colour])}</i>
    </span>
  );
};
