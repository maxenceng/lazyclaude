import { describe, expect, it } from 'vitest';
import { BoardController } from './board/boardController';
import { boardFixture, ticketFixture } from './board/fixtures';
import { Action } from './keys/bindings';
import { Focus, Layout, WindowTab } from './layout';
import { pullRequestFixture } from './review/fixtures';
import { ReviewController } from './review/reviewController';
import { NoticeStore } from './shared/noticeStore';
import { Store } from './shared/store';
import { fakeClient } from './test/fakeClient';
import { WindowTabs } from './windowTabs';

const settled = () => new Promise((resolve) => setTimeout(resolve, 0));

const board = boardFixture([
  ticketFixture({ id: 'PROJ-1', branch: 'proj-1-first', status: 'IN_PROGRESS' }),
  ticketFixture({ id: 'PROJ-2', branch: 'proj-2-second', status: 'TODO' }),
  ticketFixture({ id: 'PROJ-3', branch: 'proj-3-third', status: 'TODO' }),
]);

const setup = async () => {
  const client = fakeClient();
  const layout = new Store(new Layout());
  const boardController = new BoardController(client);
  const loading = boardController.load();
  client.answerNext(board);
  await loading;
  const review = new ReviewController({ client, root: () => '/p', notices: new NoticeStore(), onOpened: () => {}, onLeft: () => {} });
  const watching = review.pullRequests.watch('/p');
  client.answerNext({
    kind: 'openPullRequests',
    pullRequests: [
      pullRequestFixture({ number: 1, head: 'passed', checks: [{ name: 'b', outcome: 'PASSED' }] }),
      pullRequestFixture({ number: 2, head: 'failed', checks: [{ name: 'b', outcome: 'FAILED' }] }),
    ],
  });
  await watching;
  client.calls.length = 0;
  return { client, layout, board: boardController, review, tabs: new WindowTabs({ board: boardController, review, layout }) };
};

describe('the window tabs', () => {
  it('open on Base', async () => {
    expect((await setup()).tabs.selected.get()).toBe(WindowTab.Base);
  });

  it('choose a tab, taking the focus to the Board pane the tabs draw over', async () => {
    const { tabs, layout } = await setup();
    layout.set(layout.get().focusOn(Focus.Sessions));
    tabs.choose(WindowTab.PullRequests);
    expect(tabs.selected.get()).toBe(WindowTab.PullRequests);
    expect(layout.get().focus).toBe(Focus.Board);
  });

  it('ask review for the pull request of each ticket when the Board tab is chosen', async () => {
    const { tabs, client } = await setup();
    tabs.choose(WindowTab.Board);
    expect(client.calls.map((call) => call.params)).toMatchObject([{ branch: 'proj-1-first' }, { branch: 'proj-2-second' }, { branch: 'proj-3-third' }]);
  });

  it('ask again when the pull requests change while the Board tab is shown, and not on Base', async () => {
    const { tabs, client, review } = await setup();
    review.pullRequests.state.set({ kind: 'openPullRequests', pullRequests: [] });
    tabs.pullRequestsChanged();
    expect(client.calls).toEqual([]);
    tabs.choose(WindowTab.Board);
    client.calls.length = 0;
    tabs.pullRequestsChanged();
    expect(client.calls).toHaveLength(3);
  });

  it('move the cursor over the columns on the Board tab and go to Base on enter', async () => {
    const { tabs, board: boardController, layout } = await setup();
    tabs.choose(WindowTab.Board);
    tabs.press(Action.PanelRight);
    expect(boardController.selection.selected.get()).toBe('PROJ-2');
    tabs.press(Action.MoveDown);
    expect(boardController.selection.selected.get()).toBe('PROJ-3');
    tabs.press(Action.PanelLeft);
    expect(boardController.selection.selected.get()).toBe('PROJ-1');
    tabs.press(Action.PanelRight);
    tabs.press(Action.Open);
    expect(tabs.selected.get()).toBe(WindowTab.Base);
    expect(boardController.selection.selected.get()).toBe('PROJ-2');
    expect(layout.get().focus).toBe(Focus.Board);
  });

  it('go to Base with a ticket selected when it is clicked', async () => {
    const { tabs, board: boardController } = await setup();
    tabs.choose(WindowTab.Board);
    tabs.openTicket('PROJ-3');
    expect(tabs.selected.get()).toBe(WindowTab.Base);
    expect(boardController.selection.selected.get()).toBe('PROJ-3');
  });

  it('list the pull requests worst first, move over them and open the review of the one under the cursor on enter', async () => {
    const { tabs, review } = await setup();
    tabs.choose(WindowTab.PullRequests);
    expect(tabs.pullRequests().map((pullRequest) => pullRequest.number)).toEqual([2, 1]);
    tabs.press(Action.MoveDown);
    tabs.press(Action.Open);
    expect(review.current.get()?.number()).toBe(1);
    expect(tabs.selected.get()).toBe(WindowTab.PullRequests);
  });

  it('list nothing while the pull requests are not read', async () => {
    const { tabs, review } = await setup();
    review.pullRequests.state.set({ kind: 'unavailablePullRequests', reason: 'gh failed' });
    expect(tabs.pullRequests()).toEqual([]);
  });

  it('go back to Base on escape from either tab', async () => {
    const { tabs } = await setup();
    tabs.choose(WindowTab.PullRequests);
    tabs.press(Action.Back);
    expect(tabs.selected.get()).toBe(WindowTab.Base);
    await settled();
  });
});
