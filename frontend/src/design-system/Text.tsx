import type { ReactNode } from 'react';
import styles from './Text.module.css';

interface Props {
  tone?: 'plain' | 'dim' | 'warning';
  tight?: boolean;
  mono?: boolean;
  children: ReactNode;
}

/** Words that are read; mono only for code and a session's lines. */
export const Text = ({ tone = 'plain', mono = false, tight = false, children }: Props) => {
  return <p className={[styles[tone], mono ? styles.mono : '', tight ? styles.tight : ''].join(' ')}>{children}</p>;
};
