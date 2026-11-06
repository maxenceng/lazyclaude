import type { ReviewPendingCommentView, ReviewPlacedThread, ReviewReviewThreadView } from '../protocol';

export enum TargetKind {
  New = 'new',
  Edit = 'edit',
  Reply = 'reply',
  Summary = 'summary',
}

/** What saving the box writes: a comment on a line or a range, a pending comment edited, a reply to a thread, or the summary. */
export type BoxTarget =
  | { kind: TargetKind.New; path: string; position: ReviewPlacedThread }
  | { kind: TargetKind.Edit; comment: ReviewPendingCommentView }
  | { kind: TargetKind.Reply; thread: ReviewReviewThreadView }
  | { kind: TargetKind.Summary };

/** The box a comment or the summary is written in: its text, what it writes, and where it stands in saving. A save in flight holds it still; one that failed keeps the text and says why. */
export class CommentBox {
  saving = false;
  notSaved: string | undefined;

  constructor(
    readonly target: BoxTarget,
    public text: string = '',
  ) {}

  typed(text: string): void {
    if (!this.saving) {
      this.text = text;
    }
  }

  saveBegun(): void {
    this.saving = true;
    this.notSaved = undefined;
  }

  saveRefused(reason: string): void {
    this.saving = false;
    this.notSaved = reason;
  }
}
