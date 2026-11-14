import styles from './Badge.module.css';

/** The badge's colour, named for the token it reads: an outline, an outline edged blue, or a filled amber. */
export enum BadgeTone {
  Plain = 'plain',
  Blue = 'blue',
  Amber = 'amber',
}

interface Props {
  tone?: BadgeTone;
  children: string;
}

/** A word in a pill, for what a row or a card carries beside its name: never a colour alone, since the word says what it is. */
export const Badge = ({ tone = BadgeTone.Plain, children }: Props) => {
  return (
    <span className={[styles.badge, styles[tone]].join(' ')} data-tone={tone}>
      {children}
    </span>
  );
};
