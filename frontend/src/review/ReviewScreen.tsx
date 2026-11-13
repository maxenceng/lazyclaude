import type { TFunction } from 'i18next';
import { type ReactNode, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Annotation,
  type AnnotationAction,
  Busy,
  Button,
  ButtonTone,
  CommentKind,
  Dialog,
  DiffLine,
  DiffRule,
  FileChange,
  FileRow,
  LineChange,
  Pane,
  PaneActionKind,
  type PaneChip,
  PaneFoot,
  Badge,
  BadgeTone,
  Rows,
  RuleKind,
  Text,
  TextArea,
  Tone,
} from '../design-system';
import type { ReviewDiffFileView, ReviewDiffLine, ReviewPendingCommentView, ReviewPlacedThread, ReviewPullRequestView, ReviewReviewThreadView } from '../protocol';
import { ciWordKey } from './pullRequestList';
import { useStore } from '../shared/useStore';
import { DragRelease } from './dragRelease';
import { type CommentBox, TargetKind } from './commentBox';
import { type DiffRow, RowKind, deltaOf } from './diffRows';
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

interface DiffPaneProps extends DiffProps {
  /** Leaves the review, as `esc` does. */
  onClose: () => void;
}

const RANGE_DASH = '–';

const linesOf = (placed: ReviewPlacedThread): string => (placed.first === placed.last ? String(placed.last) : `${placed.first}${RANGE_DASH}${placed.last}`);

const labelOf = (file: ReviewDiffFileView, renamed: (from: string, to: string) => string): string =>
  file.status.kind === 'renamedFile' ? renamed(file.status.from, file.path) : file.path;

/** What the review's title row names of its pull request: its number, the ticket it closes, its CI in a word, and its branch. */
const chipsOf = (pullRequest: ReviewPullRequestView, t: TFunction<'review'>): PaneChip[] => [
    { label: t('diff.number', { number: pullRequest.number }) },
    ...(pullRequest.closing.kind === 'closesIssue' ? [{ label: String(pullRequest.closing.number) }] : []),
    { label: t(ciWordKey(pullRequest)) },
    { label: pullRequest.head },
];

/** The main view of a review: the diff, unified, with the threads and your pending comments under their lines; its title row names the pull request and closes the review. */
export const DiffPane = ({ review, focused, onFocus, onRetry, onClose }: DiffPaneProps) => {
  const { t } = useTranslation('review');
  useStore(review.changed);
  return (
    <Pane
      title={t('diff.title')}
      focused={focused}
      chips={chipsOf(review.pullRequest(), t)}
      actions={[{ kind: PaneActionKind.Close, label: t('diff.close'), shortcut: t('diff.closeKey'), onClick: onClose }]}
    >
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
  useEffect(() => {
    const release = new DragRelease(review, () => root.current);
    return () => release.remove();
  }, [review]);
  useEffect(() => {
    const line = root.current?.querySelector('[aria-current="true"]');
    line?.scrollIntoView({ block: 'nearest' });
    lastAnnotationUnder(line?.closest('li'))?.scrollIntoView({ block: 'nearest' });
  }, [cursor]);
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
      return (
        <DiffRule
          kind={RuleKind.File}
          text={labelOf(row.file, (from, to) => t('files.renamed', { from, to }))}
          delta={deltaOf(row.file)}
          change={{ letter: t(`files.letter.${row.file.status.kind}`), kind: CHANGES[row.file.status.kind] }}
        />
      );
    case RowKind.Hunk:
      return <DiffRule kind={RuleKind.Hunk} text={row.header} />;
    case RowKind.Line: {
      const line = row.line;
      return (
        <DiffLine
          change={LINE_CHANGES[line.kind]}
          number={line.kind === 'removedLine' ? line.oldNumber : line.newNumber}
          text={line.content}
          index={row.index}
          cursor={review.cursor() === row.index}
          inRange={review.inRange(row.index)}
          hangs={hangsFrom(review, row.index)}
          onPress={(index) => {
            onFocus();
            review.pressLine(index, focused);
          }}
          onComment={(index) => {
            onFocus();
            void review.commentOn(index);
          }}
        />
      );
    }
    case RowKind.Thread:
      return <ThreadCard thread={row.thread} lineless={row.lineless} review={review} />;
    case RowKind.Pending:
      return <PendingCard comment={row.comment} lineless={row.lineless} review={review} />;
  }
};

const hangsFrom = (review: OpenReview, line: number): CommentKind | undefined => {
  if (review.rows().hasThread(line)) {
    return CommentKind.Thread;
  }
  return review.rows().hasPending(line) ? CommentKind.Pending : undefined;
};

const whereOf = (path: string, position: ReviewReviewThreadView['position'], t: TFunction<'review'>): string => {
  const file = lastSegment(path);
  if (position.kind !== 'placedThread') {
    return t('thread.whereOutdated', { file });
  }
  const lines = position.side === 'LEFT' ? `-${linesOf(position)}` : linesOf(position);
  return t('thread.where', { file, lines });
};

/** Where an outdated thread was, as `review` still knows it: the placement it has, or the one it kept. */
const originalPlaceOf = (position: ReviewReviewThreadView['position']): ReviewPlacedThread | undefined => {
  if (position.kind === 'placedThread') {
    return position;
  }
  return position.was.kind === 'originallyPlaced' ? position.was.place : undefined;
};

const outdatedTag = (place: ReviewPlacedThread | undefined, pending: boolean, t: TFunction<'review'>): string => {
  if (place === undefined) {
    return t(pending ? 'pendingComment.outdatedTag' : 'thread.outdated');
  }
  const lines = linesOf(place);
  const single = place.first === place.last;
  if (pending) {
    return t(single ? 'pendingComment.outdatedWasLine' : 'pendingComment.outdatedWasLines', { lines });
  }
  return t(single ? 'thread.outdatedWasLine' : 'thread.outdatedWasLines', { lines });
};

const tagOf = (thread: ReviewReviewThreadView, lineless: boolean, t: TFunction<'review'>): string => {
  if (thread.resolved) {
    return t('thread.resolved');
  }
  if (lineless) {
    return outdatedTag(originalPlaceOf(thread.position), false, t);
  }
  return t('thread.open', { count: thread.comments.length });
};

/** A thread as one card across the diff: under its line, or at the top of its file when `review` places it on none. */
const ThreadCard = ({ thread, lineless, review }: { thread: ReviewReviewThreadView; lineless: boolean; review: OpenReview }) => {
  const { t } = useTranslation('review');
  const where = whereOf(thread.path, thread.position, t);
  const reply: AnnotationAction = { text: t('thread.reply'), label: t('thread.replyLabel', { where }), chip: !lineless && review.enterRepliesTo(thread) ? t('thread.replyKey') : undefined, onPress: () => review.replyTo(thread) };
  return <Annotation kind={CommentKind.Thread} where={where} tag={tagOf(thread, lineless, t)} comments={thread.comments} dim={thread.resolved} actions={[reply]} />;
};

/** Your pending comment as a neutral card edged and badged amber, with the keys that edit and delete it as buttons. */
const PendingCard = ({ comment, lineless, review }: { comment: ReviewPendingCommentView; lineless: boolean; review: OpenReview }) => {
  const { t } = useTranslation('review');
  const { anchor } = comment;
  const where = anchor.kind === 'onDiffAnchor' ? whereOf(anchor.path, anchor.position, t) : t('thread.whereReply', { file: lastSegment(anchor.path) });
  const reached = !lineless && review.keysReach(comment);
  const actions: AnnotationAction[] = [
    { text: t('pendingComment.edit'), label: t('pendingComment.editLabel', { where }), chip: reached ? t('pendingComment.editKey') : undefined, onPress: () => review.editPending(comment) },
    { text: t('pendingComment.delete'), label: t('pendingComment.deleteLabel', { where }), chip: reached ? t('pendingComment.deleteKey') : undefined, onPress: () => review.deletePending(comment) },
  ];
  return (
    <Annotation
      kind={CommentKind.Pending}
      where={where}
      tag={lineless ? outdatedTag(originalPlaceOf(anchor.position), true, t) : t('pendingComment.tag')}
      comments={[{ author: t('pendingComment.you'), body: comment.body }]}
      dim={false}
      actions={actions}
    />
  );
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

const DIRECTORY = /^(.*\/)[^/]*$/;

const directoryOf = (path: string): string => DIRECTORY.exec(path)?.[1] ?? '';

/** The rail of the files the pull request changes: each with its line counts and word badges for its threads and your pending comments, and the pending review at its foot. */
export const FilesPane = ({ review, focused, onFocus }: ReviewProps) => {
  const { t } = useTranslation('review');
  useStore(review.changed);
  const files = review.rows().files();
  const selected = review.selectedFile();
  const state = review.diffState();
  const deltas = files.map(deltaOf);
  const chips: PaneChip[] =
    files.length === 0
      ? []
      : [
          { label: t('files.count', { count: files.length }) },
          { label: t('files.delta', { added: deltas.reduce((sum, delta) => sum + delta.added, 0), removed: deltas.reduce((sum, delta) => sum + delta.removed, 0) }) },
        ];
  return (
    <Pane title={t('files.title')} focused={focused} chips={chips} foot={<ReviewPanel review={review} />}>
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
              directory={directoryOf(file.path)}
              {...deltaOf(file)}
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

const firstLine = (summary: string): string => summary.split('\n')[0]?.trim() ?? '';

/** What the foot says of the pending review: its summary's first line, how many comments it holds, and a warning when the branch moved since it was written. */
const PendingWords = ({ review }: { review: OpenReview }) => {
  const { t } = useTranslation('review');
  const pending = review.pending;
  const summary = firstLine(review.summaryToSubmit());
  if (pending.kind === 'loadingPendingReview') {
    return <Busy>{t('panel.loading')}</Busy>;
  }
  if (pending.kind === 'unavailablePendingReview') {
    return <Text tone={Tone.Dim} tight>{t('panel.unknown', { reason: pending.reason })}</Text>;
  }
  if (pending.kind === 'noPendingReview' && summary === '') {
    return <Text tone={Tone.Dim} tight>{t('panel.none')}</Text>;
  }
  return (
    <>
      <Text tight>
        {t('panel.count', { count: pending.kind === 'openPendingReview' ? pending.comments.length : 0 })}
        {summary === '' ? '' : t('panel.summary', { summary })}
      </Text>
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
  );
};

/** The pending review at the files rail's foot: its state in a badge and words, and a Submit button that does what `S` does. */
export const ReviewPanel = ({ review }: { review: OpenReview }) => {
  const { t } = useTranslation('review');
  useStore(review.changed);
  const pending = review.pending;
  const holds = pending.kind === 'openPendingReview';
  const known = holds || pending.kind === 'noPendingReview';
  return (
    <PaneFoot
      label={t('panel.title')}
      badge={known ? <Badge tone={holds ? BadgeTone.Amber : BadgeTone.Plain}>{t(holds ? 'panel.badge' : 'panel.badgeNone')}</Badge> : null}
      action={
        <Button tone={ButtonTone.Affirm} chip={t('panel.submitKey')} onClick={() => void review.openSummary()}>
          {t('panel.submit')}
        </Button>
      }
    >
      <PendingWords review={review} />
    </PaneFoot>
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
