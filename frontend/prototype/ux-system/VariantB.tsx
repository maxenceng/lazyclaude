// PROTOTYPE: variant B, "Framed panes". Gutters between opaque panes, the focused one framed and lifted, icon actions in a fixed slot, quit in a window menu. Not shipped.
import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { COUNTS, DOCK_TABS, DOCS, FACTS, GROUPS, KEYS, NOTES, LAMP_WORD, PROJECT, SELECTED_DOC, SELECTED_TICKET, TERMINAL, TICKET, TILES, type Pane } from './data';
import { DOC_PARAGRAPHS, Inline } from './Inline';
import type { VariantProps } from './main';
import './variantB.css';

type Glyph = 'close' | 'hide' | 'minimise' | 'restore' | 'add' | 'zoom';

/** Each action its own shape: close is the only cross; hide slides the main view away; minimise folds down to a line. */
const PATHS: Record<Glyph, string> = {
  close: 'M3 3 L11 11 M11 3 L3 11',
  hide: 'M2 2 H12 V12 H2 Z M8 2 V12 M5.5 5.5 L3.5 7 L5.5 8.5',
  minimise: 'M2 11 H12',
  restore: 'M2 3 H12 V12 H2 Z M2 5.5 H12',
  add: 'M7 2 V12 M2 7 H12',
  zoom: 'M2 5 V2 H5 M9 2 H12 V5 M12 9 V12 H9 M5 12 H2 V9',
};

const Icon = ({ glyph, label }: { glyph: Glyph; label: string }) => (
  <button className="vb-icon" title={label} aria-label={label}>
    <svg viewBox="0 0 14 14" aria-hidden="true">
      <path d={PATHS[glyph]} />
    </svg>
  </button>
);

/** Hides every [data-row] whose bottom falls past the body's: whole rows only, never a half-cut last one. */
const useWholeRows = (enabled: boolean) => {
  const ref = useRef<HTMLDivElement>(null);
  useLayoutEffect(() => {
    const body = ref.current;
    if (!enabled || body === null) return undefined;
    const apply = () => {
      const limit = body.getBoundingClientRect().bottom + 0.5;
      body.querySelectorAll<HTMLElement>('[data-row]').forEach((row) => {
        row.style.visibility = row.getBoundingClientRect().bottom > limit ? 'hidden' : '';
      });
    };
    apply();
    const observer = new ResizeObserver(apply);
    observer.observe(body);
    return () => observer.disconnect();
  }, [enabled]);
  return ref;
};

const Frame = ({ pane, focus, title, sub, actions, children, className, wholeRows = false, footer }: { pane: Pane; focus: Pane; title: string; sub?: ReactNode; actions?: ReactNode; children: ReactNode; className?: string; wholeRows?: boolean; footer?: ReactNode }) => {
  const ref = useWholeRows(wholeRows);
  return (
    <section className={`vb-pane ${pane === focus ? 'vb-focused' : ''} ${className ?? ''}`}>
      <header className="vb-title">
        <h2>{title}</h2>
        {sub}
        <span className="vb-slot">{actions}</span>
      </header>
      <div className={`vb-body ${wholeRows ? 'vb-whole' : ''}`} ref={ref}>
        {children}
      </div>
      {footer}
    </section>
  );
};

/** The verbs a todo ticket's status offers, as `/ticket` accepts them; the first is the one `session` preselects. */
const VERBS = [
  { verb: 'start', note: 'branch, worktree and the first criterion' },
  { verb: 'design', note: 'the ticket branch touches frontend/**' },
];

/** Only keys that act on what the pane shows now. */
const liveKeys = (focus: Pane, documentShown: boolean, picking = false): string[] => {
  if (picking) return ['j/k move', 'enter start', 'esc cancel'];
  if (focus === 'docs') return ['j/k move', 'enter open or fold', ...(documentShown ? ['esc close document'] : []), '1/2/3 tabs'];
  if (focus === 'ticket') return documentShown ? ['j/k scroll', 'esc close document', '1/2/3 tabs'] : ['j/k move', 'enter attach', 'n new session', 'm hide', '1/2/3 tabs'];
  return KEYS[focus];
};

export const VariantB = (props: VariantProps) => <FramedWindow {...props} chosen={false} />;

export type Layout = 'd' | 'e' | 'f' | 'g';

export const FramedWindow = ({ focus, documentShown, chosen, layout = 'd' }: VariantProps & { chosen: boolean; layout?: Layout }) => {
  const [menu, setMenu] = useState(false);
  const [leftTab, setLeftTab] = useState<'board' | 'docs'>('board');
  const [doneOpen, setDoneOpen] = useState(false);
  useEffect(() => {
    if (focus === 'board' || focus === 'docs') setLeftTab(focus);
  }, [focus]);
  useEffect(() => {
    if (layout !== 'f') return undefined;
    const onKey = (event: KeyboardEvent) => {
      if (event.code === 'KeyO') setLeftTab((t) => (t === 'board' ? 'docs' : 'board'));
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [layout]);
  const leftFocused = focus === 'board' || focus === 'docs';
  const [picking, setPicking] = useState(() => new URLSearchParams(location.search).get('picker') === 'open');
  const [verb, setVerb] = useState(0);
  const pickerRef = useRef<HTMLDivElement>(null);
  const picks = chosen && !documentShown;
  const baseKeys = chosen ? liveKeys(focus, documentShown, picks && picking) : KEYS[focus];
  const keys = layout === 'f' && leftFocused ? [...baseKeys.slice(0, -1), leftTab === 'board' ? 'o docs' : 'o board', ...baseKeys.slice(-1)] : baseKeys;

  useEffect(() => {
    if (!picks) return undefined;
    const onKey = (event: KeyboardEvent) => {
      if (event.code === 'KeyN') {
        setPicking((open) => !open);
        setVerb(0);
      } else if (picking && (event.code === 'KeyJ' || event.code === 'ArrowDown')) setVerb((v) => Math.min(VERBS.length - 1, v + 1));
      else if (picking && (event.code === 'KeyK' || event.code === 'ArrowUp')) setVerb((v) => Math.max(0, v - 1));
      else if (picking && (event.code === 'Enter' || event.code === 'Escape')) setPicking(false);
    };
    const onDown = (event: MouseEvent) => {
      if (picking && pickerRef.current !== null && !pickerRef.current.contains(event.target as Node)) setPicking(false);
    };
    window.addEventListener('keydown', onKey);
    window.addEventListener('mousedown', onDown);
    return () => {
      window.removeEventListener('keydown', onKey);
      window.removeEventListener('mousedown', onDown);
    };
  }, [picks, picking]);
  const sessionsList = (
    <div className="vl-sessions">
      <h3>Sessions</h3>
      {TICKET.sessions.map((s, index) => {
        const waiting = s.lamp !== 'off';
        return (
          <button key={s.name} className={`vl-session ${waiting ? `vl-waiting vl-w-${s.lamp}` : ''} ${index === 0 ? 'vl-cursor' : ''}`} title={waiting ? 'Attach (enter)' : 'Resume (enter)'}>
            <span className={`vl-state vb-t-${s.lamp}`}>{LAMP_WORD[s.lamp] === 'off' ? s.state : LAMP_WORD[s.lamp]}</span>
            <span className="vl-session-name">{s.name}</span>
            <small>{s.age}</small>
            <span className="vl-attach">
              {waiting ? 'attach' : 'resume'} <kbd>enter</kbd>
            </span>
          </button>
        );
      })}
    </div>
  );
  const criteria = (
    <section>
      <h3>Acceptance criteria</h3>
      <ul>
        {TICKET.criteria.map((c) => (
          <li key={c}>
            <Inline text={c} />
          </li>
        ))}
      </ul>
    </section>
  );
  const notes = (
    <section>
      <h3>Notes</h3>
      {NOTES.map((n) => (
        <p key={n}>
          <Inline text={n} />
        </p>
      ))}
    </section>
  );
  const facts = (
    <dl className="vl-facts">
      {FACTS.map(([k, v]) => (
        <div key={k}>
          <dt>{k}</dt>
          <dd>{v}</dd>
        </div>
      ))}
    </dl>
  );
  const heading = (
    <>
      <p className="vb-kicker">#{TICKET.id}</p>
      <h1>
        <Inline text={TICKET.title} />
      </h1>
    </>
  );
  const ticketArticle =
    layout === 'e' ? (
      <article className="vb-reading ve-split">
        <div className="ve-wide">
          {heading}
          {sessionsList}
        </div>
        <div className="ve-prose">
          {criteria}
          {notes}
        </div>
        <aside className="ve-rail">{facts}</aside>
      </article>
    ) : layout === 'f' ? (
      <article className="vb-reading vf-wide">
        {heading}
        {sessionsList}
        <div className="vf-cols">
          {criteria}
          {notes}
        </div>
      </article>
    ) : layout === 'g' ? (
      <article className="vb-reading vg-centre">
        <header className="vg-head">
          {heading}
          {facts}
        </header>
        {sessionsList}
        <div className="vg-prose">
          {criteria}
          {notes}
        </div>
      </article>
    ) : (
            <article className="vb-reading">
              <p className="vb-kicker">#{TICKET.id}</p>
              <h1>
                <Inline text={TICKET.title} />
              </h1>
              <div className={`vb-sessions-list ${chosen ? 'vd-sessions-full' : ''}`}>
                {TICKET.sessions.map((s) => (
                  <div key={s.name} className={`vb-session vb-s-${s.lamp}`}>
                    <i className={`vb-dot vb-t-${s.lamp}`} />
                    <span>{s.name}</span>
                    <small>
                      {s.state} · {s.age}
                    </small>
                  </div>
                ))}
                {!chosen && (
                  <div className="vb-session vb-session-new">
                    <svg viewBox="0 0 14 14" aria-hidden="true">
                      <path d={PATHS.add} />
                    </svg>
                    new session
                  </div>
                )}
              </div>
              <h3>Acceptance criteria</h3>
              <ul>
                {TICKET.criteria.map((c) => (
                  <li key={c}>
                    <Inline text={c} />
                  </li>
                ))}
              </ul>
            </article>
    );

  const boardRows = (
    <>
          {GROUPS.map((group) => (
            <div key={group.status} className="vb-group" style={{ ['--g' as string]: group.colour }}>
              <div className={`vb-group-head ${layout === 'e' && group.status === 'Done' ? 've-fold' : ''}`} data-row onClick={() => group.status === 'Done' && setDoneOpen((o) => !o)}>
                <span>
                  {layout === 'e' && group.status === 'Done' ? (doneOpen ? '▾ ' : '▸ ') : ''}
                  {group.status}
                </span>
                <span>{group.count}</span>
              </div>
              {(layout !== 'e' || group.status !== 'Done' || doneOpen ? group.rows : []).map((row) => (
                <div key={row.id} data-row className={`vb-row ${row.id === SELECTED_TICKET ? 'vb-selected' : ''} ${group.status === 'Done' ? 'vb-done' : ''}`}>
                  <span className="vb-id">{row.id}</span>
                  <span className="vb-row-title">
                    <Inline text={row.title} />
                  </span>
                  <span className={`vb-type vb-type-${row.type}`}>{row.type}</span>
                  {row.lamp !== undefined &&
                (layout === 'd' || !chosen ? (
                  <i className={`vb-dot vb-t-${row.lamp}`} />
                ) : (
                  <span className={`vl-wait vb-t-${row.lamp}`}>{LAMP_WORD[row.lamp]}</span>
                ))}
                </div>
              ))}
            </div>
          ))}
    </>
  );
  const docsRows = (
    <>
          {DOCS.map((doc, index) => (
            <div key={index} data-row className={`vb-row ${index === SELECTED_DOC ? 'vb-selected' : ''}`} style={{ paddingLeft: `${12 + doc.depth * 16}px` }}>
              {doc.folder !== undefined && <span className="vb-fold">{doc.folder.open ? '▾' : '▸'}</span>}
              <span className="vb-row-title">{doc.name}</span>
              {doc.folder !== undefined && <span className="vb-id">{doc.folder.count}</span>}
            </div>
          ))}
    </>
  );

  return (
    <div className={`vb ${chosen ? `vd vl-${layout}` : ''}`}>
      <nav className="vb-status">
        <div className="vb-menu-wrap">
          <button className="vb-menu" onClick={() => setMenu((m) => !m)} aria-label="Window menu">
            <span className="vb-brand">lazyclaude</span>
            <svg viewBox="0 0 10 10" aria-hidden="true">
              <path d="M2 4 L5 7 L8 4" />
            </svg>
          </button>
          {menu && (
            <div className="vb-menu-list">
              <div>
                Settings… <kbd>,</kbd>
              </div>
              <div>
                Help <kbd>?</kbd>
              </div>
              <hr />
              <div className="vb-menu-quit">
                Quit lazyclaude <kbd>q</kbd>
              </div>
            </div>
          )}
        </div>
        <div className="vb-tabs">
          <button className="vb-tab vb-tab-on">Base</button>
          <button className="vb-tab">
            Board
            <span className="vb-badge vb-badge-blue" title="Tickets in progress">
              1 <small>in progress</small>
            </span>
          </button>
          <button className="vb-tab">
            Pull requests
            <span className="vb-badge" title="Open pull requests">
              0 <small>open</small>
            </span>
          </button>
        </div>
        <span className="vb-project">{PROJECT}</span>
        <div className="vb-sessions">
          {COUNTS.map((c) => (
            <span key={c.lamp} className={`vb-count vb-c-${c.lamp} ${c.count === 0 ? 'vb-zero' : ''}`} title={c.word}>
              {c.count} <small>{c.word}</small>
            </span>
          ))}
          <span className="vb-divider" />
          {TILES.map((tile) => (
            <div key={tile.ticket} className={`vb-tile vb-t-${tile.lamp}`}>
              <b>{tile.ticket}</b>
              <small>{tile.note ?? LAMP_WORD[tile.lamp]}</small>
            </div>
          ))}
        </div>
      </nav>

      <main className="vb-grid">
        {layout === 'f' ? (
          <Frame
            pane={leftFocused ? focus : leftTab}
            focus={focus}
            title=""
            className="vf-left"
            wholeRows
            sub={
              <span className="vf-tabs">
                <button className={`vf-tab ${leftTab === 'board' ? 'vf-tab-on' : ''}`} onClick={() => setLeftTab('board')} title="Board (o)">
                  Board <small>110</small>
                </button>
                <button className={`vf-tab ${leftTab === 'docs' ? 'vf-tab-on' : ''}`} onClick={() => setLeftTab('docs')} title="Docs (o)">
                  Docs <small>117</small>
                </button>
              </span>
            }
            actions={leftTab === 'board' ? <Icon glyph="add" label="New draft ticket (n)" /> : undefined}
          >
            {leftTab === 'board' ? boardRows : docsRows}
          </Frame>
        ) : layout === 'g' ? (
          <>
            <Frame pane="board" focus={focus} title="Board" className="vb-board vg-board" wholeRows actions={<Icon glyph="add" label="New draft ticket (n)" />}>
              {boardRows}
            </Frame>
            <div className={`vg-rail ${focus === 'docs' ? 'vg-open' : ''}`}>
              <button className="vg-strip" title="Docs (2)" aria-label="Docs (2)">
                <svg viewBox="0 0 14 14" aria-hidden="true">
                  <path d="M3 1.5 H8.5 L11 4 V12.5 H3 Z M8.5 1.5 V4 H11 M5 7 H9 M5 9.5 H9" />
                </svg>
                <span>Docs</span>
                <small>117</small>
              </button>
              {focus === 'docs' && (
                <Frame pane="docs" focus={focus} title="Docs" className="vg-docs" wholeRows>
                  {docsRows}
                </Frame>
              )}
            </div>
          </>
        ) : (
          <>
        <Frame pane="board" focus={focus} title="Board" className="vb-board" wholeRows={chosen} actions={<Icon glyph="add" label="New draft ticket (n)" />}>
          {boardRows}
        </Frame>
        <Frame pane="docs" focus={focus} title="Docs" className="vb-docs" wholeRows={chosen}>
          {docsRows}
        </Frame>
          </>

        )}

        {documentShown ? (
          <Frame pane="ticket" focus={focus} title="Document" className="vb-main" sub={<span className="vb-sub">research / embedding-claude-on-a-pty.md</span>} actions={<Icon glyph="close" label="Close document (esc)" />}>
            <article className="vb-reading">
              <h1>Embedding Claude on a pty</h1>
              {DOC_PARAGRAPHS.map((p) => (
                <p key={p}>
                  <Inline text={p} />
                </p>
              ))}
            </article>
          </Frame>
        ) : (
          <Frame
            pane="ticket"
            focus={focus}
            title="Ticket"
            className="vb-main"
            sub={
              <span className="vb-sub">
                <span className="vb-pill vb-pill-todo">todo</span>
                <span className="vb-pill">session</span>
                <span className="vb-pill vb-type-fix">fix</span>
              </span>
            }
            actions={<Icon glyph="hide" label="Hide the main view (m)" />}
            footer={
              chosen ? (
                <div className="vd-new-wrap" ref={pickerRef}>
                  {picking && (
                    <div className="vd-picker" role="listbox" aria-label="start with">
                      <div className="vd-picker-head">
                        <span>start with</span>
                        <small>
                          #{TICKET.id} · todo · worktree 145-pty4j-tmpdir
                        </small>
                      </div>
                      {VERBS.map((v, i) => (
                        <div key={v.verb} role="option" aria-selected={i === verb} className={`vd-verb ${i === verb ? 'vd-verb-on' : ''}`} onMouseEnter={() => setVerb(i)} onClick={() => setPicking(false)}>
                          <b>{v.verb}</b>
                          <code>/ddd-workflow:ticket {v.verb} {TICKET.id}</code>
                          <small>{v.note}</small>
                        </div>
                      ))}
                    </div>
                  )}
                  <button className={`vd-new ${picking ? 'vd-new-open' : ''}`} onClick={() => setPicking((open) => !open)} title="New session (n)">
                    <svg viewBox="0 0 14 14" aria-hidden="true">
                      <path d={PATHS.add} />
                    </svg>
                    new session
                    <kbd>n</kbd>
                  </button>
                </div>
              ) : undefined
            }
          >
            {ticketArticle}
          </Frame>
        )}

        <Frame
          pane="dock"
          focus={focus}
          title="Sessions"
          className="vb-dock"
          wholeRows={chosen}
          sub={
            <span className="vb-docktabs">
              {DOCK_TABS.map((tab, i) => (
                <span key={tab.ticket} className={`vb-docktab ${i === 0 ? 'vb-docktab-on' : ''}`}>
                  <i className={`vb-dot vb-t-${tab.lamp}`} />
                  {tab.label}
                </span>
              ))}
            </span>
          }
          actions={
            <>
              <Icon glyph="zoom" label="Zoom the dock (alt+z)" />
              <Icon glyph="minimise" label="Fold the dock to its tabs (M)" />
            </>
          }
        >
          <pre className="vb-terminal">
            {TERMINAL.map((line, i) => (
              <div key={i} data-row>
                {line === '' ? '\u00a0' : line}
              </div>
            ))}
          </pre>
        </Frame>
      </main>

      <footer className="vb-keybar">
        {keys.map((k) => {
          const [key, ...rest] = k.split(' ');
          return (
            <span key={k}>
              <kbd>{key}</kbd> {rest.join(' ')}
            </span>
          );
        })}
      </footer>
    </div>
  );
};
