import type { TFunction } from 'i18next';
import { type ReactNode, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Alert, Annotation, Busy, Button, CommentRow, Dialog, DiffLine, DiffRule, FileRow, Pane, Rows, Text, TextArea } from '../design-system';
import type { DiffFileView, PendingCommentView, PlacedThread, ThreadView } from '../protocol';
import { useStore } from '../shared/useStore';
import { DragRelease } from './dragRelease';
import { type CommentBox, TargetKind } from './commentBox';
import { type DiffRow, RowKind } from './diffRows';
import { type DiffState, DiffStateKind } from './diffState';
import { type CommentEntry, EntryKind, type ReviewSession } from './reviewSession';

interface SessionProps {
  session: ReviewSession;
  focused: boolean;
  onFocus: () => void;
}

interface DiffProps extends SessionProps {
  onRetry: () => void;
}

const RANGE_DASH = '–';

const linesOf = (placed: PlacedThread): string => (placed.first === placed.last ? String(placed.last) : `${placed.first}${RANGE_DASH}${placed.last}`);

const labelOf = (file: DiffFileView, renamed: (from: string, to: string) => string): string =>
  file.status.kind === 'renamedFile' ? renamed(file.status.from, file.path) : file.path;

/** The main view of a review: the diff, unified, with the threads and your pending comments under their lines. */
export const DiffPane = ({ session, focused, onFocus, onRetry }: DiffProps) => {
  const { t } = useTranslation('review');
  useStore(session.changed);
  return (
    <Pane title={t('diff.title', { number: session.number(), title: session.title() })} focused={focused}>
      <Body state={session.diffState()} session={session} focused={focused} onFocus={onFocus} onRetry={onRetry} />
    </Pane>
  );
};

const Body = ({ state, session, focused, onFocus, onRetry }: DiffProps & { state: DiffState }) => {
  const { t } = useTranslation('review');
  switch (state.kind) {
    case DiffStateKind.Loading:
      return <Busy>{t('diff.loading')}</Busy>;
    case DiffStateKind.Unavailable:
      return <Text tone="dim">{t(state.tooLarge ? 'diff.tooLarge' : 'diff.unavailable', { reason: state.reason })}</Text>;
    case DiffStateKind.Empty:
      return <Text tone="dim">{t('diff.empty')}</Text>;
    case DiffStateKind.Fault:
      return (
        <Alert title={t('diff.fault')} action={<Button onClick={onRetry}>{t('diff.retry')}</Button>}>
          {state.message}
        </Alert>
      );
    case DiffStateKind.Populated:
      return <Lines session={session} focused={focused} onFocus={onFocus} />;
  }
};

const Lines = ({ session, focused, onFocus }: SessionProps) => {
  const root = useRef<HTMLDivElement>(null);
  const cursor = session.cursor();
  useEffect(() => {
    const release = new DragRelease(session, () => root.current);
    return () => release.remove();
  }, [session]);
  useEffect(() => {
    root.current?.querySelector('[aria-current="true"]')?.scrollIntoView({ block: 'nearest' });
  }, [cursor]);
  useEffect(() => {
    const line = root.current?.querySelector('[data-line]')?.getBoundingClientRect().height ?? 0;
    const view = root.current?.parentElement?.clientHeight ?? 0;
    session.observeLines(line > 0 ? Math.floor(view / line) : 0);
  });
  return (
    <div ref={root}>
      <Rows>
        {session.rows().all().map((row, index) => (
          <RowView key={index} row={row} session={session} focused={focused} onFocus={onFocus} />
        ))}
      </Rows>
    </div>
  );
};

const RowView = ({ row, session, focused, onFocus }: SessionProps & { row: DiffRow }): ReactNode => {
  const { t } = useTranslation('review');
  switch (row.kind) {
    case RowKind.File:
      return <DiffRule kind="file" text={labelOf(row.file, (from, to) => t('files.renamed', { from, to }))} />;
    case RowKind.Hunk:
      return <DiffRule kind="hunk" text={row.header} />;
    case RowKind.Line: {
      const line = row.line;
      return (
        <DiffLine
          change={line.kind === 'addedLine' ? 'added' : line.kind === 'removedLine' ? 'removed' : 'context'}
          oldNumber={line.kind === 'addedLine' ? undefined : line.oldNumber}
          newNumber={line.kind === 'removedLine' ? undefined : line.newNumber}
          text={line.content}
          index={row.index}
          cursor={session.cursor() === row.index}
          inRange={session.inRange(row.index)}
          onPress={(index) => {
            onFocus();
            session.pressLine(index, focused);
          }}
        />
      );
    }
    case RowKind.Thread:
      return <ThreadAnnotation thread={row.thread} />;
    case RowKind.Pending:
      return <PendingAnnotation comment={row.comment} />;
  }
};

const ThreadAnnotation = ({ thread }: { thread: ThreadView }) => {
  const { t } = useTranslation('review');
  const placed = thread.position.kind === 'placedThread' ? thread.position : undefined;
  return <Annotation kind="thread" heading={placed === undefined ? '' : t('annotation.lines', { lines: linesOf(placed) })} comments={thread.comments} dim={thread.resolved} />;
};

const PendingAnnotation = ({ comment }: { comment: PendingCommentView }) => {
  const { t } = useTranslation('review');
  const heading = comment.anchor.kind === 'onDiffAnchor' ? t('annotation.lines', { lines: linesOf(comment.anchor.position) }) : t('annotation.reply');
  return <Annotation kind="pending" heading={heading} comments={[{ author: t('annotation.you'), body: comment.body }]} dim={false} />;
};

const LAST_SEGMENT = /[^/]*$/;
const COMMIT_LENGTH = 7;

const lastSegment = (path: string): string => LAST_SEGMENT.exec(path)?.[0] ?? path;

const CHANGES = {
  addedFile: 'added',
  modifiedFile: 'modified',
  deletedFile: 'deleted',
  renamedFile: 'renamed',
} as const satisfies Record<DiffFileView['status']['kind'], string>;

/** The panel of the files the pull request changes. */
export const FilesPane = ({ session, focused, onFocus }: SessionProps) => {
  const { t } = useTranslation('review');
  useStore(session.changed);
  const files = session.rows().files();
  const selected = session.selectedFile();
  const state = session.diffState();
  return (
    <Pane title={t('files.title')} focused={focused}>
      {state.kind === DiffStateKind.Loading ? (
        <Busy>{t('diff.loading')}</Busy>
      ) : (
        <Rows>
          {files.map((file, index) => (
            <FileRow
              key={file.path}
              letter={t(`files.letter.${file.status.kind}`)}
              change={CHANGES[file.status.kind]}
              name={file.status.kind === 'renamedFile' ? t('files.renamed', { from: lastSegment(file.status.from), to: lastSegment(file.path) }) : lastSegment(file.path)}
              openThreads={session.threadsPlaced().filter((placed) => placed.thread.open && placed.thread.path === file.path).length}
              pending={session.pendingComments().filter((placed) => placed.comment.anchor.path === file.path).length}
              selected={index === selected}
              onSelect={() => {
                onFocus();
                session.pressFile(index, focused);
              }}
            />
          ))}
        </Rows>
      )}
    </Pane>
  );
};

const whereOf = (path: string, position: ThreadView['position'], t: TFunction<'review'>, author?: string): string => {
  const file = lastSegment(path);
  if (position.kind !== 'placedThread') {
    return t('comments.outdated', { file });
  }
  const lines = position.side === 'LEFT' ? `-${linesOf(position)}` : linesOf(position);
  return author === undefined ? t('comments.placedWithoutAuthor', { file, lines }) : t('comments.placed', { file, lines, author });
};

/** The panel of what has been said on the pull request: your pending comments first, then its threads. */
export const CommentsPane = ({ session, focused, onFocus }: SessionProps) => {
  const { t } = useTranslation('review');
  useStore(session.changed);
  const threads = session.threads;
  const selected = session.selectedComment();
  return (
    <Pane title={t('comments.title')} focused={focused}>
      {threads.kind === 'loadingThreads' ? (
        <Busy>{t('comments.loading')}</Busy>
      ) : threads.kind === 'unavailableThreads' ? (
        <Text tone="dim">{t('comments.unknown', { reason: threads.reason })}</Text>
      ) : session.entries().length === 0 ? (
        <Text tone="dim">{t('comments.none')}</Text>
      ) : (
        <Rows>
          {session.entries().map((entry, index) => (
            <EntryRow key={entry.kind === EntryKind.Pending ? entry.placed.comment.id : entry.placed.thread.id} entry={entry} selected={index === selected} onPress={() => {
              onFocus();
              session.pressComment(index, focused);
            }} />
          ))}
        </Rows>
      )}
    </Pane>
  );
};

const firstLine = (summary: string): string => summary.split('\n')[0]?.trim() ?? '';

/** The pending review: its summary's first line, how many comments it holds, and a warning when the branch moved since it was written. */
export const ReviewPanel = ({ session }: { session: ReviewSession }) => {
  const { t } = useTranslation('review');
  useStore(session.changed);
  const pending = session.pending;
  const summary = firstLine(session.summaryToSubmit());
  return (
    <Pane title={t('panel.title')} focused={false}>
      {pending.kind === 'loadingPendingReview' ? (
        <Busy>{t('panel.loading')}</Busy>
      ) : pending.kind === 'unavailablePendingReview' ? (
        <Text tone="dim" tight>
          {t('panel.unknown', { reason: pending.reason })}
        </Text>
      ) : pending.kind === 'noPendingReview' && summary === '' ? (
        <Text tone="dim" tight>
          {t('panel.none')}
        </Text>
      ) : (
        <>
          <Text tight tone={summary === '' ? 'dim' : 'plain'}>
            {summary === '' ? t('panel.noSummary') : summary}
          </Text>
          <Text tight>{t('panel.count', { count: pending.kind === 'openPendingReview' ? pending.comments.length : 0 })}</Text>
          {pending.kind === 'openPendingReview' && pending.headMove.kind === 'movedHead' ? (
            <>
              <Text tight tone="warning">
                {t('panel.writtenOn', { commit: pending.headMove.writtenOn.slice(0, COMMIT_LENGTH) })}
              </Text>
              <Text tight tone="warning">
                {t('panel.headIs', { commit: pending.headMove.head.slice(0, COMMIT_LENGTH) })}
              </Text>
            </>
          ) : null}
        </>
      )}
    </Pane>
  );
};

const boxTitle = (box: CommentBox, t: TFunction<'review'>): string => {
  switch (box.target.kind) {
    case TargetKind.Summary:
      return t('box.summary');
    case TargetKind.Reply:
      return box.target.thread.comments[0]?.body.split('\n')[0] ?? t('box.reply');
    case TargetKind.New:
    case TargetKind.Edit:
      return t('box.comment');
  }
};

const BoxDialog = ({ session, box }: { session: ReviewSession; box: CommentBox }) => {
  const { t } = useTranslation('review');
  return (
    <Dialog title={boxTitle(box, t)} hint={t('box.hint')}>
      {box.target.kind === TargetKind.Reply
        ? box.target.thread.comments.map((comment, index) => (
            <Text key={index} tight mono>
              {comment.author} {comment.body}
            </Text>
          ))
        : null}
      <TextArea
        label={t('box.label')}
        value={box.text}
        readOnly={box.saving}
        onChange={(text) => session.typeInBox(text)}
        onSave={() => session.saveBox()}
        onCancel={() => session.cancelBox()}
      />
      {box.saving ? <Text tone="dim" tight>{t('box.saving')}</Text> : null}
      {box.notSaved === undefined ? null : <Text tone="dim" tight>{t('box.notSaved', { reason: box.notSaved })}</Text>}
    </Dialog>
  );
};

/** What the review asks in a popup: the box a comment or the summary is written in, or the confirm that submits it. */
export const ReviewDialogs = ({ session }: { session: ReviewSession }) => {
  const { t } = useTranslation('review');
  useStore(session.changed);
  const box = session.box();
  if (box !== undefined) {
    return <BoxDialog session={session} box={box} />;
  }
  return session.submitPrompt() ? (
    <Dialog title={t('submit.title')} hint={t('submit.hint')}>
      <Button onClick={() => session.answerSubmit(true)}>{t('submit.yes')}</Button>
      <Button onClick={() => session.answerSubmit(false)}>{t('submit.no')}</Button>
    </Dialog>
  ) : null;
};

const EntryRow = ({ entry, selected, onPress }: { entry: CommentEntry; selected: boolean; onPress: () => void }) => {
  const { t } = useTranslation('review');
  if (entry.kind === EntryKind.Pending) {
    const { anchor } = entry.placed.comment;
    return <CommentRow kind="pending" open={false} where={whereOf(anchor.path, anchor.position, t)} dim={false} selected={selected} onSelect={onPress} />;
  }
  const { thread } = entry.placed;
  return (
    <CommentRow
      kind="thread"
      open={thread.open}
      where={whereOf(thread.path, thread.position, t, thread.comments[0]?.author)}
      dim={thread.resolved}
      selected={selected}
      onSelect={onPress}
    />
  );
};
