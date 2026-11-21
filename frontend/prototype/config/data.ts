// PROTOTYPE: canned configuration for the editor variants. Not shipped.
export const STATES = ['menu', 'clean', 'recording', 'edited', 'refused', 'stale', 'invalid-file'] as const;
export type ConfigState = (typeof STATES)[number];

export enum Reach {
  Both = 'both',
  Desktop = 'desktop',
  Tui = 'TUI',
}

export interface ChordRow {
  key: string;
  action: string;
  chord: string;
  reach: Reach;
  set: boolean;
}

export const CHORDS: readonly ChordRow[] = [
  { key: 'focusLeft', action: 'Focus left', chord: 'alt+h', reach: Reach.Both, set: false },
  { key: 'focusDown', action: 'Focus down', chord: 'alt+j', reach: Reach.Both, set: false },
  { key: 'focusUp', action: 'Focus up', chord: 'alt+k', reach: Reach.Both, set: false },
  { key: 'focusRight', action: 'Focus right', chord: 'alt+l', reach: Reach.Both, set: false },
  { key: 'hideMainView', action: 'Hide main view', chord: 'alt+m', reach: Reach.Both, set: false },
  { key: 'zoom', action: 'Zoom', chord: 'ctrl+alt+z', reach: Reach.Both, set: true },
  { key: 'nextWaiting', action: 'Next waiting session', chord: 'alt+w', reach: Reach.Both, set: false },
  { key: 'close', action: 'Close session', chord: 'alt+x', reach: Reach.Desktop, set: false },
];

export type StatusKey = 'draft' | 'todo' | 'in-progress' | 'in-review' | 'done';

export interface VerbsRow {
  status: StatusKey;
  colour: string;
  on: readonly string[];
  off: readonly string[];
  set: boolean;
}

export const VERBS: readonly VerbsRow[] = [
  { status: 'draft', colour: 'var(--status-draft)', on: ['refine'], off: ['review', 'respond', 'done'], set: false },
  { status: 'todo', colour: 'var(--status-todo)', on: ['start'], off: ['review', 'respond', 'done'], set: false },
  { status: 'in-progress', colour: 'var(--status-in-progress)', on: ['review', 'design'], off: ['respond', 'done'], set: false },
  { status: 'in-review', colour: 'var(--status-in-review)', on: ['done', 'respond'], off: ['review'], set: true },
  { status: 'done', colour: 'var(--status-done)', on: ['done'], off: [], set: false },
];

export const PATH = '~/.config/lazyclaude/config.yml';
