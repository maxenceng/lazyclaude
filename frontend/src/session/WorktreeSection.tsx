import { useTranslation } from 'react-i18next';
import { LampColour, PillInk, ReadingSection, Rows, SessionRow, Text, Tone } from '../design-system';
import type { SessionWorktreeShownView } from '../protocol';

interface Props {
  /** The selected ticket's worktree as read; none draws no section. */
  shown: Exclude<SessionWorktreeShownView, { kind: 'noWorktree' }> | undefined;
  /** The ticket's branch, which stands in the path's place where the worktree could not be read. */
  branch: string;
  /** Why the last removal removed nothing. */
  refusal: string | undefined;
  /** The row pressed while its worktree is clean: the question that removes it. */
  onPress: () => void;
}

enum Pill {
  Clean = 'clean',
  Held = 'held',
  Unknown = 'unknown',
}

/** The worktree's state as the pill says it: clean, held for what is in it, unknown where that could not be judged. */
const pillOf = (shown: NonNullable<Props['shown']>): Pill => {
  switch (shown.kind) {
    case 'clearWorktree':
      return Pill.Clean;
    case 'heldBackWorktree':
      return shown.state.kind === 'worktreeContents' ? Pill.Held : Pill.Unknown;
    case 'unreadWorktree':
      return Pill.Unknown;
  }
};

const labelOf = (shown: NonNullable<Props['shown']>, branch: string): string => (shown.kind === 'unreadWorktree' ? branch : shown.shown);

/** A done ticket's worktree after its sessions: one row like a session's, its pill, its path and `remove d` while it is clean; while it is not, the reason in a dim line under the row and no remove. */
export const WorktreeSection = ({ shown, branch, refusal, onPress }: Props) => {
  const { t } = useTranslation('session');
  if (shown === undefined) {
    return null;
  }
  const clean = shown.kind === 'clearWorktree';
  const pill = pillOf(shown);
  return (
    <ReadingSection title={t('worktree.title')}>
      <Rows>
        <SessionRow
          label={labelOf(shown, branch)}
          state={t(`worktree.${pill}`)}
          pill={clean ? PillInk.Clean : PillInk.Lamp}
          action={clean ? { label: t('worktree.remove'), shortcut: t('worktree.removeKey') } : undefined}
          selected={false}
          lamp={LampColour.Off}
          onSelect={clean ? onPress : undefined}
        />
      </Rows>
      {clean ? null : (
        <Text tone={Tone.Dim} tight>
          {shown.held}
        </Text>
      )}
      {refusal === undefined ? null : (
        <div role="alert">
          <Text tone={Tone.Warning} tight>
            {refusal}
          </Text>
        </div>
      )}
    </ReadingSection>
  );
};
