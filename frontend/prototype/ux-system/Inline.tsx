// PROTOTYPE: renders `code` spans in specimen text. Not shipped.
import { spans } from './data';

export const Inline = ({ text }: { text: string }) => (
  <>{spans(text).map((part, index) => (typeof part === 'string' ? part : <code key={index}>{part.code}</code>))}</>
);

export const DOC_PARAGRAPHS = [
  'Claude Code is an interactive TUI: it expects a real terminal, reads the window size from the pty, and redraws on `SIGWINCH`. Hosting it means giving it one.',
  'pty4j spawns the child on a pseudo-terminal and hands back its streams; the adapter carries the bytes both ways and resizes the pty when the pane does.',
  'Two traps: a pty starts at `0 0` until the first resize, and a same-size resize after spawn is dropped by the kernel, so the first one must differ.',
];
