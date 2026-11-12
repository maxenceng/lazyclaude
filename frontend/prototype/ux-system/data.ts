// PROTOTYPE: throwaway specimen data, copied from the running window's screenshots. Not shipped.

export type Pane = 'board' | 'docs' | 'ticket' | 'dock';
export const PANES: Pane[] = ['board', 'docs', 'ticket', 'dock'];

export type Lamp = 'permission' | 'input' | 'turn' | 'off';

export interface Row {
  id: number;
  title: string;
  type: string;
  lamp?: Lamp;
}

export interface Group {
  status: string;
  rows: Row[];
  count: number;
  colour: string;
}

export const GROUPS: Group[] = [
  { status: 'In progress', count: 1, colour: 'var(--ansi-blue)', rows: [{ id: 188, title: 'A click on a status line lamp or count goes to its session', type: 'feature', lamp: 'input' }] },
  { status: 'In review', count: 0, colour: 'var(--ansi-magenta)', rows: [] },
  {
    status: 'Todo',
    count: 2,
    colour: 'var(--ansi-yellow)',
    rows: [
      { id: 145, title: 'pty4j leaves a `/tmp/pty4j-linux-x86-64-*` directory behind on every run', type: 'fix', lamp: 'turn' },
      { id: 146, title: "A pty spawned with `setInitialColumns(100)` reports `stty size` as `0 0`", type: 'fix' },
    ],
  },
  {
    status: 'Draft',
    count: 8,
    colour: 'var(--dim)',
    rows: [
      { id: 169, title: 'A pending comment is anchored against the diff before it is written', type: 'feature' },
      { id: 173, title: "The sidecar carries each live session's marker", type: 'feature' },
      { id: 179, title: 'The desktop frontend draws a mermaid diagram in a document', type: 'feature' },
      { id: 184, title: "A Configuration button edits config.yml: the TUI's options, the plugin's name and the verbs per status", type: 'feature' },
      { id: 185, title: 'The panes resize by dragging the seams between them', type: 'feature' },
      { id: 200, title: 'Improve UX globally', type: 'feature' },
      { id: 206, title: 'g, G and d act in the desktop window, or are no longer bound there', type: 'feature' },
      { id: 213, title: 'External links do not work. They do not open the browser.', type: 'fix' },
    ],
  },
  {
    status: 'Done',
    count: 99,
    colour: 'var(--ansi-green)',
    rows: [
      { id: 211, title: "The bundle job caches the Temurin JDK and AppRun downloads so a GitHub 504 does not fail it", type: 'chore' },
      { id: 189, title: 'Markdown stays readable on a large screen', type: 'feature' },
      { id: 203, title: 'See done tickets in the Base tab, like in the Board tab, to be able to trigger the done step', type: 'feature' },
    ],
  },
];

export interface DocNode {
  name: string;
  depth: number;
  folder?: { open: boolean; count: number };
}

export const DOCS: DocNode[] = [
  { name: 'README.md', depth: 0 },
  { name: 'context-map.md', depth: 0 },
  { name: 'glossary.md', depth: 0 },
  { name: 'adr', depth: 0, folder: { open: false, count: 103 } },
  { name: 'protocol', depth: 0, folder: { open: true, count: 1 } },
  { name: 'README.md', depth: 1 },
  { name: 'research', depth: 0, folder: { open: true, count: 12 } },
  { name: 'cross-platform-blockers.md', depth: 1 },
  { name: 'embedding-claude-on-a-pty.md', depth: 1 },
  { name: 'github-issues-capabilities.md', depth: 1 },
  { name: 'github-pr-review-api.md', depth: 1 },
  { name: 'jlink-sidecar-bundle.md', depth: 1 },
  { name: 'kitty-remote-control.md', depth: 1 },
  { name: 'session-discovery.md', depth: 1 },
  { name: 'roadmap', depth: 0, folder: { open: false, count: 1 } },
];

export const SELECTED_TICKET = 145;
export const SELECTED_DOC = 8;

export const TICKET = {
  id: 145,
  status: 'todo',
  context: 'session',
  type: 'fix',
  title: 'pty4j leaves a `/tmp/pty4j-linux-x86-64-*` directory behind on every run',
  criteria: [
    'A run of the jar (`make run`, or the dev relay’s `java -jar … --sidecar`) that spawns a hosted session creates no `pty4j-*` directory under `java.io.tmpdir`.',
    'pty4j’s natives are extracted under `$XDG_CACHE_HOME/lazyclaude/pty4j`, resolved like the existing cache files; lazyclaude creates that directory before the first spawn.',
    'When `pty4j.preferred.native.folder` is already set (the bundle, ADR 0059), lazyclaude leaves `pty4j.tmpdir` unset.',
    'Failing case: when the directory cannot be created, `pty4j.tmpdir` stays unset, sessions still start, and a warning names the directory and the reason.',
  ],
  sessions: [
    { name: '/ddd-workflow:ticket start 145', state: 'your turn', lamp: 'turn' as Lamp, age: '12m' },
    { name: '/ddd-workflow:ticket refine 145', state: 'ended', lamp: 'off' as Lamp, age: '2d' },
  ],
};

export interface Tile {
  ticket: number;
  lamp: Lamp;
  note?: string;
}

export const TILES: Tile[] = [
  { ticket: 145, lamp: 'turn' },
  { ticket: 188, lamp: 'input', note: 'needs input' },
  { ticket: 146, lamp: 'off' },
];

export const DOCK_TABS = [
  { ticket: 145, label: '145 start', lamp: 'turn' as Lamp },
  { ticket: 188, label: '188 start', lamp: 'input' as Lamp },
];

export const TERMINAL = [
  '✻ Welcome to Claude Code',
  '  cwd: ~/projects/lazyclaude/.worktrees/145',
  '',
  '> /ddd-workflow:ticket start 145',
  '',
  '● Reading the ticket… done. Branch 145-pty4j-tmpdir created.',
  '● Plan: resolve the cache dir in session.infrastructure.secondary,',
  '  set pty4j.tmpdir before the first spawn, lock it, warn on failure.',
  '',
  '> ',
];

export const KEYS: Record<Pane, string[]> = {
  board: ['j/k move', 'enter go to ticket', 'n new draft', '1/2/3 tabs'],
  docs: ['j/k move', 'enter open or fold', 'esc close', '1/2/3 tabs'],
  ticket: ['j/k move', 'enter attach', 'n new session', 'm hide', '1/2/3 tabs'],
  dock: ['alt+h/l tabs', 'alt+m hide main', 'alt+z zoom', 'M fold'],
};

export const PANE_TITLE: Record<Pane, string> = { board: 'Board', docs: 'Docs', ticket: 'Ticket', dock: 'Sessions' };

export const LAMP_WORD: Record<Lamp, string> = { permission: 'permission', input: 'needs input', turn: 'your turn', off: 'off' };

export const COUNTS: { lamp: Lamp; count: number; word: string }[] = [
  { lamp: 'permission', count: 0, word: 'permission' },
  { lamp: 'input', count: 1, word: 'input' },
  { lamp: 'turn', count: 1, word: 'your turn' },
];

export const PROJECT = '~/projects/lazyclaude';

/** Turns `code` spans into <code>; prototype-only, no markdown parser. */
export const spans = (text: string): (string | { code: string })[] =>
  text.split('`').map((part, index) => (index % 2 === 1 ? { code: part } : part));

export const NOTES = [
  'Found by the jlink research (`docs/research/jlink-sidecar-bundle.md`): `deleteOnExit` cannot remove a non-empty directory, and 26 were found in `/tmp`.',
  'Suggested fix: ship the natives pre-extracted and point pty4j at them (`-Dpty4j.preferred.native.folder`, `-Djna.boot.library.path`, `-Djna.nounpack=true`).',
  'Migrated from SESSION-15, created 2026-10-27.',
];

export const FACTS: [string, string][] = [
  ['status', 'todo'],
  ['type', 'fix'],
  ['context', 'session'],
  ['branch', '145-pty4j-tmpdir'],
  ['worktree', '.worktrees/145'],
  ['pull request', 'none yet'],
  ['links', 'jlink-sidecar-bundle.md · ADR 0059'],
];

export interface BoardCard {
  id: number;
  title: string;
  type: string;
  lamp?: Lamp;
  sessions: number;
  pr?: 'merged' | 'open';
}

export interface BoardColumn {
  status: string;
  colour: string;
  count: number;
  cards: BoardCard[];
}

/** The Board tab's columns, from the running window: Done shows its ten latest, as the real app does. */
export const BOARD_COLUMNS: BoardColumn[] = [
  {
    status: 'Draft',
    colour: 'var(--dim)',
    count: 8,
    cards: [
      { id: 169, title: 'A pending comment is anchored against the diff before it is written', type: 'feature', sessions: 0 },
      { id: 173, title: "The sidecar carries each live session's marker", type: 'feature', sessions: 0 },
      { id: 179, title: 'The desktop frontend draws a mermaid diagram in a document', type: 'feature', sessions: 0 },
      { id: 184, title: "A Configuration button edits config.yml: the TUI's options, the plugin's name and the verbs per status", type: 'feature', sessions: 0 },
      { id: 185, title: 'The panes resize by dragging the seams between them', type: 'feature', sessions: 0 },
      { id: 200, title: 'Improve UX globally', type: 'feature', sessions: 1 },
      { id: 206, title: 'g, G and d act in the desktop window, or are no longer bound there', type: 'feature', sessions: 0 },
      { id: 213, title: 'External links do not work. They do not open the browser.', type: 'fix', sessions: 0 },
    ],
  },
  {
    status: 'Todo',
    colour: 'var(--ansi-yellow)',
    count: 3,
    cards: [
      { id: 145, title: 'pty4j leaves a `/tmp/pty4j-linux-x86-64-*` directory behind on every run', type: 'fix', lamp: 'turn', sessions: 2 },
      { id: 146, title: "A pty spawned with `setInitialColumns(100)`/`setInitialRows(30)` reports `stty size` as `0 0`", type: 'fix', sessions: 0 },
      { id: 188, title: 'A click on a status line lamp or count goes to its session', type: 'feature', lamp: 'input', sessions: 1 },
    ],
  },
  { status: 'In progress', colour: 'var(--ansi-blue)', count: 0, cards: [] },
  { status: 'In review', colour: 'var(--ansi-magenta)', count: 0, cards: [] },
  {
    status: 'Done',
    colour: 'var(--ansi-green)',
    count: 99,
    cards: [
      { id: 211, title: "The bundle job caches the Temurin JDK and AppRun downloads so a GitHub 504 does not fail it", type: 'chore', sessions: 1 },
      { id: 189, title: 'Markdown stays readable on a large screen', type: 'feature', sessions: 2 },
      { id: 203, title: 'See done tickets in the Base tab, like in the Board tab, to be able to trigger the done step', type: 'feature', sessions: 2 },
      { id: 197, title: 'Design a better thread part on the pull request tab: see comments more clearly, answer existing ones, and move to a comment in one click', type: 'feature', sessions: 3 },
      { id: 83, title: 'The desktop frontend ships as a deb and an AppImage', type: 'feature', sessions: 4 },
      { id: 82, title: 'The desktop frontend captures a draft ticket', type: 'feature', sessions: 2 },
      { id: 193, title: "The desktop relay's test of lines written together fails about 6 times in 10 on main", type: 'fix', sessions: 1 },
      { id: 195, title: "Command markup bleeds into session titles, and old sessions pollute an in-progress ticket's sessions", type: 'fix', sessions: 2 },
      { id: 186, title: 'A shown document closes, and the dock minimises to its tabs', type: 'feature', sessions: 2 },
      { id: 187, title: 'Tabs switch the window between the base view, the Issues board and the pull requests', type: 'feature', sessions: 2 },
    ],
  },
];
