import { i18n } from '../i18n/i18n';
import type {
  Client,
  ReviewCommentsInDiffView,
  ReviewPendingCommentView,
  ReviewPendingInDiffView,
  ReviewPendingReviewView,
  ReviewPendingReviewWriteView,
  ReviewPullRequestDiffView,
  ReviewPullRequestView,
  ReviewReviewThreadView,
  ReviewSummaryDraftPayload,
  ReviewThreadInDiffView,
  ReviewThreadsView,
} from '../protocol';
import { Store } from '../shared/store';
import { CommentBox, TargetKind } from './commentBox';
import { DiffRows, type Stop, StopKind, placedAt, samePlace } from './diffRows';
import { type DiffState, EmptyDiff, FaultedDiff, LoadingDiff, PopulatedDiff, UnavailableDiff } from './diffState';

export interface ReviewDeps {
  client: Client;
  root: string;
  /** The pull request as listed when its review was opened. */
  pullRequest: ReviewPullRequestView;
  /** The status line's notice: what a gesture of the review did not do, and why. */
  refuse: (text: string) => void;
  /** GitHub took the submit. */
  onSubmitted: () => void;
}

const NO_COMMENTS: ReviewCommentsInDiffView = { threads: [], pending: [], shown: [], listed: [], lastAtPlaces: [] };

const HALF = 2;

const messageOf = (error: unknown): string => (error instanceof Error ? error.message : String(error));

const clamp = (index: number, size: number): number => Math.min(Math.max(index, 0), Math.max(0, size - 1));

/**
 * What the review screen shows of one pull request: its diff, threads and pending review as last read, the cursor over the diff lines
 * and the file selected in the files rail. A read lands only if no later read of the same kind was begun since, and the review is
 * closed when the review is left, which drops every read still under way.
 */
export class OpenReview {
  readonly changed: Store<number> = new Store(0);
  diff: ReviewPullRequestDiffView = { kind: 'loadingDiff' };
  threads: ReviewThreadsView = { kind: 'loadingThreads' };
  pending: ReviewPendingReviewView = { kind: 'loadingPendingReview' };
  private diffFault: string | undefined;
  private comments: ReviewCommentsInDiffView = NO_COMMENTS;
  private summary = '';
  private diffRows: DiffRows = DiffRows.of({ kind: 'loadingDiff' }, [], []);
  private showResolved = false;
  private stop = 0;
  private fileSelected = 0;
  private viewLines = 0;
  private diffReads = 0;
  private threadsReads = 0;
  private pendingReads = 0;
  private placements = 0;
  private anchors = 0;
  private closed = false;
  private rangeStart: number | undefined;
  private rangeEnd: number | undefined;
  private dragFrom: number | undefined;
  private pressedAgain = false;
  private open: CommentBox | undefined;
  private typedSummary: string | undefined;
  private confirming = false;
  private writes: Promise<void> = Promise.resolve();
  private waiting = 0;

  constructor(private readonly deps: ReviewDeps) {}

  pullRequest(): ReviewPullRequestView {
    return this.deps.pullRequest;
  }

  number(): number {
    return this.deps.pullRequest.number;
  }

  title(): string {
    return this.deps.pullRequest.title;
  }

  /** Throws away what reads are still under way. */
  close(): void {
    this.closed = true;
  }

  diffState(): DiffState {
    if (this.diffFault !== undefined) {
      return new FaultedDiff(this.diffFault);
    }
    switch (this.diff.kind) {
      case 'loadingDiff':
        return new LoadingDiff();
      case 'unavailableDiff':
        return new UnavailableDiff(this.diff.reason, this.diff.tooLarge);
      case 'readDiff':
        return this.diff.files.length === 0 ? new EmptyDiff() : new PopulatedDiff(this.diff.files);
    }
  }

  rows(): DiffRows {
    return this.diffRows;
  }

  /** The diff line the cursor is on; none while it stands on a file's rule or after the diff. */
  cursor(): number | undefined {
    const at = this.at();
    return at?.kind === StopKind.Line ? at.line : undefined;
  }

  /** Where the cursor stands among the stops of the main view. */
  cursorStop(): Stop | undefined {
    return this.at();
  }

  private at(): Stop | undefined {
    return this.diffRows.stops()[this.stop];
  }

  selectedFile(): number {
    return this.fileSelected;
  }

  resolvedShown(): boolean {
    return this.showResolved;
  }

  /** The pending comments in diff order, the ones on no line last; none until the pending review is read. */
  pendingComments(): ReviewPendingInDiffView[] {
    return this.comments.pending;
  }

  /** The threads in diff order, the ones on no line last; none until they are read. */
  threadsPlaced(): ReviewThreadInDiffView[] {
    return this.comments.threads;
  }

  /** Reads the pending review, the diff and the threads again; each lands unless a later read of its kind was begun. */
  read(): void {
    this.diffFault = undefined;
    if (this.diff.kind !== 'readDiff') {
      this.diff = { kind: 'loadingDiff' };
    }
    const params = { root: this.deps.root, number: this.deps.pullRequest.number };
    void this.readPending(params);
    void this.readDiff(params);
    void this.readThreads(params);
    this.touch();
  }

  private async readPending(params: { root: string; number: number }): Promise<void> {
    this.pendingReads += 1;
    const read = this.pendingReads;
    const view = await this.client('review/pendingOf', params).catch((error: unknown): ReviewPendingReviewView => ({ kind: 'unavailablePendingReview', reason: messageOf(error) }));
    if (read === this.pendingReads && !this.closed) {
      this.pending = view;
      this.touch();
      await this.place();
    }
  }

  private async readDiff(params: { root: string; number: number }): Promise<void> {
    this.diffReads += 1;
    const read = this.diffReads;
    try {
      const view = await this.deps.client.call('review/diffOf', params);
      if (read === this.diffReads && !this.closed) {
        this.diff = view;
        this.touch();
        await this.place();
      }
    } catch (error) {
      if (read === this.diffReads && !this.closed) {
        this.diffFault = messageOf(error);
        this.touch();
      }
    }
  }

  private async readThreads(params: { root: string; number: number }): Promise<void> {
    this.threadsReads += 1;
    const read = this.threadsReads;
    const view = await this.client('review/threadsOf', params).catch((error: unknown): ReviewThreadsView => ({ kind: 'unavailableThreads', reason: messageOf(error) }));
    if (read === this.threadsReads && !this.closed) {
      this.threads = view;
      this.touch();
      await this.place();
    }
  }

  private client<M extends 'review/pendingOf' | 'review/threadsOf'>(method: M, params: { root: string; number: number }) {
    return this.deps.client.call(method, params);
  }

  /**
   * Asks `review` for the comments as the screen shows them and for the summary to submit, and builds the rows from the answer; only the
   * latest ask lands. A failed ask faults the diff, offering its retry, and leaves rows of the diff now held, so no line answers for a
   * diff since replaced.
   */
  private async place(): Promise<void> {
    this.placements += 1;
    const placement = this.placements;
    const { diff, threads, pending, showResolved } = this;
    try {
      const [comments, summary] = await Promise.all([
        this.deps.client.call('review/commentsInDiff', { diff, threads, pending, resolvedShown: showResolved }),
        this.askSummary(),
      ]);
      if (placement === this.placements && !this.closed) {
        this.comments = comments;
        this.summary = summary;
        this.rebuild();
      }
    } catch (error) {
      if (placement === this.placements && !this.closed) {
        this.comments = NO_COMMENTS;
        this.diffFault = messageOf(error);
        this.rebuild();
      }
    }
  }

  /** What `review` says a submit sends now, from the summary typed here and the pending review as last read. */
  private askSummary(): Promise<string> {
    const draft: ReviewSummaryDraftPayload = this.typedSummary === undefined ? { kind: 'untypedSummaryPayload' } : { kind: 'typedSummaryPayload', summary: this.typedSummary };
    return this.deps.client.call('review/summaryToSubmit', { pending: this.pending, draft });
  }

  /** Builds the rows again; the cursor keeps its stop while there still is one, else goes to the first stop of its file. */
  private rebuild(): void {
    const was = this.at();
    this.diffRows = DiffRows.of(this.diff, this.comments.shown, this.comments.pending);
    const kept = was === undefined ? undefined : (this.diffRows.indexOf(was) ?? this.diffRows.firstStopOf(was.file));
    this.goTo(kept ?? this.stop);
  }

  /** The cursor on diff line `line`, held within the lines there are; the Files selection goes to the cursor's file. */
  moveTo(line: number): void {
    this.goTo(this.diffRows.stopOfLine(clamp(line, this.diffRows.lineCount())) ?? 0);
  }

  /** The cursor on the stop at `stop`, held within the stops there are; the Files selection goes to its file, the last one for after the diff. */
  private goTo(stop: number): void {
    this.stop = clamp(stop, this.diffRows.stops().length);
    const at = this.at();
    this.fileSelected = clamp(at === undefined ? this.fileSelected : at.file, this.diffRows.files().length);
    this.touch();
  }

  /** `j` and `k`: the cursor moves by `delta` stops, a line or the rule of a file with comments under it. */
  moveCursor(delta: number): void {
    this.goTo(this.stop + delta);
  }

  /** A press on the rule of the file at `file`, or after the diff for one past the last: the cursor goes there when it is a stop. */
  pressRule(file: number): void {
    const stop = this.diffRows.stops().findIndex((candidate) => candidate.kind !== StopKind.Line && candidate.file === file);
    if (stop !== -1) {
      this.goTo(stop);
    }
  }

  /** Takes how many diff lines the main view shows, which is what half a page is of. */
  observeLines(lines: number): void {
    this.viewLines = lines;
  }

  halfPage(down: boolean): void {
    const half = Math.max(1, Math.floor(this.viewLines / HALF));
    this.moveCursor(down ? half : -half);
  }

  /** To the first stop of the next file that has one, its rule when comments hang there; nowhere from the last. */
  nextFile(): void {
    const target = this.diffRows.firstStopOfNextFile(this.stop);
    if (target !== undefined) {
      this.goTo(target);
    }
  }

  previousFile(): void {
    const target = this.diffRows.firstStopOfPreviousFile(this.stop);
    if (target !== undefined) {
      this.goTo(target);
    }
  }

  /** Selects the file at `index`, held within the files there are. */
  selectFile(index: number): void {
    this.fileSelected = clamp(index, this.diffRows.files().length);
    this.touch();
  }

  goToSelectedFile(): void {
    const target = this.diffRows.firstStopOf(this.fileSelected);
    if (target !== undefined) {
      this.goTo(target);
    }
  }

  /** `r`: resolved threads show under their lines or stop showing, once `review` has said which threads show. */
  toggleResolved(): void {
    this.showResolved = !this.showResolved;
    this.touch();
    void this.place();
  }

  /** Whether the diff line `line` is inside the range being marked. */
  inRange(line: number): boolean {
    return this.rangeStart !== undefined && line >= this.rangeLow() && line <= this.rangeHigh();
  }

  /** Drops the range marked; answers whether there was one. */
  clearRange(): boolean {
    const marked = this.rangeStart !== undefined;
    this.rangeStart = undefined;
    this.rangeEnd = undefined;
    this.touch();
    return marked;
  }

  private rangeLow(): number {
    const here = this.cursor() ?? this.rangeStart ?? 0;
    return Math.min(this.rangeStart ?? here, this.rangeEnd ?? here);
  }

  private rangeHigh(): number {
    const here = this.cursor() ?? this.rangeStart ?? 0;
    return Math.max(this.rangeStart ?? here, this.rangeEnd ?? here);
  }

  /** A press on a line's gutter: the cursor goes to the line, and the comment box opens on it as `c` opens it. */
  commentOn(line: number): Promise<void> {
    this.clearRange();
    this.moveTo(line);
    return this.comment();
  }

  /** `v`: starts a range at the cursor, or ends the one started at it when its lines can take a comment; one that cannot is refused and left as it was. */
  async markRangeKey(): Promise<void> {
    const here = this.cursor();
    if (here === undefined) {
      return;
    }
    if (this.rangeStart === undefined || this.rangeEnd !== undefined) {
      this.rangeStart = here;
      this.rangeEnd = undefined;
      this.touch();
      return;
    }
    await this.markRange(this.rangeStart, here);
  }

  /**
   * A drag: the cursor goes to diff line `to`, and the range from `from` to it is marked when its lines can take a comment. One that cannot
   * is refused with the reason, and the range marked before is left as it was.
   */
  async markRange(from: number, to: number): Promise<void> {
    this.moveTo(to);
    const start = clamp(from, this.diffRows.lineCount());
    const end = this.cursor();
    if (end === undefined) {
      return;
    }
    const answer = await this.anchorOf(Math.min(start, end), Math.max(start, end));
    if (answer === undefined) {
      return;
    }
    if (answer.kind === 'onDiffAnchor') {
      this.rangeStart = start;
      this.rangeEnd = end;
      this.touch();
    } else {
      this.deps.refuse(answer.reason);
    }
  }

  /** Asks where a comment on diff lines `first` to `last` would hang; an answer that is not the latest ask's, or comes after the review was left, is dropped. */
  private async anchorOf(first: number, last: number) {
    this.anchors += 1;
    const ask = this.anchors;
    try {
      const answer = await this.deps.client.call('review/anchorOf', { diff: this.diff, first, last });
      return ask === this.anchors && !this.closed ? answer : undefined;
    } catch (error) {
      if (ask === this.anchors && !this.closed) {
        this.deps.refuse(messageOf(error));
      }
      return undefined;
    }
  }

  box(): CommentBox | undefined {
    return this.open;
  }

  submitPrompt(): boolean {
    return this.confirming;
  }

  /** What the summary box starts from and a submit sends, as `review` last said it. */
  summaryToSubmit(): string {
    return this.summary;
  }

  /** `c` on the cursor's line, or on the range marked: the box opens once the sidecar says a comment can hang there. */
  async comment(): Promise<void> {
    const here = this.cursor();
    if (here === undefined) {
      return;
    }
    const marked = this.rangeStart !== undefined;
    const answer = await this.anchorOf(marked ? this.rangeLow() : here, marked ? this.rangeHigh() : here);
    if (this.open !== undefined) {
      return;
    }
    if (answer?.kind === 'onDiffAnchor') {
      this.openBox(new CommentBox({ kind: TargetKind.New, path: answer.path, position: answer.position }));
    } else if (answer !== undefined) {
      this.deps.refuse(answer.reason);
    }
  }

  /** `S`: the summary box opens on what `review` says a submit would send now, unless another box opened meanwhile. */
  async openSummary(): Promise<void> {
    try {
      const summary = await this.askSummary();
      if (this.open === undefined && !this.closed) {
        this.openBox(new CommentBox({ kind: TargetKind.Summary }, summary));
      }
    } catch (error) {
      if (!this.closed) {
        this.deps.refuse(messageOf(error));
      }
    }
  }

  typeInBox(text: string): void {
    this.open?.typed(text);
    this.touch();
  }

  /** Throws the box and what was typed in it away, and the range it was written on; a box whose save is in flight stays until GitHub answers. */
  cancelBox(): void {
    if (this.open?.saving === true) {
      return;
    }
    this.open = undefined;
    this.rangeStart = undefined;
    this.rangeEnd = undefined;
    this.touch();
  }

  /** Saves what the box holds: the summary is kept until the submit, anything else is written off the render thread. */
  saveBox(): void {
    const box = this.open;
    if (box === undefined || box.saving) {
      return;
    }
    const { root } = this.deps;
    const { number } = this.deps.pullRequest;
    const body = box.text;
    switch (box.target.kind) {
      case TargetKind.Summary:
        this.typedSummary = body;
        this.cancelBox();
        this.confirming = true;
        this.touch();
        void this.place();
        return;
      case TargetKind.New: {
        const { path, position } = box.target;
        return this.write(box, () => this.deps.client.call('review/comment', { root, number, path, position, body }));
      }
      case TargetKind.Edit: {
        const comment = box.target.comment.id;
        return this.write(box, () => this.deps.client.call('review/edit', { root, number, comment, body }));
      }
      case TargetKind.Reply: {
        const thread = box.target.thread.id;
        return this.write(box, () => this.deps.client.call('review/reply', { root, number, thread, body }));
      }
    }
  }

  /** The confirm after the summary is saved: yes submits the pending review as a Comment, anything else leaves it pending. */
  answerSubmit(yes: boolean): void {
    this.confirming = false;
    this.touch();
    if (yes) {
      const { root } = this.deps;
      const { number } = this.deps.pullRequest;
      this.inOrder(
        async () => this.deps.client.call('review/submit', { root, number, summary: await this.askSummary() }),
        (answer) => {
          if (answer.kind === 'writtenReview') {
            this.deps.onSubmitted();
          } else {
            this.deps.refuse(i18n.t('review:refused.notSubmitted', { reason: answer.reason }));
          }
        },
      );
    }
  }

  /** `enter` where the cursor stands, a line or a file's rule: its pending comment opens in the box, or else the last thread there to be replied to. */
  enterInDiff(): void {
    const comment = this.pendingHere();
    const thread = this.threadHere();
    if (comment !== undefined) {
      this.openBox(new CommentBox({ kind: TargetKind.Edit, comment }, comment.body));
    } else if (thread !== undefined) {
      this.replyTo(thread);
    }
  }

  /** The Edit on a pending comment's card: the box opens with its text, and the cursor stays where it is. */
  editPending(comment: ReviewPendingCommentView): void {
    this.openBox(new CommentBox({ kind: TargetKind.Edit, comment }, comment.body));
  }

  /** The Delete on a pending comment's card: it is deleted, and the cursor stays where it is. */
  deletePending(comment: ReviewPendingCommentView): void {
    this.delete(comment);
  }

  /** Whether `enter` where it shows replies to `thread`: it is the last thread there, and no pending comment there takes the key first. */
  enterRepliesTo(thread: ReviewReviewThreadView): boolean {
    const placed = this.comments.shown.find((entry) => entry.thread.id === thread.id);
    if (placed === undefined) {
      return false;
    }
    const last = this.comments.shown.findLast((entry) => samePlace(entry.line, placed.line));
    const pendingFirst = this.comments.lastAtPlaces.some((entry) => samePlace(entry.line, placed.line));
    return last?.thread.id === thread.id && !pendingFirst;
  }

  /** Whether `enter` and `x` where it shows act on `comment`: it is the pending comment `review` picks for its place. */
  keysReach(comment: ReviewPendingCommentView): boolean {
    return this.comments.lastAtPlaces.some((entry) => entry.comment.id === comment.id);
  }

  /** The thread where the cursor stands, the last one when there are several. */
  private threadHere(): ReviewReviewThreadView | undefined {
    const at = this.at();
    return at === undefined ? undefined : this.comments.shown.findLast((placed) => placedAt(placed.line, at))?.thread;
  }

  /** `x` where the cursor stands deletes its pending comment. */
  deleteAtCursor(): void {
    const comment = this.pendingHere();
    if (comment !== undefined) {
      this.delete(comment);
    }
  }

  /** The pending comment `enter` and `x` act on where the cursor stands, as `review` picks it, if there is one there. */
  private pendingHere(): ReviewPendingCommentView | undefined {
    const at = this.at();
    return at === undefined ? undefined : this.comments.lastAtPlaces.find((placed) => placedAt(placed.line, at))?.comment;
  }

  /** The Reply under a thread's line: the box opens to reply to it, resolved or not, and neither the cursor nor the selection moves. */
  replyTo(thread: ReviewReviewThreadView): void {
    this.openBox(new CommentBox({ kind: TargetKind.Reply, thread }));
  }

  /** A press on a diff line: the cursor goes to it and a drag starts from it, remembering whether it was a second press, on the cursor's line in the focused diff. */
  pressLine(line: number, focused: boolean): void {
    this.pressedAgain = focused && this.cursor() === line;
    this.moveTo(line);
    this.dragFrom = line;
  }

  /** A press that landed on no diff line, or the window losing the pointer: a drag under way ends without marking anything. */
  dropDrag(): void {
    this.dragFrom = undefined;
    this.pressedAgain = false;
  }

  /**
   * The release of a drag, on the line nearest to where the pointer let go: the range between the two lines is marked unless they are one.
   * Released where a second press began, it edits the line's pending comment, as `enter` does, or writes one there, as `c` does.
   */
  async releaseLine(line: number): Promise<void> {
    const from = this.dragFrom;
    const again = this.pressedAgain;
    this.dragFrom = undefined;
    this.pressedAgain = false;
    if (from !== undefined && from !== line) {
      await this.markRange(from, line);
    } else if (from !== undefined && again && this.pendingHere() !== undefined) {
      this.enterInDiff();
    } else if (from !== undefined && again) {
      await this.comment();
    }
  }

  /** A press on a file: it is selected and the cursor goes to its first stop. */
  pressFile(index: number): void {
    this.selectFile(index);
    this.goToSelectedFile();
  }

  private openBox(box: CommentBox): void {
    this.open = box;
    this.touch();
  }

  private delete(comment: ReviewPendingCommentView): void {
    const { root } = this.deps;
    const { number } = this.deps.pullRequest;
    this.inOrder(
      () => this.deps.client.call('review/delete', { root, number, comment: comment.id }),
      (answer) => {
        if (answer.kind === 'writtenReview') {
          this.applyWritten(answer.pending);
        } else {
          this.deps.refuse(i18n.t('review:refused.notDeleted', { reason: answer.reason }));
        }
      },
    );
  }

  /**
   * Writes off the render thread; GitHub's answer closes the box, or keeps it with the reason it was refused. A call that failed may
   * still have written, so it closes the box rather than offer a save that could write the comment twice.
   */
  private write(box: CommentBox, write: () => Promise<ReviewPendingReviewWriteView>): void {
    box.saveBegun();
    this.touch();
    this.inOrder(
      write,
      (answer) => {
        if (answer.kind === 'writtenReview') {
          this.applyWritten(answer.pending);
          this.closeBox(box);
        } else {
          box.saveRefused(answer.reason);
          this.touch();
        }
      },
      () => this.closeBox(box),
    );
  }

  private closeBox(box: CommentBox): void {
    if (this.open === box) {
      this.open = undefined;
      this.rangeStart = undefined;
      this.rangeEnd = undefined;
      this.touch();
    }
  }

  /** What a write answered with replaces what was read, and any read begun before it. */
  private applyWritten(pending: ReviewPendingReviewView): void {
    this.pending = pending;
    this.pendingReads += 1;
    void this.place();
  }

  /** Writes to the pending review one after the other; an answer for a review since left is dropped. */
  private inOrder(write: () => Promise<ReviewPendingReviewWriteView>, answered: (answer: ReviewPendingReviewWriteView) => void, failed: () => void = () => undefined): void {
    this.waiting += 1;
    this.writes = this.waiting === 1 ? this.perform(write, answered, failed) : this.writes.then(() => this.perform(write, answered, failed));
  }

  /** A write GitHub answered is handed on; one whose call failed may or may not have landed, so the pending review is read again to show which. */
  private async perform(write: () => Promise<ReviewPendingReviewWriteView>, answered: (answer: ReviewPendingReviewWriteView) => void, failed: () => void): Promise<void> {
    try {
      const answer = await write();
      this.waiting -= 1;
      if (!this.closed) {
        answered(answer);
      }
    } catch (error) {
      this.waiting -= 1;
      if (!this.closed) {
        failed();
        this.deps.refuse(i18n.t('review:refused.notWritten', { reason: messageOf(error) }));
        void this.readPending({ root: this.deps.root, number: this.deps.pullRequest.number });
      }
    }
  }

  private touch(): void {
    this.changed.set(this.changed.get() + 1);
  }
}
