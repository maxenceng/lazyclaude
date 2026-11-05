import { useTranslation } from 'react-i18next';
import { Alert, Busy, Button, Detail, DocsTreeRow, Document, Pane, Text, Rows } from '../design-system';
import { useStore } from '../shared/useStore';
import type { DocsController } from './docsController';
import { type DocsState, DocsStateKind } from './docsState';
import { RowKind } from './rows';
import { ShownKind } from './shownDocument';

interface PaneProps {
  docs: DocsController;
  focused: boolean;
}

interface TreeProps extends PaneProps {
  onFocus: () => void;
  onRetry: () => void;
}

/** The Docs panel: the tree of `docs/`. A press on the selected row opens it, or folds it if it is a folder. */
export const DocsPane = ({ docs, focused, onFocus, onRetry }: TreeProps) => {
  const { t } = useTranslation('docs');
  const state = useStore(docs.store.state);
  useStore(docs.selection);
  useStore(docs.folded);
  const selected = docs.selectedKey();
  return (
    <Pane title={t('docs.title')} focused={focused}>
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
      return <Text tone="dim">{t('docs.empty')}</Text>;
    case DocsStateKind.Unavailable:
      return (
        <>
          <Text>{t('docs.unavailable')}</Text>
          <Text tone="dim">{state.reason}</Text>
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

/** The main view while the Docs panel owns it: the selected document, typeset, or why there is none. */
export const DocumentPane = ({ docs, focused }: PaneProps) => {
  const { t } = useTranslation('docs');
  useStore(docs.store.state);
  const selection = useStore(docs.selection);
  useStore(docs.folded);
  const shown = docs.shown();
  return (
    <Pane title={t('document.title')} focused={focused}>
      <Shown shown={shown} docs={docs} target={{ fragment: selection.fragment, opened: selection.opened }} />
    </Pane>
  );
};

const Shown = ({ shown, docs, target }: { shown: ReturnType<DocsController['shown']>; docs: DocsController; target: { fragment: string | undefined; opened: number } }) => {
  const { t } = useTranslation('docs');
  switch (shown.kind) {
    case ShownKind.None:
      return <Text tone="dim">{t('document.noneSelected')}</Text>;
    case ShownKind.Missing:
      return (
        <>
          <Detail facts={[]} title={shown.path} body={undefined} onLink={() => {}} />
          <Text tone="dim">{t('document.missing')}</Text>
        </>
      );
    case ShownKind.Found: {
      const document = shown.document;
      if (document.kind === 'unreadableDocument') {
        return (
          <>
            <Detail facts={[]} title={document.path} body={undefined} onLink={() => {}} />
            <Text tone="dim">{document.reason}</Text>
          </>
        );
      }
      return document.text.kind === 'content' ? (
        <Document source={document.text.value} onLink={(href) => docs.follow(href)} target={target} />
      ) : (
        <Text tone="dim">{t('document.emptyDocument')}</Text>
      );
    }
  }
};
