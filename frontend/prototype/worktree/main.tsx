// PROTOTYPE: throwaway switcher for where a done ticket's worktree and its remove action sit in the ticket pane. Not shipped.
import { StrictMode, useEffect, useState, type ReactNode } from 'react';
import { createRoot } from 'react-dom/client';
import '../../src/design-system/global.css';
import '../../src/i18n/i18n';
import '../ux-system/switcher.css';
import './worktree.css';
import { BoardPane, TicketPane } from '../../src/board/BoardScreen';
import { BoardStore } from '../../src/board/boardStore';
import { Selection } from '../../src/board/selection';
import { PullRequestRead } from '../../src/board/ticketPullRequest';
import { Button, ButtonTone, FactsLine, KeyBar, LampColour, Popup, ReadingSection, Rows, SessionRow, Text, Tone } from '../../src/design-system';
import { NewSessionBar } from '../../src/session/SessionsList';
import { NewRow } from '../../src/session/sessionRows';
import { FakeClient } from '../../src/test/fakeClient';
import { BOARD, PATH, SELECTED, STATES, STATE_WORDS, isClear, type WorktreeCase } from './data';

const VARIANTS = ['W0', 'W1', 'W2', 'W3'] as const;
type Variant = (typeof VARIANTS)[number];
const NAMES: Record<Variant, string> = { W0: 'Today: no worktree', W1: 'Worktree section, one row', W2: 'Second facts line', W3: 'Pinned above new session' };

const params = new URLSearchParams(location.search);

const client = new FakeClient();
const board = new BoardStore(client);
const selection = new Selection();
void board.load();
client.answer('board/watch', BOARD);
selection.select(SELECTED);

const Sessions = () => (
  <ReadingSection title="sessions">
    <Rows>
      <SessionRow label="211 done" state="ended" action={{ label: 'resume', shortcut: 'enter' }} selected={false} lamp={undefined} onSelect={() => undefined} />
      <SessionRow label="211 start" state="ended" action={{ label: 'resume', shortcut: 'enter' }} selected={false} lamp={undefined} onSelect={() => undefined} />
    </Rows>
  </ReadingSection>
);

/** W1: a section of its own after the sessions, one row like theirs: a pill, the path, and the action while it is clear. */
const SectionRow = ({ state }: { state: WorktreeCase }) => (
  <ReadingSection title="worktree">
    <Rows>
      <SessionRow
        label={PATH}
        state={isClear(state) ? 'clean' : state === 'reading' ? 'reading' : 'held'}
        action={isClear(state) ? { label: 'remove', shortcut: 'd' } : undefined}
        selected={false}
        lamp={isClear(state) ? LampColour.Turn : LampColour.Off}
        onSelect={() => undefined}
      />
    </Rows>
    {isClear(state) || state === 'reading' ? null : (
      <div className="wt-why">
        <Text tone={Tone.Dim} tight>
          cannot remove: {STATE_WORDS[state]}
        </Text>
      </div>
    )}
  </ReadingSection>
);

/** W2: a second facts line under the first, the button at its end while it is clear. */
const FactsRow = ({ state }: { state: WorktreeCase }) => (
  <div className="wt-facts">
    <FactsLine
      facts={[
        { label: 'worktree', value: PATH },
        { label: 'state', value: STATE_WORDS[state], tone: isClear(state) ? undefined : Tone.Dim },
      ]}
    />
    {isClear(state) ? (
      <Button tone={ButtonTone.Quiet} chip="d" onClick={() => undefined}>
        Remove worktree
      </Button>
    ) : null}
  </div>
);

/** W3: a strip pinned above the new session bar, out of the scroll, so the action is where the ticket's other action is. */
const FootStrip = ({ state }: { state: WorktreeCase }) => (
  <div className="wt-strip">
    <span className="wt-strip-text">
      <span className="wt-strip-label">worktree</span>
      <span className="wt-strip-path">{PATH}</span>
      <span className={isClear(state) ? 'wt-strip-state' : 'wt-strip-state wt-held'}>{STATE_WORDS[state]}</span>
    </span>
    {isClear(state) ? (
      <Button tone={ButtonTone.Quiet} chip="d" onClick={() => undefined}>
        Remove
      </Button>
    ) : null}
  </div>
);

const Window = ({ variant, state }: { variant: Variant; state: WorktreeCase }) => {
  const newBar = <NewSessionBar rows={[new NewRow()]} cursor={-1} picker={undefined} onPress={() => undefined} onPickVerb={() => undefined} onDismiss={() => undefined} />;
  const foot: ReactNode =
    variant === 'W3' ? (
      <div className="wt-foot">
        <FootStrip state={state} />
        {newBar}
      </div>
    ) : (
      newBar
    );
  const lead: ReactNode =
    variant === 'W1' ? (
      <>
        <Sessions />
        <SectionRow state={state} />
      </>
    ) : variant === 'W2' ? (
      <>
        <FactsRow state={state} />
        <Sessions />
      </>
    ) : (
      <Sessions />
    );
  const keys = ['j/k move', 'enter open', 'n/N new', ...(variant !== 'W0' && isClear(state) ? ['d remove worktree'] : []), 'c capture', '? help'];
  return (
    <div className="wt-window">
      <div className="wt-columns">
        <BoardPane
          board={board}
          selection={selection}
          focused
          markerOf={() => undefined}
          onOpen={() => undefined}
          onFocus={() => undefined}
          onRetry={() => undefined}
          onCapture={() => undefined}
          panelTabs={{ tabs: [{ id: 'board', label: 'Board', count: 4 }, { id: 'docs', label: 'Docs', count: 124 }], selected: 'board', onSelect: () => undefined }}
        />
        <TicketPane board={board} selection={selection} focused={false} pullRequest={{ read: PullRequestRead.None, number: undefined }} onLink={() => undefined} foot={foot}>
          {lead}
        </TicketPane>
      </div>
      <KeyBar keys={keys} />
      {state === 'confirm' && variant !== 'W0' ? (
        <Popup title="Remove this worktree?" main={{ label: 'Remove', chip: 'y', onClick: () => undefined }} out={{ label: 'Keep', chip: 'esc', onClick: () => undefined }} onDismiss={() => undefined}>
          <Text tight mono>
            {PATH}
          </Text>
          <Text tone={Tone.Dim} tight>
            Nothing in it is lost: no uncommitted work, no commit beyond the merge, no live session. The branch stays.
          </Text>
        </Popup>
      ) : null}
    </div>
  );
};

const Prototype = () => {
  const [index, setIndex] = useState(() => Math.max(0, VARIANTS.findIndex((v) => v === params.get('variant'))));
  const [state, setState] = useState<WorktreeCase>(() => STATES.find((s) => s === params.get('state')) ?? 'clean');
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
