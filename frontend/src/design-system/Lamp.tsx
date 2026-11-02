import { useTranslation } from 'react-i18next';
import styles from './Lamp.module.css';

export enum LampColour {
  Permission = 'permission',
  Input = 'input',
  Turn = 'turn',
  Off = 'off',
}

const GLYPH: Record<LampColour, string> = { permission: '!', input: '?', turn: '•', off: '' };
const MEANING = {
  permission: 'lamp.permission',
  input: 'lamp.input',
  turn: 'lamp.turn',
  off: 'lamp.off',
} as const satisfies Record<LampColour, string>;
const SHORT_MEANING = {
  permission: 'lamp.shortPermission',
  input: 'lamp.shortInput',
  turn: 'lamp.shortTurn',
} as const satisfies Record<Exclude<LampColour, LampColour.Off>, string>;

interface Props {
  colour: LampColour;
  size: 'marker' | 'tile';
  number?: string;
}

/** A session's state as a lamp: a marker beside a ticket, or a tile on the andon board. Its colour means session state and nothing else. */
export const Lamp = ({ colour, size, number }: Props) => {
  const { t } = useTranslation('designSystem');
  if (size === 'marker') {
    return (
      <span className={`${styles.marker} ${styles[colour]}`} role="img" aria-label={t(MEANING[colour])}>
        {GLYPH[colour]}
      </span>
    );
  }
  return (
    <span className={`${styles.tile} ${styles[colour]}`} role="img" aria-label={t('lamp.tile', { number: number ?? t('lamp.noTicket'), meaning: t(MEANING[colour]) })}>
      <b className={styles.number}>{number ?? t('lamp.noNumber')}</b>
      <i className={styles.meaning}>{colour === LampColour.Off ? null : t(SHORT_MEANING[colour])}</i>
    </span>
  );
};
