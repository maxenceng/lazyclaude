// PROTOTYPE: the Pull requests tab and the review it opens: R0 as the app draws it today, R1–R3 three answers. Not shipped.
import { Fragment, useLayoutEffect, useRef, type ReactNode } from 'react';
import type { Pane } from './data';
import { Inline } from './Inline';
import { CI_GLYPH, CI_WORD, CURSOR_LINE, DIFF, FILES, PENDING_REVIEW, PULL_REQUESTS, THREADS, fileDir, fileName, threadById, type DiffRow, type Thread } from './reviewData';
import { Frame, Icon } from './VariantB';
import './review.css';

export type ReviewLayout = 'r0' | 'r1' | 'r2' | 'r3' | 'r4';
export type PrView = 'off' | 'list' | 'review';

interface Props {
  variant: ReviewLayout;
  view: PrView;
  focus: Pane;
  empty: boolean;
  leftTab: 'files' | 'threads';
  onOpen: () => void;
}

/** Live keys only, per variant and pane. */
export const reviewKeys = (variant: ReviewLayout, view: PrView, focus: Pane, empty: boolean, leftTab: 'files' | 'threads'): string[] => {
  if (view === 'list') return empty ? ['1/2/3 tabs'] : ['j/k move', 'enter review', '1/2/3 tabs'];
  if (variant === 'r0') {
    if (focus === 'board') return ['j/k move', 'enter go to line', 'S submit', 'r resolved', 'R refresh', 'esc back'];
    if (focus === 'docs') return ['c reply', 'enter open', 'x delete', 'S submit', 'j/k move', 'r resolved', 'R refresh', 'esc back'];
    return ['c comment', 'v range', 'enter edit', 'x delete', 'S submit', 'j/k line', 'J/K file', '^d/^u page', 'r resolved', 'R refresh', 'esc back'];
  }
  const diff = ['j/k line', 'J/K file', 'c comment', 'v range', 'enter reply', 'r resolve', 'S submit', 'esc back'];
  if (focus === 'ticket' || focus === 'dock') return variant === 'r2' ? [...diff.slice(0, 2), 't next thread', ...diff.slice(2)] : diff;
  if (variant === 'r3') return leftTab === 'files' ? ['j/k move', 'enter go to file', 'o threads', 'S submit', 'esc back'] : ['j/k move', 'enter go to thread', 'c reply', 'r resolve', 'o files', 'S submit', 'esc back'];
  if (variant === 'r2' && focus === 'docs') return ['j/k move', 'enter go to line', 'c reply', 'r resolve', 'x delete', 'S submit', 'esc back'];
  return ['j/k move', 'enter go to file', 'S submit', 'esc back'];
};

const ciBadge = (ci: keyof typeof CI_WORD, withWord: boolean) => (
  <span className={`rv-ci rv-ci-${ci}`}>
    {CI_GLYPH[ci]}
    {withWord && <span> {CI_WORD[ci]}</span>}
  </span>
);

/* ---------- the list ---------- */

const PrList = ({ variant, focus, empty, onOpen }: Props) => {
  const list = empty ? [] : PULL_REQUESTS;
  const groups = (
    <div className="rv-groups">
      {(['failed', 'pending', 'passed', 'none'] as const).map((ci) => {
        const inGroup = list.filter((p) => p.ci === ci);
        return (
          <section key={ci}>
            <h3 className={`rv-ci-${ci}`}>
              {CI_GLYPH[ci]} {ci === 'none' ? 'no checks' : CI_WORD[ci]} <span>{inGroup.length}</span>
            </h3>
            {inGroup.length === 0 && <p className="rv-group-none">none</p>}
            {inGroup.map((pr) => (
              <div key={pr.number} className={`rv-card2 ${pr.number === 217 ? 'rv-sel' : ''}`} onClick={onOpen}>
                <div className="rv-card2-top">
                  <span className="rv-ticket">{pr.ticket}</span>
                  <span className="rv-title">
                    <Inline text={pr.title} />
                  </span>
                </div>
                <div className="rv-meta">
                  <span className="rv-num">#{pr.number}</span>
                  <span className="rv-head">{pr.head}</span>
                  <span>{pr.comments === 0 ? 'no comments' : `${pr.comments} comment${pr.comments === 1 ? '' : 's'}`}</span>
                  <span>{pr.age}</span>
                </div>
              </div>
            ))}
          </section>
        );
      })}
    </div>
  );
  const body =
    variant === 'r4' ? (
      <>
        {list.length === 0 && <p className="rv-none">No open pull requests.</p>}
        {groups}
      </>
    ) : list.length === 0 ? (
      <p className="rv-none">No open pull requests.</p>
    ) : variant === 'r0' ? (
      <div className="rv-list0">
        {list.map((pr, i) => (
          <div key={pr.number} className={`rv-row0 ${i === 0 ? 'rv-sel' : ''}`} onClick={onOpen}>
            {ciBadge(pr.ci, false)}
            <span className="rv-num">#{pr.number}</span>
            <span className="rv-title">
              <Inline text={pr.title} />
            </span>
            <span className="rv-head">{pr.head}</span>
          </div>
        ))}
      </div>
    ) : variant === 'r2' ? (
      groups
    ) : (
      <div className={`rv-table rv-table-${variant}`}>
        <div className="rv-thead">
          <span>CI</span>
          <span>ticket</span>
          <span>title</span>
          <span>branch</span>
          <span>comments</span>
          <span>age</span>
        </div>
        {list.map((pr, i) => (
          <div key={pr.number} className={`rv-trow rv-stripe-${pr.ci} ${i === 0 ? 'rv-sel' : ''}`} onClick={onOpen}>
            {ciBadge(pr.ci, true)}
            <span className="rv-ticket">{pr.ticket}</span>
            <span className="rv-title">
              <Inline text={pr.title} />
              <small>#{pr.number}</small>
            </span>
            <span className="rv-head">{pr.head}</span>
            <span className="rv-count">{pr.comments === 0 ? '—' : `💬 ${pr.comments}`}</span>
            <span className="rv-age">{pr.age}</span>
          </div>
        ))}
      </div>
    );
  const first = list[0];
  return (
    <main className={`rv-listmain ${variant === 'r3' ? 'rv-listmain-side' : ''}`}>
      <Frame pane="board" focus={focus === 'ticket' && variant !== 'r3' ? 'board' : focus} title="Pull requests" sub={list.length === 0 ? undefined : <span className="vb-sub">{list.length} open</span>}>
        {body}
      </Frame>
      {variant === 'r3' && (
        <Frame pane="ticket" focus={focus} title="Pull request" sub={first === undefined ? undefined : <span className="vb-sub">#{first.number}</span>}>
          {first === undefined ? (
            <p className="rv-none">No pull request selected.</p>
          ) : (
            <article className="rv-prside">
              <p className="vb-kicker">
                {first.ticket} · #{first.number}
              </p>
              <h1>
                <Inline text={first.title} />
              </h1>
              <dl className="vl-facts rv-prfacts">
                <div>
                  <dt>checks</dt>
                  <dd>{ciBadge(first.ci, true)}</dd>
                </div>
                <div>
                  <dt>branch</dt>
                  <dd>{first.head}</dd>
                </div>
                <div>
                  <dt>files</dt>
                  <dd>
                    {FILES.length} · <span className="rv-plus">+196</span> <span className="rv-minus">−5</span>
                  </dd>
                </div>
                <div>
                  <dt>threads</dt>
                  <dd>1 open · 1 resolved</dd>
                </div>
                <div>
                  <dt>your pending review</dt>
                  <dd>{PENDING_REVIEW.count} comment</dd>
                </div>
              </dl>
              <button className="rv-submit rv-open" onClick={onOpen}>
                Open the review <kbd>enter</kbd>
              </button>
            </article>
          )}
        </Frame>
      )}
    </main>
  );
};

/* ---------- the review ---------- */

const CHANGE_WORD = { A: 'added', M: 'modified', D: 'deleted', R: 'renamed' };

const FileList = ({ selected, tree }: { selected: number; tree: boolean }) => (
  <div className="rv-files">
    {FILES.map((f, i) => (
      <div key={f.path} className={`rv-file ${i === selected ? 'rv-sel' : ''}`} title={f.path}>
        <span className={`rv-change rv-change-${f.change}`} title={CHANGE_WORD[f.change]}>
          {f.change}
        </span>
        <span className="rv-fname">
          {fileName(f.path)}
          {tree && <small>{fileDir(f.path).replace('src/main/java/dev/maxenceng/lazyclaude/', '…/')}</small>}
        </span>
        {tree && (
          <span className="rv-delta">
            <b className="rv-plus">+{f.added}</b> <b className="rv-minus">−{f.removed}</b>
          </span>
        )}
        {f.threads > 0 && <span className="rv-tcount">💬{f.threads}</span>}
        {f.pending > 0 && <span className="rv-pcount">✎{f.pending}</span>}
      </div>
    ))}
  </div>
);

const ThreadCard = ({ thread, compact, current }: { thread: Thread; compact: boolean; current?: boolean }) => {
  const file = FILES[thread.file]!;
  const where = `${fileName(file.path)}:${thread.line}`;
  if (thread.resolved && compact) {
    return (
      <div className="rv-thread rv-resolved">
        <span className="rv-ok">✓ resolved</span> <span className="rv-where">{where}</span> · {thread.comments[0]!.author}: <Inline text={thread.comments[0]!.body.slice(0, 64) + '…'} />
        <span className="rv-spring" />
        <kbd>enter</kbd> show
      </div>
    );
  }
  return (
    <div className={`rv-thread ${thread.pending ? 'rv-pending' : ''} ${thread.resolved ? 'rv-resolved-open' : ''} ${current ? 'rv-current' : ''}`}>
      <div className="rv-thread-head">
        <span className="rv-where">{where}</span>
        {thread.pending ? <span className="rv-tag rv-tag-pending">pending · not submitted</span> : thread.resolved ? <span className="rv-tag rv-tag-ok">✓ resolved</span> : <span className="rv-tag">open · {thread.comments.length} comments</span>}
      </div>
      {thread.comments.map((c, i) => (
        <div key={i} className="rv-comment">
          <b>{c.author}</b> <small>{c.age}</small>
          <p>
            <Inline text={c.body} />
          </p>
        </div>
      ))}
      <div className="rv-thread-actions">
        {thread.pending ? (
          <>
            <span>
              <kbd>enter</kbd> edit
            </span>
            <span>
              <kbd>x</kbd> delete
            </span>
          </>
        ) : (
          <>
            <span className="rv-reply-box">Reply…</span>
            <span>
              <kbd>c</kbd> reply
            </span>
            <span>
              <kbd>r</kbd> {thread.resolved ? 'unresolve' : 'resolve'}
            </span>
          </>
        )}
      </div>
    </div>
  );
};

/** The today annotation: a boxed block under the line, comments listed, a Reply button. */
const Annotation0 = ({ thread }: { thread: Thread }) => (
  <div className={`rv-ann0 ${thread.pending ? 'rv-ann0-pending' : ''} ${thread.resolved ? 'rv-ann0-dim' : ''}`}>
    <div className="rv-ann0-head">L{thread.line}</div>
    {thread.comments.map((c, i) => (
      <div key={i}>
        <b>{c.author}</b> <Inline text={c.body} />
      </div>
    ))}
    {!thread.pending && <button className="rv-ann0-reply">Reply</button>}
  </div>
);

interface DiffOptions {
  wrap: boolean;
  gutters: 'two' | 'one';
  threads: 'inline0' | 'inline' | 'marker' | 'margin';
  stickyFiles: boolean;
}

const lineClass = (row: Extract<DiffRow, { kind: 'line' }>) => `rv-line rv-${row.sign === '+' ? 'add' : row.sign === '-' ? 'del' : 'ctx'} ${row.new === CURSOR_LINE && row.sign === '+' ? 'rv-cursor' : ''}`;

const threadLines = new Map(THREADS.map((t) => [`${t.file}:${t.line}`, t]));

/** Margin notes start level with their line and are pushed down past the note above, never overlapping it. */
const useStackedNotes = (enabled: boolean) => {
  const ref = useRef<HTMLDivElement>(null);
  useLayoutEffect(() => {
    if (!enabled || ref.current === null) return;
    const gap = 8;
    let floor = -Infinity;
    ref.current.querySelectorAll<HTMLElement>('.rv-margin-note').forEach((note) => {
      note.style.transform = '';
      const top = note.getBoundingClientRect().top;
      const shift = Math.max(0, floor - top);
      note.style.transform = shift > 0 ? `translateY(${shift}px)` : '';
      floor = top + shift + note.getBoundingClientRect().height + gap;
    });
  });
  return ref;
};

const Diff = ({ o, currentThread }: { o: DiffOptions; currentThread?: string }) => {
  const ref = useStackedNotes(o.threads === 'margin');
  let file = -1;
  return (
    <div ref={ref} className={`rv-diff ${o.wrap ? 'rv-wrap' : 'rv-nowrap'} rv-g-${o.gutters} rv-t-${o.threads}`}>
      {DIFF.map((row, i) => {
        if (row.kind === 'file') {
          file = row.file;
          const f = FILES[row.file]!;
          return (
            <div key={i} className={`rv-filehead ${o.stickyFiles ? 'rv-sticky' : ''}`}>
              <span className={`rv-change rv-change-${f.change}`}>{f.change}</span>
              <span className="rv-fpath">
                {fileDir(f.path)}
                <b>{fileName(f.path)}</b>
              </span>
              <span className="rv-delta">
                <b className="rv-plus">+{f.added}</b> <b className="rv-minus">−{f.removed}</b>
              </span>
            </div>
          );
        }
        if (row.kind === 'hunk') {
          const [range, context] = [row.header.match(/@@[^@]*@@/)?.[0] ?? '', row.header.replace(/@@[^@]*@@\s*/, '')];
          return (
            <div key={i} className="rv-hunk">
              <span className="rv-hunk-range">{range}</span>
              {context !== '' && <span className="rv-hunk-ctx">{context}</span>}
            </div>
          );
        }
        if (row.kind === 'thread') {
          const t = threadById(row.thread);
          if (o.threads === 'inline0') return <Annotation0 key={i} thread={t} />;
          if (o.threads === 'inline')
            return (
              <div key={i} className="rv-inline">
                <ThreadCard thread={t} compact />
              </div>
            );
          return null;
        }
        const marked = row.new !== undefined ? threadLines.get(`${file}:${row.new}`) : undefined;
        return (
          <div key={i} className={`${lineClass(row)} ${marked !== undefined ? 'rv-has-thread' : ''}`}>
            {o.gutters === 'two' && <span className="rv-n">{row.old ?? ''}</span>}
            <span className="rv-n">{o.gutters === 'one' && row.sign === '-' ? <s>{row.old}</s> : (row.new ?? '')}</span>
            <span className="rv-sign">{row.sign}</span>
            <span className="rv-code">{row.text === '' ? ' ' : row.text}</span>
            {o.threads === 'marker' && marked !== undefined && <span className={`rv-marker ${marked.pending ? 'rv-marker-p' : marked.resolved ? 'rv-marker-r' : ''} ${marked.id === currentThread ? 'rv-marker-cur' : ''}`}>{marked.pending ? '✎' : '💬'}</span>}
            {o.threads === 'margin' && marked !== undefined && (
              <span className="rv-margin-note">
                <ThreadCard thread={marked} compact />
              </span>
            )}
          </div>
        );
      })}
    </div>
  );
};

const PendingBar = ({ wide }: { wide: boolean }) => (
  <div className={`rv-pendingbar ${wide ? 'rv-pendingbar-wide' : ''}`}>
    <span className="rv-tag rv-tag-pending">pending review</span>
    <span>
      {PENDING_REVIEW.count} comment · “{PENDING_REVIEW.summary}”
    </span>
    <span className="rv-spring" />
    <button className="rv-submit">
      Submit review <kbd>S</kbd>
    </button>
  </div>
);

const pr = PULL_REQUESTS[0]!;
const reviewTitle = (
  <span className="vb-sub">
    #{pr.number} · {pr.ticket} · {ciBadge(pr.ci, true)} · {pr.head}
  </span>
);

const DockStrip = () => (
  <div className="rv-dockstrip">
    SESSIONS <span className="rv-docktab">● 145 start</span> <span className="rv-docktab rv-dim">● 188 start</span>
    <span className="rv-spring" />
    <Icon glyph="restore" label="Unfold the dock (M)" />
  </div>
);

const ReviewView = ({ variant, focus, leftTab }: Props) => {
  if (variant === 'r0') {
    return (
      <main className="rv-review rv-r0">
        <Frame pane="board" focus={focus} title="Files" className="rv-a-files">
          <FileList selected={0} tree={false} />
        </Frame>
        <Frame pane="docs" focus={focus} title="Comments" className="rv-a-comments">
          <div className="rv-entries0">
            {[THREADS[2]!, THREADS[0]!, THREADS[1]!].map((t, i) => (
              <div key={t.id} className={`rv-entry0 ${i === 0 ? 'rv-sel' : ''} ${t.resolved ? 'rv-dim' : ''}`}>
                <span>{t.pending ? '✎' : '💬'}</span>
                <span className="rv-where">
                  {fileName(FILES[t.file]!.path)}:{t.line}
                </span>
                <span className="rv-dim">{t.comments.length > 1 ? `${t.comments.length - 1} replies · ${t.comments.at(-1)!.author}` : t.comments[0]!.author}</span>
              </div>
            ))}
          </div>
        </Frame>
        <Frame pane="dock" focus={'board' as Pane} title="Review" className="rv-a-panel">
          <div className="rv-panel0">
            <p>{PENDING_REVIEW.summary}</p>
            <p>{PENDING_REVIEW.count} pending</p>
          </div>
        </Frame>
        <Frame pane="ticket" focus={focus} title={`PR #${pr.number} pty4j leaves a /tmp/pty4j-linux-x86-64-* directory behind on every run`} className="rv-a-diff">
          <Diff o={{ wrap: false, gutters: 'two', threads: 'inline0', stickyFiles: false }} />
        </Frame>
        <DockStrip />
      </main>
    );
  }
  if (variant === 'r1' || variant === 'r4') {
    return (
      <main className="rv-review rv-r1">
        <Frame pane="board" focus={focus} title="Files" sub={<span className="vb-sub">6 · +196 −5</span>} className="rv-a-files" footer={<PendingBar wide={false} />}>
          <FileList selected={0} tree />
        </Frame>
        <Frame pane="ticket" focus={focus} title="Review" sub={reviewTitle} className="rv-a-diff" actions={<Icon glyph="close" label="Close the review (esc)" />}>
          <Diff o={{ wrap: true, gutters: 'one', threads: 'inline', stickyFiles: true }} />
        </Frame>
        <DockStrip />
      </main>
    );
  }
  if (variant === 'r2') {
    return (
      <main className="rv-review rv-r2">
        <Frame pane="board" focus={focus} title="Files" className="rv-a-files">
          <FileList selected={0} tree={false} />
        </Frame>
        <Frame pane="ticket" focus={focus} title="Diff" sub={reviewTitle} className="rv-a-diff" actions={<Icon glyph="close" label="Close the review (esc)" />}>
          <Diff o={{ wrap: true, gutters: 'two', threads: 'marker', stickyFiles: true }} currentThread="t1" />
        </Frame>
        <Frame pane="docs" focus={focus} title="Threads" sub={<span className="vb-sub">3 · following the cursor</span>} className="rv-a-threads" footer={<PendingBar wide={false} />}>
          <div className="rv-threadcol">
            {[THREADS[0]!, THREADS[2]!, THREADS[1]!].map((t) => (
              <ThreadCard key={t.id} thread={t} compact={t.resolved} current={t.id === 't1'} />
            ))}
          </div>
        </Frame>
        <DockStrip />
      </main>
    );
  }
  return (
    <main className="rv-review rv-r3">
      <Frame
        pane="board"
        focus={focus}
        title=""
        className="rv-a-files rv-tabs-left"
        sub={
          <span className="vf-tabs">
            <button className={`vf-tab ${leftTab === 'files' ? 'vf-tab-on' : ''}`}>
              Files <small>6</small>
            </button>
            <button className={`vf-tab ${leftTab === 'threads' ? 'vf-tab-on' : ''}`}>
              Threads <small>3</small>
            </button>
          </span>
        }
      >
        {leftTab === 'files' ? (
          <FileList selected={0} tree />
        ) : (
          <div className="rv-threadcol">
            {THREADS.map((t) => (
              <ThreadCard key={t.id} thread={t} compact />
            ))}
          </div>
        )}
      </Frame>
      <Frame pane="ticket" focus={focus} title="Review" sub={reviewTitle} className="rv-a-diff" actions={<Icon glyph="close" label="Close the review (esc)" />}>
        <PendingBar wide />
        <Diff o={{ wrap: true, gutters: 'one', threads: 'margin', stickyFiles: true }} />
      </Frame>
      <DockStrip />
    </main>
  );
};

export const ReviewArea = (props: Props): ReactNode => <Fragment>{props.view === 'list' ? <PrList {...props} /> : <ReviewView {...props} />}</Fragment>;
