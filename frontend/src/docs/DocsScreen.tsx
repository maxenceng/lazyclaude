import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Alert, Busy, Button, Detail, DocsTreeRow, Document, Pane, PaneActionKind, type PanelTabsProps, Text, Tone, Rows } from '../design-system';
import { useStore } from '../shared/useStore';
import type { DocsController } from './docsController';
import { type DocsState, DocsStateKind } from './docsState';
import { RowKind } from './rows';
import { ShownKind } from './shownDocument';
import { type ScrollRequest, ScrollStep } from './documentScroll';

interface PaneProps {
  docs: DocsController;
  focused: boolean;
}

interface TreeProps extends PaneProps {
  /** The left column's panel tabs, which the Docs panel's title row shows in place of its title. */
  panelTabs: PanelTabsProps;
  onFocus: () => void;
  onRetry: () => void;
}

/** The Docs panel: the tree of `docs/`. A press on the selected row opens it, or folds it if it is a folder. */
export const DocsPane = ({ docs, focused, panelTabs, onFocus, onRetry }: TreeProps) => {
  const { t } = useTranslation('docs');
  const state = useStore(docs.store.state);
  useStore(docs.selection);
  useStore(docs.folded);
  const selected = docs.selectedKey();
  return (
    <Pane title={t('docs.title')} focused={focused} tabs={panelTabs}>
      {state.kind === DocsStateKind.Populated ? (
        <Rows>
          {docs.rows().map((row) => (
            <DocsTreeRow
              key={`${row.kind}:${row.path}`}
              name={row.name}
              depth={row.depth}
              selected={selected?.kind === row.kind && selected.path === row.path}
              folder={row.kind === RowKind.Folder ? { count: row.count, folded: row.folded } : undefined}
              onSelect={() => {
                onFocus();
                docs.press({ kind: row.kind, path: row.path });
              }}
            />
          ))}
        </Rows>
      ) : (
        <Unread state={state} onRetry={onRetry} />
      )}
    </Pane>
  );
};

const Unread = ({ state, onRetry }: { state: DocsState; onRetry: () => void }) => {
  const { t } = useTranslation('docs');
  switch (state.kind) {
    case DocsStateKind.Loading:
      return <Busy>{t('docs.loading')}</Busy>;
    case DocsStateKind.Empty:
      return <Text tone={Tone.Dim}>{t('docs.empty')}</Text>;
    case DocsStateKind.Unavailable:
      return (
        <>
          <Text>{t('docs.unavailable')}</Text>
          <Text tone={Tone.Dim}>{state.reason}</Text>
        </>
      );
    case DocsStateKind.Fault:
      return (
        <Alert title={t('docs.fault')} action={<Button onClick={onRetry}>{t('docs.retry')}</Button>}>
          {state.message}
        </Alert>
      );
    case DocsStateKind.Populated:
      return null;
  }
};

interface DocumentProps extends PaneProps {
  onFocus: () => void;
  onClose: () => void;
}

const HALVES = 2;
/** A pixel: the least a line step moves when the body's type has no measurable size, as in a layout-free render. */
const LEAST_STEP = 1;

/** How far one request moves the pane's body: a line of its text, or half of what it shows, either way down or up. */
const distanceOf = (request: ScrollRequest, body: HTMLElement): number => {
  const line = Number.parseFloat(getComputedStyle(body).lineHeight);
  const lineStep = Number.isFinite(line) ? line : Number.parseFloat(getComputedStyle(body).fontSize) || LEAST_STEP;
  switch (request.step) {
    case ScrollStep.LineDown:
      return lineStep;
    case ScrollStep.LineUp:
      return -lineStep;
    case ScrollStep.HalfPageDown:
      return body.clientHeight / HALVES;
    case ScrollStep.HalfPageUp:
      return -body.clientHeight / HALVES;
    default:
      return 0;
  }
};

/** Scrolls the pane body around `anchor` by each request made after it mounted. */
const useScrollRequests = (docs: DocsController) => {
  const anchor = useRef<HTMLSpanElement>(null);
  const request = useStore(docs.scroll);
  const seen = useRef(request.at);
  useEffect(() => {
    const body = anchor.current?.closest<HTMLElement>('[data-pane-body]');
    if (request.at === seen.current || body === null || body === undefined) {
      return;
    }
    seen.current = request.at;
    body.scrollBy({ top: distanceOf(request, body) });
  }, [request]);
  return anchor;
};

/** The main view while the Docs panel owns it: the selected document, typeset, or why there is none, with a button that closes it back to the ticket. */
export const DocumentPane = ({ docs, focused, onFocus, onClose }: DocumentProps) => {
  const { t } = useTranslation('docs');
  const anchor = useScrollRequests(docs);
  useStore(docs.store.state);
  const selection = useStore(docs.selection);
  useStore(docs.folded);
  const shown = docs.shown();
  return (
    <Pane
      title={t('document.title')}
      focused={focused}
      onFocus={onFocus}
      actions={[{ kind: PaneActionKind.Close, label: t('document.close'), shortcut: t('document.closeKey'), onClick: onClose }]}
    >
      <span ref={anchor} hidden />
      <Shown shown={shown} docs={docs} target={{ fragment: selection.fragment, opened: selection.opened }} />
    </Pane>
  );
};

const Shown = ({ shown, docs, target }: { shown: ReturnType<DocsController['shown']>; docs: DocsController; target: { fragment: string | undefined; opened: number } }) => {
  const { t } = useTranslation('docs');
  switch (shown.kind) {
    case ShownKind.None:
      return <Text tone={Tone.Dim}>{t('document.noneSelected')}</Text>;
    case ShownKind.Missing:
      return (
        <>
          <Detail facts={[]} title={shown.path} body={undefined} onLink={() => {}} />
          <Text tone={Tone.Dim}>{t('document.missing')}</Text>
        </>
      );
    case ShownKind.Found: {
      const document = shown.document;
      if (document.kind === 'unreadableDocument') {
        return (
          <>
            <Detail facts={[]} title={document.path} body={undefined} onLink={() => {}} />
            <Text tone={Tone.Dim}>{document.reason}</Text>
          </>
        );
      }
      return document.text.kind === 'content' ? (
        <Document source={document.text.value} onLink={(href) => docs.follow(href)} target={target} />
      ) : (
        <Text tone={Tone.Dim}>{t('document.emptyDocument')}</Text>
      );
    }
  }
};
