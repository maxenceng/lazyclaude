// PROTOTYPE: variant C, "Terminal boxes". lazygit-like boxes, mono throughout, actions as key hints in the bottom border, quit only as a key. Not shipped.
import type { ReactNode } from 'react';
import { COUNTS, DOCK_TABS, DOCS, GROUPS, KEYS, LAMP_WORD, PROJECT, SELECTED_DOC, SELECTED_TICKET, TERMINAL, TICKET, TILES, type Pane } from './data';
import { DOC_PARAGRAPHS, Inline } from './Inline';
import type { VariantProps } from './main';
import './variantC.css';

const Hint = ({ k, label }: { k: string; label: string }) => (
  <button className="vc-hint" title={label}>
    [<b>{k}</b>] {label}
  </button>
);

const Box = ({ pane, focus, n, title, extra, hints, children, className }: { pane: Pane; focus: Pane; n: number; title: string; extra?: ReactNode; hints?: ReactNode; children: ReactNode; className?: string }) => (
  <section className={`vc-box ${pane === focus ? 'vc-focused' : ''} ${className ?? ''}`}>
    <header className="vc-legend">
      <span className="vc-n">[{n}]</span> {title}
      {extra}
    </header>
    <div className="vc-body">{children}</div>
    {hints !== undefined && <footer className="vc-hints">{hints}</footer>}
  </section>
);

const mark = (selected: boolean) => (selected ? '›' : ' ');

export const VariantC = ({ focus, documentShown }: VariantProps) => (
  <div className="vc">
    <nav className="vc-status">
      <span className="vc-tab vc-tab-on">Base</span>
      <span className="vc-sep">│</span>
      <span className="vc-tab">
        Board <span className="vc-dimmed">in progress</span> 1
      </span>
      <span className="vc-sep">│</span>
      <span className="vc-tab">
        PRs <span className="vc-dimmed">open</span> 0
      </span>
      <span className="vc-sep">│</span>
      <span className="vc-dimmed">{PROJECT}</span>
      <span className="vc-spring" />
      {COUNTS.map((c) => (
        <span key={c.lamp} className={`vc-count vc-ink-${c.lamp} ${c.count === 0 ? 'vc-zero' : ''}`}>
          ● {c.count} {c.word}
        </span>
      ))}
      <span className="vc-sep">│</span>
      {TILES.map((tile) => (
        <span key={tile.ticket} className={`vc-tile vc-bg-${tile.lamp}`}>
          <b>{tile.ticket}</b>
          <small>{tile.note ?? LAMP_WORD[tile.lamp]}</small>
        </span>
      ))}
    </nav>

    <main className="vc-grid">
      <Box pane="board" focus={focus} n={1} title="Board" className="vc-board" hints={<Hint k="n" label="new draft" />}>
        {GROUPS.map((group) => (
          <div key={group.status}>
            <div className="vc-group" style={{ color: group.colour }}>
              ── {group.status} {group.count} ──
            </div>
            {group.rows.map((row) => {
              const selected = row.id === SELECTED_TICKET;
              return (
                <div key={row.id} className={`vc-row ${selected ? 'vc-selected' : ''} ${group.status === 'Done' ? 'vc-done' : ''}`}>
                  <span className="vc-mark">{mark(selected)}</span>
                  <span className="vc-id">{row.id}</span>
                  <span className="vc-row-title">
                    <Inline text={row.title} />
                  </span>
                  {row.lamp !== undefined && <span className={`vc-ink-${row.lamp}`}>●</span>}
                </div>
              );
            })}
          </div>
        ))}
      </Box>
      <Box pane="docs" focus={focus} n={2} title="Docs" className="vc-docs" hints={<Hint k="enter" label="open or fold" />}>
        {DOCS.map((doc, index) => {
          const selected = index === SELECTED_DOC;
          return (
            <div key={index} className={`vc-row ${selected ? 'vc-selected' : ''}`}>
              <span className="vc-mark">{mark(selected)}</span>
              <span className="vc-row-title">
                {'  '.repeat(doc.depth)}
                {doc.folder !== undefined ? (doc.folder.open ? '▾ ' : '▸ ') : '  '}
                {doc.name}
                {doc.folder !== undefined && <span className="vc-dimmed"> {doc.folder.count}</span>}
              </span>
            </div>
          );
        })}
      </Box>

      {documentShown ? (
        <Box pane="ticket" focus={focus} n={3} title="research/embedding-claude-on-a-pty.md" className="vc-main" hints={<Hint k="esc" label="close document" />}>
          <article className="vc-reading">
            <h1>Embedding Claude on a pty</h1>
            {DOC_PARAGRAPHS.map((p) => (
              <p key={p}>
                <Inline text={p} />
              </p>
            ))}
          </article>
        </Box>
      ) : (
        <Box
          pane="ticket"
          focus={focus}
          n={3}
          title={`Ticket ${TICKET.id}`}
          extra={
            <span className="vc-dimmed">
              {' '}
              · {TICKET.status} · {TICKET.context} · {TICKET.type}
            </span>
          }
          className="vc-main"
          hints={
            <>
              <Hint k="enter" label="attach" />
              <Hint k="n" label="new session" />
              <Hint k="m" label="hide" />
            </>
          }
        >
          <article className="vc-reading">
            <h1>
              <Inline text={TICKET.title} />
            </h1>
            <div className="vc-h">Sessions</div>
            {TICKET.sessions.map((s, i) => (
              <div key={s.name} className={`vc-row ${i === 0 ? 'vc-selected' : ''}`}>
                <span className="vc-mark">{mark(i === 0)}</span>
                <span className={`vc-ink-${s.lamp}`}>●</span>
                <span className="vc-row-title">{s.name}</span>
                <span className="vc-dimmed">
                  {s.state} {s.age}
                </span>
              </div>
            ))}
            <div className="vc-h">Acceptance criteria</div>
            <ul>
              {TICKET.criteria.map((c) => (
                <li key={c}>
                  <Inline text={c} />
                </li>
              ))}
            </ul>
          </article>
        </Box>
      )}

      <Box
        pane="dock"
        focus={focus}
        n={4}
        title="Sessions"
        extra={
          <>
            {DOCK_TABS.map((tab, i) => (
              <span key={tab.ticket} className={`vc-docktab ${i === 0 ? 'vc-docktab-on' : ''}`}>
                {' '}
                <span className={`vc-ink-${tab.lamp}`}>●</span> {tab.label}
              </span>
            ))}
          </>
        }
        className="vc-dock"
        hints={
          <>
            <Hint k="alt+z" label="zoom" />
            <Hint k="M" label="fold" />
          </>
        }
      >
        <pre className="vc-terminal">{TERMINAL.join('\n')}</pre>
      </Box>
    </main>

    <footer className="vc-keybar">
      {KEYS[focus].map((k) => {
        const [key, ...rest] = k.split(' ');
        return (
          <span key={k}>
            <b>{key}</b> {rest.join(' ')}
          </span>
        );
      })}
      <span className="vc-spring" />
      <span>
        <b>?</b> help
      </span>
      <span>
        <b>q</b> quit
      </span>
    </footer>
  </div>
);
