// PROTOTYPE: variant A, "Lit title". The focused pane's title row is lit; actions are labelled key buttons after the title. Not shipped.
import type { ReactNode } from 'react';
import { COUNTS, DOCK_TABS, DOCS, GROUPS, KEYS, LAMP_WORD, PROJECT, SELECTED_DOC, SELECTED_TICKET, TERMINAL, TICKET, TILES, type Pane } from './data';
import { DOC_PARAGRAPHS, Inline } from './Inline';
import type { VariantProps } from './main';
import './variantA.css';

const Action = ({ keyName, label }: { keyName: string; label: string }) => (
  <button className="va-action" title={`${label} (${keyName})`}>
    <kbd>{keyName}</kbd>
    {label}
  </button>
);

const PaneBox = ({ pane, focus, title, count, actions, children, className }: { pane: Pane; focus: Pane; title: string; count?: string; actions?: ReactNode; children: ReactNode; className?: string }) => (
  <section className={`va-pane ${pane === focus ? 'va-focused' : ''} ${className ?? ''}`}>
    <header className="va-title">
      <h2>{title}</h2>
      {count !== undefined && <span className="va-title-count">{count}</span>}
      <span className="va-actions">{actions}</span>
    </header>
    <div className="va-body">{children}</div>
  </section>
);

export const VariantA = ({ focus, documentShown }: VariantProps) => (
  <div className="va">
    <nav className="va-status">
      <div className="va-tabs">
        <button className="va-tab va-tab-on">Base</button>
        <button className="va-tab">
          Board <span className="va-tab-finding">1 in progress</span>
        </button>
        <button className="va-tab">
          Pull requests <span className="va-tab-finding">none open</span>
        </button>
      </div>
      <div className="va-tiles">
        {TILES.map((tile) => (
          <div key={tile.ticket} className={`va-tile va-lamp-${tile.lamp}`}>
            <span className="va-tile-number">{tile.ticket}</span>
            <span className="va-tile-note">{tile.note ?? LAMP_WORD[tile.lamp]}</span>
          </div>
        ))}
      </div>
      <div className="va-counts">
        {COUNTS.map((c) => (
          <span key={c.lamp} className={`va-count va-count-${c.lamp} ${c.count === 0 ? 'va-count-zero' : ''}`}>
            <i />
            {c.count} {c.word}
          </span>
        ))}
      </div>
      <span className="va-project">{PROJECT}</span>
      <button className="va-quit" title="Quit lazyclaude (q)">
        <kbd>q</kbd>Quit
      </button>
    </nav>

    <main className="va-grid">
      <div className="va-left">
        <PaneBox pane="board" focus={focus} title="Board" actions={<Action keyName="n" label="New draft" />}>
          {GROUPS.map((group) => (
            <div key={group.status} className="va-group">
              <div className="va-group-head">
                <span>{group.status}</span>
                <span>{group.count}</span>
              </div>
              {group.rows.map((row) => (
                <div key={row.id} className={`va-row ${row.id === SELECTED_TICKET ? 'va-selected' : ''} ${group.status === 'Done' ? 'va-done' : ''}`}>
                  <span className="va-id">{row.id}</span>
                  <span className="va-row-title">
                    <Inline text={row.title} />
                  </span>
                  {row.lamp !== undefined && <i className={`va-dot va-lamp-${row.lamp}`} />}
                </div>
              ))}
            </div>
          ))}
        </PaneBox>
        <PaneBox pane="docs" focus={focus} title="Docs">
          {DOCS.map((doc, index) => (
            <div key={index} className={`va-row va-doc ${index === SELECTED_DOC ? 'va-selected' : ''}`} style={{ paddingLeft: `${12 + doc.depth * 16}px` }}>
              {doc.folder !== undefined && <span className="va-fold">{doc.folder.open ? '▾' : '▸'}</span>}
              <span className="va-row-title">{doc.name}</span>
              {doc.folder !== undefined && <span className="va-id">{doc.folder.count}</span>}
            </div>
          ))}
        </PaneBox>
      </div>

      <div className="va-right">
        {documentShown ? (
          <PaneBox pane="ticket" focus={focus} title="Document" count="research/embedding-claude-on-a-pty.md" actions={<Action keyName="esc" label="Close document" />}>
            <article className="va-reading">
              <h1>Embedding Claude on a pty</h1>
              {DOC_PARAGRAPHS.map((p) => (
                <p key={p}>
                  <Inline text={p} />
                </p>
              ))}
            </article>
          </PaneBox>
        ) : (
          <PaneBox pane="ticket" focus={focus} title="Ticket" count={`${TICKET.id}`} actions={<Action keyName="m" label="Hide" />}>
            <article className="va-reading">
              <div className="va-chips">
                <span className="va-chip va-chip-status">{TICKET.status}</span>
                <span className="va-chip">{TICKET.context}</span>
                <span className="va-chip">{TICKET.type}</span>
              </div>
              <h1>
                <Inline text={TICKET.title} />
              </h1>
              <h3>Sessions</h3>
              {TICKET.sessions.map((s) => (
                <div key={s.name} className={`va-row va-session ${s.lamp === 'turn' ? 'va-selected' : ''}`}>
                  <i className={`va-dot va-lamp-${s.lamp}`} />
                  <span className="va-row-title">{s.name}</span>
                  <span className="va-id">
                    {s.state} · {s.age}
                  </span>
                </div>
              ))}
              <div className="va-row va-session va-new">+ new session</div>
              <h3>Acceptance criteria</h3>
              <ul>
                {TICKET.criteria.map((c) => (
                  <li key={c}>
                    <Inline text={c} />
                  </li>
                ))}
              </ul>
            </article>
          </PaneBox>
        )}
        <PaneBox
          pane="dock"
          focus={focus}
          title="Sessions"
          className="va-dock"
          actions={
            <>
              <span className="va-docktabs">
                {DOCK_TABS.map((tab, i) => (
                  <span key={tab.ticket} className={`va-docktab ${i === 0 ? 'va-docktab-on' : ''}`}>
                    <i className={`va-dot va-lamp-${tab.lamp}`} />
                    {tab.label}
                  </span>
                ))}
              </span>
              <Action keyName="alt+z" label="Zoom" />
              <Action keyName="M" label="Fold" />
            </>
          }
        >
          <pre className="va-terminal">{TERMINAL.join('\n')}</pre>
        </PaneBox>
      </div>
    </main>

    <footer className="va-keybar">
      {KEYS[focus].map((k) => {
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
