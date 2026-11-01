import type { ReactNode } from 'react';
import styles from './Alert.module.css';

interface Props {
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}

/** A fault the user can act on: what failed, then the action. */
export function Alert({ title, children, action }: Props) {
  return (
    <div role="alert" className={styles.alert}>
      <strong className={styles.title}>{title}</strong>
      {children}
      {action}
    </div>
  );
}
