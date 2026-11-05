import '@xterm/xterm/css/xterm.css';
import type { Ref } from 'react';
import styles from './TerminalSurface.module.css';

interface Props {
  label: string;
  ref: Ref<HTMLDivElement>;
  onFocus: () => void;
}

/** The place a pane's terminal draws: `claude`'s own screen on the dock's darker ground. The window-level keys leave what is typed inside it to the terminal. */
export const TerminalSurface = ({ label, ref, onFocus }: Props) => {
  return <div ref={ref} className={styles.surface} data-terminal role="group" aria-label={label} onFocus={onFocus} />;
};
