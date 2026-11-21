// PROTOTYPE: throwaway switcher for the configuration editor's variants, over the shipped window. Not shipped.
import { StrictMode, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import '../../src/design-system/global.css';
import '../../src/i18n/i18n';
import '../ux-system/switcher.css';
import '../worktree/worktree.css';
import './config.css';
import menuStyles from '../../src/design-system/WindowMenu.module.css';
import { BoardPane, TicketPane } from '../../src/board/BoardScreen';
import { BoardStore } from '../../src/board/boardStore';
import { Selection } from '../../src/board/selection';
import { PullRequestRead } from '../../src/board/ticketPullRequest';
import { KeyBar, StatusLine, WindowTabBar } from '../../src/design-system';
import { FakeClient } from '../../src/test/fakeClient';
import { BOARD, SELECTED } from '../worktree/data';
import { STATES, type ConfigState } from './data';
import { Editor, type Layout } from './Editor';

const VARIANTS = ['C0', 'C1', 'C2', 'C3'] as const;
type Variant = (typeof VARIANTS)[number];
const NAMES: Record<Variant, string> = { C0: 'Today: menu holds Quit', C1: 'One column, stacked', C2: 'Two columns, verbs beside', C3: 'Sections as tabs' };
const LAYOUT: Record<Exclude<Variant, 'C0'>, Layout> = { C1: 'stack', C2: 'split', C3: 'tabs' };

const params = new URLSearchParams(location.search);
const client = new FakeClient();
const board = new BoardStore(client);
const selection = new Selection();
void board.load();
client.answer('board/watch', BOARD);
selection.select(SELECTED);

const Menu = ({ open, withConfig }: { open: boolean; withConfig: boolean }) => (
  <div className={menuStyles.wrap}>
    <button type="button" className={menuStyles.button} aria-expanded={open}>
      <span className={menuStyles.name}>lazyclaude</span>
      <svg className={menuStyles.caret} viewBox="0 0 12 12" aria-hidden="true">
        <path d="M2 4 L6 8 L10 4" />
      </svg>
    </button>
    {open ? (
      <div className={menuStyles.list} role="menu">
        {withConfig ? (
          <button type="button" role="menuitem" className={menuStyles.item}>
            <span>Configuration</span>
            <kbd className={menuStyles.key}>,</kbd>
          </button>
        ) : null}
        <button type="button" role="menuitem" className={menuStyles.item}>
          <span>Quit</span>
          <kbd className={menuStyles.key}>q</kbd>
        </button>
      </div>
    ) : null}
  </div>
);

const Window = ({ variant, state }: { variant: Variant; state: ConfigState }) => {
  const menuOpen = variant === 'C0' || state === 'menu';
  const editing = variant !== 'C0' && state !== 'menu';
  const warning = state === 'invalid-file' ? { text: 'config.yml:7: unknown verb ‘ship’; keeping the last good configuration', count: 1 } : undefined;
  const keys = editing
    ? state === 'recording'
      ? ['press a chord to bind', 'esc stop recording']
      : ['j/k move', 'enter record / edit', 'space toggle', 'ctrl+←/→ move verb', 'd default', 'ctrl+s save', 'esc cancel']
    : ['j/k move', 'enter open', 'n/N new', 'c capture', ', configuration', '? help'];
  return (
    <div className="cf-window">
      <StatusLine
        menu={<Menu open={menuOpen} withConfig={variant !== 'C0'} />}
        tabs={<WindowTabBar tabs={[{ id: 'base', label: 'Base' }, { id: 'board', label: 'Board' }, { id: 'prs', label: 'Pull requests' }]} selected="base" onSelect={() => undefined} />}
        tiles={null}
        counts={null}
        project="lazyclaude"
        warning={warning}
      />
      <div className="wt-columns">
        <BoardPane
          board={board}
          selection={selection}
          focused={!editing}
          markerOf={() => undefined}
          onOpen={() => undefined}
          onFocus={() => undefined}
          onRetry={() => undefined}
          onCapture={() => undefined}
          panelTabs={{ tabs: [{ id: 'board', label: 'Board', count: 4 }, { id: 'docs', label: 'Docs', count: 124 }], selected: 'board', onSelect: () => undefined }}
        />
        <TicketPane board={board} selection={selection} focused={false} pullRequest={{ read: PullRequestRead.None, number: undefined }} onLink={() => undefined} foot={null}>
          {null}
        </TicketPane>
      </div>
      <KeyBar keys={keys} />
      {editing ? <Editor layout={LAYOUT[variant as Exclude<Variant, 'C0'>]} state={state} /> : null}
    </div>
  );
};

const Prototype = () => {
  const [index, setIndex] = useState(() => Math.max(0, VARIANTS.findIndex((v) => v === params.get('variant'))));
  const [state, setState] = useState<ConfigState>(() => STATES.find((s) => s === params.get('state')) ?? 'clean');
  const variant = VARIANTS[index]!;
  useEffect(() => {
    const url = new URL(location.href);
    url.searchParams.set('variant', variant);
    url.searchParams.set('state', state);
    history.replaceState(null, '', url);
  }, [variant, state]);
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.code === 'ArrowRight') setIndex((i) => (i + 1) % VARIANTS.length);
      else if (event.code === 'ArrowLeft') setIndex((i) => (i + VARIANTS.length - 1) % VARIANTS.length);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);
  return (
    <>
      <Window variant={variant} state={state} />
      {params.get('bare') === null ? (
        <div className="proto-switcher">
          <span className="proto-label">
            {variant} · {NAMES[variant]}
          </span>
          <span className="proto-sep" />
          {STATES.map((s) => (
            <button key={s} className={s === state ? 'proto-on' : ''} onClick={() => setState(s)}>
              {s}
            </button>
          ))}
        </div>
      ) : null}
    </>
  );
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Prototype />
  </StrictMode>,
);
