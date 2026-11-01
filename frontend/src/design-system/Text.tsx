import type { ReactNode } from 'react';
import styles from './Text.module.css';

interface Props {
  tone?: 'plain' | 'dim';
  mono?: boolean;
  children: ReactNode;
}

/** Words that are read; mono only for code and a session's lines. */
export function Text({ tone = 'plain', mono = false, children }: Props) {
  return <p className={[styles[tone], mono ? styles.mono : ''].join(' ')}>{children}</p>;
}
