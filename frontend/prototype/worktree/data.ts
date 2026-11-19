// PROTOTYPE: throwaway canned data for the worktree variants. Not shipped.
import { boardFixture, ticketFixture } from '../../src/board/fixtures';

export const STATES = ['clean', 'held', 'nomerge', 'nosessions', 'unreadable', 'reading', 'confirm'] as const;
export type WorktreeCase = (typeof STATES)[number];

export const PATH = '../lazyclaude.worktrees/211-the-bundle-job-caches-the-temurin-jdk';

/** What the TUI's WorktreeText says for each case, word for word. */
export const STATE_WORDS: Record<WorktreeCase, string> = {
  clean: 'clean',
  confirm: 'clean',
  held: '2 dirty paths, 1 commit beyond the merge, 1 live session',
  nomerge: 'no merged pull request',
  nosessions: 'no sessions read',
  unreadable: 'unreadable (git worktree list exited 128)',
  reading: 'reading…',
};

export const isClear = (state: WorktreeCase) => state === 'clean' || state === 'confirm';

const BODY = `## Acceptance criteria

- The bundle job restores the Temurin JDK and Tauri's AppImage tools from the cache keyed on their pins.
- A 504 from GitHub's release download no longer fails the job when the cache holds the file.

## Notes

The cache key is the pin, so bumping a pin downloads once.`;

export const BOARD = boardFixture([
  ticketFixture({ id: '188', title: 'A click on a status line lamp or count goes to its session', status: 'IN_PROGRESS', type: { kind: 'typeName', value: 'feature' }, context: { kind: 'contextName', value: 'session' } }),
  ticketFixture({ id: '145', title: 'pty4j leaves a `/tmp/pty4j-linux-x86-64-*` directory behind on every run', status: 'TODO', type: { kind: 'typeName', value: 'fix' }, context: { kind: 'contextName', value: 'session' } }),
  ticketFixture({
    id: '211',
    branch: '211-the-bundle-job-caches-the-temurin-jdk',
    title: 'The bundle job caches the Temurin JDK and AppRun downloads so a GitHub 504 does not fail it',
    status: 'DONE',
    type: { kind: 'typeName', value: 'chore' },
    context: { kind: 'contextName', value: 'shared' },
    body: { kind: 'body', value: BODY },
  }),
  ticketFixture({ id: '189', title: 'Markdown stays readable on a large screen', status: 'DONE', type: { kind: 'typeName', value: 'feature' }, context: { kind: 'contextName', value: 'docs' } }),
]);

export const SELECTED = '211';
