import type { ReactNode } from 'react';
import styles from './Button.module.css';

interface Props {
  onClick: () => void;
  children: ReactNode;
}

export function Button({ onClick, children }: Props) {
  return (
    <button type="button" className={styles.button} onClick={onClick}>
      {children}
    </button>
  );
}
