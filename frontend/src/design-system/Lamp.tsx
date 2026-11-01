import styles from './Lamp.module.css';

export type LampColour = 'permission' | 'input' | 'turn' | 'off';

const GLYPH: Record<LampColour, string> = { permission: '!', input: '?', turn: '•', off: '' };
const MEANING: Record<LampColour, string> = {
  permission: 'waiting for permission',
  input: 'waiting for input',
  turn: 'your turn',
  off: 'no session waits on you',
};

interface Props {
  colour: LampColour;
  size: 'marker' | 'tile';
  number?: string;
}

/** A session's state as a lamp: a marker beside a ticket, or a tile on the andon board. Its colour means session state and nothing else. */
export function Lamp({ colour, size, number }: Props) {
  if (size === 'marker') {
    return (
      <span className={`${styles.marker} ${styles[colour]}`} role="img" aria-label={MEANING[colour]}>
        {GLYPH[colour]}
      </span>
    );
  }
  return (
    <span className={`${styles.tile} ${styles[colour]}`} role="img" aria-label={`${number ?? 'no ticket'}: ${MEANING[colour]}`}>
      <b className={styles.number}>{number ?? '—'}</b>
      <i className={styles.meaning}>{colour === 'off' ? '' : MEANING[colour].replace('waiting for ', '')}</i>
    </span>
  );
}
