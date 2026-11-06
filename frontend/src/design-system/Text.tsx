import type { ReactNode } from 'react';
import styles from './Text.module.css';

export enum Tone {
  Plain = 'plain',
  Dim = 'dim',
  Warning = 'warning',
}

interface Props {
  tone?: Tone;
  tight?: boolean;
  mono?: boolean;
  children: ReactNode;
}

/** Words that are read; mono only for code and a session's lines. */
export const Text = ({ tone = Tone.Plain, mono = false, tight = false, children }: Props) => {
  return <p className={[styles[tone], mono ? styles.mono : '', tight ? styles.tight : ''].join(' ')}>{children}</p>;
};
