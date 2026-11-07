import type { TFunction } from 'i18next';
import { type ReactNode, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Annotation,
  Busy,
  Button,
  CommentKind,
  CommentRow,
  Dialog,
  DiffLine,
  DiffRule,
  FileChange,
  FileRow,
  LineChange,
  Pane,
  Rows,
  RuleKind,
  Text,
  TextArea,
  Tone,
} from '../design-system';
import type { ReviewCommentInDiffView, ReviewDiffFileView, ReviewDiffLine, ReviewPendingCommentView, ReviewPlacedThread, ReviewReviewThreadView } from '../protocol';
import { useStore } from '../shared/useStore';
import { DragRelease } from './dragRelease';
import { type CommentBox, TargetKind } from './commentBox';
import { type DiffRow, RowKind } from './diffRows';
import { type DiffState, DiffStateKind } from './diffState';
import type { OpenReview } from './openReview';

interface ReviewProps {
  review: OpenReview;
  focused: boolean;
  onFocus: () => void;
}

interface DiffProps extends ReviewProps {
  onRetry: () => void;
}

const RANGE_DASH = '–';

const linesOf = (placed: ReviewPlacedThread): string => (placed.first === placed.last ? String(placed.last) : `${placed.first}${RANGE_DASH}${placed.last}`);

const labelOf = (file: ReviewDiffFileView, renamed: (from: string, to: string) => string): string =>
  file.status.kind === 'renamedFile' ? renamed(file.status.from, file.path) : file.path;

/** The main view of a review: the diff, unified, with the threads and your pending comments under their lines. */
export const DiffPane = ({ review, focused, onFocus, onRetry }: DiffProps) => {
  const { t } = useTranslation('review');
  useStore(review.changed);
  return (
    <Pane title={t('diff.title', { number: review.number(), title: review.title() })} focused={focused}>
      <Body state={review.diffState()} review={review} focused={focused} onFocus={onFocus} onRetry={onRetry} />
    </Pane>
  );
};

const Body = ({ state, review, focused, onFocus, onRetry }: DiffProps & { state: DiffState }) => {
  const { t } = useTranslation('review');
  switch (state.kind) {
    case DiffStateKind.Loading:
      return <Busy>{t('diff.loading')}</Busy>;
    case DiffStateKind.Unavailable:
      return <Text tone={Tone.Dim}>{t(state.tooLarge ? 'diff.tooLarge' : 'diff.unavailable', { reason: state.reason })}</Text>;
    case DiffStateKind.Empty:
      return <Text tone={Tone.Dim}>{t('diff.empty')}</Text>;
    case DiffStateKind.Fault:
      return (
        <Alert title={t('diff.fault')} action={<Button onClick={onRetry}>{t('diff.retry')}</Button>}>
          {state.message}
        </Alert>
      );
    case DiffStateKind.Populated:
      return <Lines review={review} focused={focused} onFocus={onFocus} />;
  }
};

/** The last comment set under a diff line's row, if it has any: the rows after it that hold an annotation. */
const lastAnnotationUnder = (row: Element | null | undefined): Element | undefined => {
  const next = row?.nextElementSibling;
  if (next === null || next === undefined || next.querySelector('article') === null) {
    return undefined;
  }
  return lastAnnotationUnder(next) ?? next;
};

const Lines = ({ review, focused, onFocus }: ReviewProps) => {
  const root = useRef<HTMLDivElement>(null);
  const cursor = review.cursor();
  const goneToComment = review.goneToComment();
  useEffect(() => {
    const release = new DragRelease(review, () => root.current);
    return () => release.remove();
  }, [review]);
  useEffect(() => {
    root.current?.querySelector('[aria-current="true"]')?.scrollIntoView({ block: 'nearest' });
  }, [cursor]);
  useEffect(() => {
    if (goneToComment > 0) {
      lastAnnotationUnder(root.current?.querySelector('[aria-current="true"]')?.closest('li'))?.scrollIntoView({ block: 'nearest' });
    }
  }, [goneToComment]);
  useEffect(() => {
    const line = root.current?.querySelector('[data-line]')?.getBoundingClientRect().height ?? 0;
    const view = root.current?.parentElement?.clientHeight ?? 0;
    review.observeLines(line > 0 ? Math.floor(view / line) : 0);
  });
  return (
    <div ref={root}>
      <Rows>
        {review.rows().all().map((row, index) => (
          <RowView key={index} row={row} review={review} focused={focused} onFocus={onFocus} />
        ))}
      </Rows>
    </div>
  );
};

const RowView = ({ row, review, focused, onFocus }: ReviewProps & { row: DiffRow }): ReactNode => {
  const { t } = useTranslation('review');
  switch (row.kind) {
    case RowKind.File:
      return <DiffRule kind={RuleKind.File} text={labelOf(row.file, (from, to) => t('files.renamed', { from, to }))} />;
    case RowKind.Hunk:
      return <DiffRule kind={RuleKind.Hunk} text={row.header} />;
    case RowKind.Line: {
      const line = row.line;
      return (
        <DiffLine
          change={LINE_CHANGES[line.kind]}
          oldNumber={line.kind === 'addedLine' ? undefined : line.oldNumber}
          newNumber={line.kind === 'removedLine' ? undefined : line.newNumber}
          text={line.content}
          index={row.index}
          cursor={review.cursor() === row.index}
          inRange={review.inRange(row.index)}
          onPress={(index) => {
            onFocus();
            review.pressLine(index, focused);
          }}
        />
      );
    }
    case RowKind.Thread:
      return <ThreadAnnotation thread={row.thread} review={review} />;
    case RowKind.Pending:
      return <PendingAnnotation comment={row.comment} />;
  }
};

const ThreadAnnotation = ({ thread, review }: { thread: ReviewReviewThreadView; review: OpenReview }) => {
  const { t } = useTranslation('review');
  const placed = thread.position.kind === 'placedThread' ? thread.position : undefined;
  const lines = placed === undefined ? '' : t('annotation.lines', { lines: linesOf(placed) });
  return (
    <Annotation
      kind={CommentKind.Thread}
      heading={lines}
      comments={thread.comments}
      dim={thread.resolved}
      reply={{ text: t('annotation.replyAction'), label: t('annotation.replyLabel', { lines }), onReply: () => review.replyTo(thread) }}
    />
  );
};

const PendingAnnotation = ({ comment }: { comment: ReviewPendingCommentView }) => {
  const { t } = useTranslation('review');
  const heading = comment.anchor.kind === 'onDiffAnchor' ? t('annotation.lines', { lines: linesOf(comment.anchor.position) }) : t('annotation.reply');
  return <Annotation kind={CommentKind.Pending} heading={heading} comments={[{ author: t('annotation.you'), body: comment.body }]} dim={false} />;
};

const LAST_SEGMENT = /[^/]*$/;
const COMMIT_LENGTH = 7;

const lastSegment = (path: string): string => LAST_SEGMENT.exec(path)?.[0] ?? path;

const CHANGES: Readonly<Record<ReviewDiffFileView['status']['kind'], FileChange>> = {
  addedFile: FileChange.Added,
  modifiedFile: FileChange.Modified,
  deletedFile: FileChange.Deleted,
  renamedFile: FileChange.Renamed,
};

const LINE_CHANGES: Readonly<Record<ReviewDiffLine['kind'], LineChange>> = {
  addedLine: LineChange.Added,
  removedLine: LineChange.Removed,
  contextLine: LineChange.Context,
};

/** The panel of the files the pull request changes. */
export const FilesPane = ({ review, focused, onFocus }: ReviewProps) => {
  const { t } = useTranslation('review');
  useStore(review.changed);
  const files = review.rows().files();
  const selected = review.selectedFile();
  const state = review.diffState();
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
              openThreads={review.threadsPlaced().filter((placed) => placed.thread.open && placed.thread.path === file.path).length}
              pending={review.pendingComments().filter((placed) => placed.comment.anchor.path === file.path).length}
              selected={index === selected}
              onSelect={() => {
                onFocus();
                review.pressFile(index);
              }}
            />
          ))}
        </Rows>
      )}
    </Pane>
  );
};

const whereOf = (path: string, position: ReviewReviewThreadView['position'], t: TFunction<'review'>): string => {
  const file = lastSegment(path);
  if (position.kind !== 'placedThread') {
    return t('comments.outdated', { file });
  }
  const lines = position.side === 'LEFT' ? `-${linesOf(position)}` : linesOf(position);
  return t('comments.placed', { file, lines });
};

const trailingOf = (thread: ReviewReviewThreadView, t: TFunction<'review'>): string => {
  const author = thread.comments.at(-1)?.author ?? '';
  const replies = thread.comments.length - 1;
  return replies > 0 ? t('comments.replies', { count: replies, author }) : author;
};

/** The panel of what has been said on the pull request: your pending comments first, then its threads. */
export const CommentsPane = ({ review, focused, onFocus }: ReviewProps) => {
  const { t } = useTranslation('review');
  useStore(review.changed);
  const threads = review.threads;
  const selected = review.selectedComment();
  return (
    <Pane title={t('comments.title')} focused={focused}>
      {threads.kind === 'loadingThreads' ? (
        <Busy>{t('comments.loading')}</Busy>
      ) : threads.kind === 'unavailableThreads' ? (
        <Text tone={Tone.Dim}>{t('comments.unknown', { reason: threads.reason })}</Text>
      ) : review.entries().length === 0 ? (
        <Text tone={Tone.Dim}>{t('comments.none')}</Text>
      ) : (
        <Rows>
          {review.entries().map((entry, index) => (
            <EntryRow key={entry.kind === 'pendingInDiff' ? entry.comment.id : entry.thread.id} entry={entry} selected={index === selected} onPress={() => {
              onFocus();
              review.pressComment(index, focused);
            }} />
          ))}
        </Rows>
      )}
    </Pane>
  );
};

const firstLine = (summary: string): string => summary.split('\n')[0]?.trim() ?? '';

/** The pending review: its summary's first line, how many comments it holds, and a warning when the branch moved since it was written. */
export const ReviewPanel = ({ review }: { review: OpenReview }) => {
  const { t } = useTranslation('review');
  useStore(review.changed);
  const pending = review.pending;
  const summary = firstLine(review.summaryToSubmit());
  return (
    <Pane title={t('panel.title')} focused={false}>
      {pending.kind === 'loadingPendingReview' ? (
        <Busy>{t('panel.loading')}</Busy>
      ) : pending.kind === 'unavailablePendingReview' ? (
        <Text tone={Tone.Dim} tight>
          {t('panel.unknown', { reason: pending.reason })}
        </Text>
      ) : pending.kind === 'noPendingReview' && summary === '' ? (
        <Text tone={Tone.Dim} tight>
          {t('panel.none')}
        </Text>
      ) : (
        <>
          <Text tight tone={summary === '' ? Tone.Dim : Tone.Plain}>
            {summary === '' ? t('panel.noSummary') : summary}
          </Text>
          <Text tight>{t('panel.count', { count: pending.kind === 'openPendingReview' ? pending.comments.length : 0 })}</Text>
          {pending.kind === 'openPendingReview' && pending.headMove.kind === 'movedHead' ? (
            <>
              <Text tight tone={Tone.Warning}>
                {t('panel.writtenOn', { commit: pending.headMove.writtenOn.slice(0, COMMIT_LENGTH) })}
              </Text>
              <Text tight tone={Tone.Warning}>
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

const BoxDialog = ({ review, box }: { review: OpenReview; box: CommentBox }) => {
  const { t } = useTranslation('review');
  return (
    <Dialog title={boxTitle(box, t)} hint={t('box.hint')} onDismiss={() => review.cancelBox()}>
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
        onChange={(text) => review.typeInBox(text)}
        onSave={() => review.saveBox()}
        onCancel={() => review.cancelBox()}
      />
      {box.saving ? <Text tone={Tone.Dim} tight>{t('box.saving')}</Text> : null}
      {box.notSaved === undefined ? null : <Text tone={Tone.Warning} tight>{t('box.notSaved', { reason: box.notSaved })}</Text>}
    </Dialog>
  );
};

/** What the review asks in a popup: the box a comment or the summary is written in, or the confirm that submits it. */
export const ReviewDialogs = ({ review }: { review: OpenReview }) => {
  const { t } = useTranslation('review');
  useStore(review.changed);
  const box = review.box();
  if (box !== undefined) {
    return <BoxDialog review={review} box={box} />;
  }
  return review.submitPrompt() ? (
    <Dialog title={t('submit.title')} hint={t('submit.hint')}>
      <Button onClick={() => review.answerSubmit(true)}>{t('submit.yes')}</Button>
      <Button onClick={() => review.answerSubmit(false)}>{t('submit.no')}</Button>
    </Dialog>
  ) : null;
};

const EntryRow = ({ entry, selected, onPress }: { entry: ReviewCommentInDiffView; selected: boolean; onPress: () => void }) => {
  const { t } = useTranslation('review');
  if (entry.kind === 'pendingInDiff') {
    const { anchor, body } = entry.comment;
    return <CommentRow kind={CommentKind.Pending} open={false} where={whereOf(anchor.path, anchor.position, t)} trailing="" preview={firstLine(body)} dim={false} selected={selected} onSelect={onPress} />;
  }
  const { thread } = entry;
  return (
    <CommentRow
      kind={CommentKind.Thread}
      open={thread.open}
      where={whereOf(thread.path, thread.position, t)}
      trailing={trailingOf(thread, t)}
      preview={firstLine(thread.comments[0]?.body ?? '')}
      dim={thread.resolved}
      selected={selected}
      onSelect={onPress}
    />
  );
};
