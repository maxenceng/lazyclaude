import { describe, expect, it } from 'vitest';
import { boardFixture, ticketFixture } from './board/fixtures';
import { readFixture, treeFixture } from './docs/fixtures';
import { AppModel } from './app';
import { Action } from './keys/bindings';
import { Dock } from './session/dock';
import { Focus, Panel, WindowTab } from './layout';
import { ScrollStep } from './docs/documentScroll';
import { FaultedConfiguration } from './keys/configurationState';
import { Connecting, Exited, Ready } from './protocol';
import type { OpenReview } from './review/openReview';
import { type FakeClient, fakeClient } from './test/fakeClient';
import { testPlatform } from './testPlatform';

describe('the app', () => {
  it('reads nothing until the sidecar is ready', () => {
    const client = fakeClient(new Connecting());
    new AppModel(client, testPlatform()).start();
    expect(client.calls).toEqual([]);
  });

  it('loads the board when the sidecar becomes ready', () => {
    const client = fakeClient(new Connecting());
    new AppModel(client, testPlatform()).start();
    client.setStatus(new Ready());
    expect(client.calls.map((call) => call.method)).toEqual(['board/watch', 'session/configuration', 'warning/watch']);
  });

  it('loads the board at once when the page reloaded onto a ready sidecar', () => {
    const client = fakeClient(new Ready());
    new AppModel(client, testPlatform()).start();
    expect(client.calls.map((call) => call.method)).toEqual(['board/watch', 'session/configuration', 'warning/watch']);
  });

  it('watches the sessions of the project once the board shows tickets', async () => {
    const client = fakeClient(new Connecting());
    new AppModel(client, testPlatform()).start();
    client.setStatus(new Ready());
    client.answerNext(boardFixture([ticketFixture({ id: 'PROJ-1' }), ticketFixture({ id: 'PROJ-2' })]));
    await Promise.resolve();
    await Promise.resolve();
    expect(client.calls[3]).toEqual({ method: 'session/watch', params: { root: '/p', ticketIds: ['PROJ-1', 'PROJ-2'] } });
  });

  it('loads the board again after a restart reaches ready', () => {
    const client = fakeClient(new Ready());
    new AppModel(client, testPlatform()).start();
    client.setStatus(new Exited(1, []));
    client.setStatus(new Ready());
    expect(client.calls.map((call) => call.method)).toEqual(['board/watch', 'session/configuration', 'warning/watch', 'board/watch', 'session/configuration', 'warning/watch']);
  });

  it('asks for the configuration once ready and reserves the chords it answers', async () => {
    const client = fakeClient(new Ready());
    const app = new AppModel(client, testPlatform());
    app.start();
    expect(app.reserved()).toEqual([]);
    expect(client.calls.map((call) => call.method)).toContain('session/configuration');
    const index = client.calls.findIndex((call) => call.method === 'session/configuration');
    expect(client.calls[index]?.params).toEqual({});
    client.answerNext(boardFixture([]));
    client.answerNext({
      chords: [{ action: 'ZOOM', chord: { modifiers: ['ALT'], key: { kind: 'typedCharacter', codePoint: 120 } } }],
      desktopNotifications: true,
      ticketPlugin: 'ddd-workflow',
      statusVerbs: [],
      mouse: true,
    });
    await Promise.resolve();
    await Promise.resolve();
    expect(app.reserved()).toHaveLength(1);
  });

  it('holds the fault when the configuration cannot be read, reserving no chord', async () => {
    const client = fakeClient(new Ready());
    const app = new AppModel(client, testPlatform());
    app.start();
    client.answerNext(boardFixture([]));
    client.failNext(new Error('refused'));
    await Promise.resolve();
    await Promise.resolve();
    expect(app.configuration.state.get()).toEqual(new FaultedConfiguration('refused'));
    expect(app.reserved()).toEqual([]);
  });

  it('watches the sessions once a board that started empty fills through a change', async () => {
    const client = fakeClient(new Ready());
    new AppModel(client, testPlatform()).start();
    client.answerNext(boardFixture([]));
    await Promise.resolve();
    await Promise.resolve();
    client.push('board/changed', boardFixture([ticketFixture({ id: 'PROJ-1' })]));
    expect(client.calls.map((call) => call.method)).toContain('session/watch');
  });

  it('watches the sessions after a retry that reads the board', async () => {
    const client = fakeClient(new Ready());
    const app = new AppModel(client, testPlatform());
    app.start();
    client.fail('board/watch', new Error('gh timed out'));
    client.answer('session/configuration', { chords: [], desktopNotifications: true, mouse: true, ticketPlugin: 'ddd-workflow', statusVerbs: [] });
    await Promise.resolve();
    await Promise.resolve();
    void app.load();
    client.answer('board/watch', boardFixture([ticketFixture({ id: 'PROJ-1' })]));
    await Promise.resolve();
    await Promise.resolve();
    expect(client.calls.map((call) => call.method)).toContain('session/watch');
  });

  it('reads the docs of the project once the board is read', async () => {
    const client = fakeClient(new Ready());
    new AppModel(client, testPlatform()).start();
    client.answerNext(boardFixture([ticketFixture({ id: 'PROJ-1' })]));
    await Promise.resolve();
    await Promise.resolve();
    expect(client.calls.map((call) => call.method)).toContain('docs/watch');
    expect(client.calls.find((call) => call.method === 'docs/watch')?.params).toEqual({ root: '/p' });
  });

  it('shows the document a ticket\'s link opens in the main view, focusing the Docs panel', async () => {
    const client = fakeClient(new Ready());
    const app = new AppModel(client, testPlatform());
    app.start();
    client.answerNext(boardFixture([ticketFixture({ id: 'PROJ-1' })]));
    await Promise.resolve();
    await Promise.resolve();
    client.answerNext(treeFixture([readFixture('README.md', '# Docs'), readFixture('guide.md', '# Guide')]));
    await Promise.resolve();
    app.docs.followFromTicket('docs/guide.md');
    expect(app.layout.get().focus).toBe(Focus.Docs);
    expect(app.layout.get().showsDocument()).toBe(true);
    expect(app.docs.selectedKey()?.path).toBe('guide.md');
  });
});

const docsApp = async () => {
  const client = fakeClient(new Ready());
  const app = new AppModel(client, testPlatform());
  app.start();
  client.answerNext(boardFixture([ticketFixture({ id: 'PROJ-1' })]));
  await Promise.resolve();
  await Promise.resolve();
  client.answerNext(treeFixture([readFixture('README.md', '# Docs'), readFixture('guide.md', '# Guide')]));
  await Promise.resolve();
  return app;
};

const hostedApp = async () => {
  const app = await docsApp();
  app.hosting.dock.set(new Dock().opened('h1'));
  return app;
};

describe('the app and the left column\'s panel tabs', () => {
  it('switches to the Docs tab on o, focusing it, and back to the Board on o again', async () => {
    const app = await docsApp();
    app.press(Action.TogglePanel);
    expect(app.layout.get().panel).toBe(Panel.Docs);
    expect(app.layout.get().focus).toBe(Focus.Docs);
    app.press(Action.TogglePanel);
    expect(app.layout.get().panel).toBe(Panel.Board);
    expect(app.layout.get().focus).toBe(Focus.Board);
  });

  it('keeps each tab\'s selection across switches', async () => {
    const app = await docsApp();
    app.focusOn(Focus.Docs);
    app.docs.move(1);
    const document = app.docs.selectedKey()?.path;
    app.press(Action.TogglePanel);
    app.press(Action.TogglePanel);
    expect(app.docs.selectedKey()?.path).toBe(document);
    expect(app.board.selectedTicket()?.id).toBe('PROJ-1');
  });

  it('shows the Docs tab when focus goes down from the Board, and the Board tab on h', async () => {
    const app = await docsApp();
    app.press(Action.FocusDown);
    expect(app.layout.get().panel).toBe(Panel.Docs);
    app.press(Action.PanelLeft);
    expect(app.layout.get().panel).toBe(Panel.Board);
  });
});

describe('the app and a shown document', () => {
  it('moves focus from the Docs panel to the shown document on l, and back on h with the document kept', async () => {
    const app = await docsApp();
    app.focusOn(Focus.Docs);
    app.press(Action.PanelRight);
    expect(app.layout.get().focus).toBe(Focus.Document);
    app.press(Action.PanelLeft);
    expect(app.layout.get().focus).toBe(Focus.Docs);
    expect(app.layout.get().showsDocument(), 'going back to the Docs panel keeps the document it shows').toBe(true);
  });

  it('scrolls the focused document a line on j and k and half a page on ctrl+d and ctrl+u, leaving the Docs cursor where it is', async () => {
    const app = await docsApp();
    app.focusOn(Focus.Docs);
    const cursor = app.docs.selectedKey();
    app.focusOn(Focus.Document);
    const steps = [Action.MoveDown, Action.MoveUp, Action.HalfPageDown, Action.HalfPageUp].map((action) => {
      app.press(action);
      return app.docs.scroll.get().step;
    });
    expect(steps).toEqual([ScrollStep.LineDown, ScrollStep.LineUp, ScrollStep.HalfPageDown, ScrollStep.HalfPageUp]);
    expect(app.docs.selectedKey()).toEqual(cursor);
  });

  it('closes the focused document on escape and hands focus to the ticket', async () => {
    const app = await docsApp();
    app.focusOn(Focus.Docs);
    app.focusOn(Focus.Document);
    app.press(Action.Back);
    expect(app.layout.get().showsDocument()).toBe(false);
    expect(app.layout.get().focus).toBe(Focus.Sessions);
  });

  it('goes up from the dock to the document it keeps shown', async () => {
    const app = await hostedApp();
    app.focusOn(Focus.Docs);
    app.focusOn(Focus.Dock);
    app.press(Action.FocusUp);
    expect(app.layout.get().focus).toBe(Focus.Document);
  });

  it('closes a shown document on escape and hands focus to the ticket, and goes to the Board on a second escape', async () => {
    const app = await docsApp();
    app.focusOn(Focus.Docs);
    app.press(Action.Back);
    expect(app.layout.get().showsDocument(), 'the first escape closes the document').toBe(false);
    expect(app.layout.get().focus).toBe(Focus.Sessions);
    app.press(Action.Back);
    expect(app.layout.get().focus).toBe(Focus.Board);
  });

  it('goes to the Board on the first escape while the main view, and so the document, is hidden', async () => {
    const app = await hostedApp();
    app.focusOn(Focus.Docs);
    app.press(Action.ToggleMainView);
    app.press(Action.Back);
    expect(app.layout.get().focus).toBe(Focus.Board);
  });

  it('goes to the Board on escape when the Docs panel shows no document', async () => {
    const app = await docsApp();
    app.focusOn(Focus.Docs);
    app.press(Action.Back);
    app.focusOn(Focus.Docs);
    app.press(Action.Back);
    app.focusOn(Focus.Board);
    app.press(Action.Back);
    expect(app.layout.get().focus).toBe(Focus.Board);
  });

  it('shows a document again when the Docs cursor moves or enter is pressed', async () => {
    const app = await docsApp();
    app.focusOn(Focus.Docs);
    app.press(Action.Back);
    app.focusOn(Focus.Docs);
    app.press(Action.MoveDown);
    expect(app.layout.get().showsDocument()).toBe(true);
    app.press(Action.Back);
    app.focusOn(Focus.Docs);
    app.press(Action.Open);
    expect(app.layout.get().showsDocument()).toBe(true);
  });
});

describe('the app and the dock\'s zoom button', () => {
  it('focuses the dock from another pane and zooms it, and a second press leaves the zoom', async () => {
    const app = await hostedApp();
    app.focusOn(Focus.Board);
    app.toggleZoom();
    expect(app.layout.get().focus).toBe(Focus.Dock);
    expect(app.layout.get().zoomed).toBe(true);
    app.toggleZoom();
    expect(app.layout.get().zoomed).toBe(false);
  });
});

describe('the app and a minimised dock', () => {
  it('minimises on M from a panel and restores on a second M', async () => {
    const app = await hostedApp();
    app.press(Action.ToggleDockMinimised);
    expect(app.layout.get().dockMinimised).toBe(true);
    app.press(Action.ToggleDockMinimised);
    expect(app.layout.get().dockMinimised).toBe(false);
  });

  it('leaves M to the session while a hosted session\'s pane has focus', async () => {
    const app = await hostedApp();
    app.focusOn(Focus.Dock);
    app.press(Action.ToggleDockMinimised);
    expect(app.layout.get().dockMinimised).toBe(false);
  });

  it('does nothing on M with no hosted session', async () => {
    const app = await docsApp();
    app.press(Action.ToggleDockMinimised);
    expect(app.layout.get().dockMinimised).toBe(false);
  });

  it('restores the dock before the focus-down chord focuses it', async () => {
    const app = await hostedApp();
    app.focusOn(Focus.Sessions);
    app.press(Action.ToggleDockMinimised);
    app.press(Action.FocusDown);
    expect(app.layout.get().focus).toBe(Focus.Dock);
    expect(app.layout.get().dockMinimised).toBe(false);
  });

  it('restores the dock on w and focuses it', async () => {
    const app = await hostedApp();
    app.focusOn(Focus.Sessions);
    app.press(Action.ToggleDockMinimised);
    app.press(Action.NextWaiting);
    expect(app.layout.get().dockMinimised).toBe(false);
    expect(app.layout.get().focus).toBe(Focus.Dock);
  });

  it('leaves focus where it is on w with no hosted session', async () => {
    const app = await docsApp();
    app.focusOn(Focus.Sessions);
    app.press(Action.NextWaiting);
    expect(app.layout.get().focus).toBe(Focus.Sessions);
  });

  it('restores a minimised dock when a session opens', async () => {
    const app = await hostedApp();
    app.press(Action.ToggleDockMinimised);
    app.hosting.dock.set(app.hosting.dock.get().opened('h2'));
    app.focusOn(Focus.Dock);
    expect(app.layout.get().dockMinimised).toBe(false);
  });

  it('leaves the no-dock layout when the last session exits, the next session\'s dock opening unfolded', async () => {
    const app = await hostedApp();
    app.press(Action.ToggleDockMinimised);
    app.hosting.dock.set(new Dock());
    expect(app.layout.get().dockMinimised).toBe(false);
    app.hosting.dock.set(new Dock().opened('h2'));
    expect(app.layout.get().dockMinimised).toBe(false);
  });

  it('moves a focused dock\'s focus to the sessions when it is minimised', async () => {
    const app = await hostedApp();
    app.focusOn(Focus.Dock);
    app.layout.set(app.layout.get().toggleDockMinimised(Focus.Sessions));
    expect(app.layout.get().focus).toBe(Focus.Sessions);
  });

  it('keeps m and M exclusive, the last pressed winning', async () => {
    const app = await hostedApp();
    app.press(Action.ToggleDockMinimised);
    app.press(Action.ToggleMainView);
    expect(app.layout.get().dockMinimised).toBe(false);
    expect(app.layout.get().mainHidden).toBe(true);
    app.press(Action.ToggleDockMinimised);
    expect(app.layout.get().mainHidden).toBe(false);
    expect(app.layout.get().dockMinimised).toBe(true);
  });
});

const settled = () => new Promise((resolve) => setTimeout(resolve, 0));

const reviewing = async () => {
  const client = fakeClient(new Ready());
  const app = new AppModel(client, testPlatform());
  app.start();
  client.answer('board/watch', boardFixture([ticketFixture({ id: 'PROJ-1', branch: 'proj-1-first' }), ticketFixture({ id: 'PROJ-2', branch: 'proj-2-second' })]));
  await settled();
  client.answer('review/watch', {
    kind: 'openPullRequests',
    pullRequests: [{ number: 7, title: 'Add the thing', head: 'proj-1-first', draft: false, checks: [] }],
  });
  await settled();
  client.calls.length = 0;
  return { client, app };
};

const PULL_REQUEST = { number: 7, title: 'Add the thing', head: 'proj-1-first', draft: false, checks: [] };

const opened = async (client: FakeClient, app: AppModel) => {
  app.press(Action.Review);
  client.answer('review/pullRequestOn', { kind: 'headedPullRequest', pullRequest: PULL_REQUEST });
  await settled();
};

const placed = async (client: FakeClient) => {
  client.answerAll('review/commentsInDiff', { threads: [], pending: [], underLines: [], listed: [], lastOnLines: [] });
  client.answerAll('review/summaryToSubmit', '');
  await settled();
};

const summaryKept = async (client: FakeClient, current: OpenReview | undefined) => {
  const opening = current?.openSummary();
  client.answer('review/summaryToSubmit', '');
  await opening;
  current?.saveBox();
  client.answerAll('review/summaryToSubmit', '');
  await settled();
};

describe('the app and a review', () => {
  it('watches the open pull requests of the project once the board is read', async () => {
    const client = fakeClient(new Ready());
    new AppModel(client, testPlatform()).start();
    client.answer('board/watch', boardFixture([ticketFixture({ id: 'PROJ-1' })]));
    await settled();
    expect(client.calls.find((call) => call.method === 'review/watch')?.params).toEqual({ root: '/p' });
  });

  it('opens the review of the selected ticket\'s pull request on p, focusing its diff', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    expect(app.review.isOpen()).toBe(true);
    expect(app.layout.get().focus).toBe(Focus.Diff);
    expect(client.calls.map((call) => call.method)).toEqual(['review/pullRequestOn', 'review/pendingOf', 'review/diffOf', 'review/threadsOf']);
  });

  it('opens nothing on p for a ticket whose branch heads no open pull request', async () => {
    const { client, app } = await reviewing();
    app.press(Action.MoveDown);
    app.press(Action.Review);
    expect(client.calls).toEqual([{ method: 'review/pullRequestOn', params: { pullRequests: { kind: 'openPullRequests', pullRequests: [PULL_REQUEST] }, branch: 'proj-2-second' } }]);
    client.answer('review/pullRequestOn', { kind: 'noHeadedPullRequest' });
    await settled();
    expect(app.review.isOpen()).toBe(false);
  });

  it('goes back to the Board on escape, and leaves the zoom first', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    app.press(Action.Back);
    expect(app.review.isOpen()).toBe(false);
    expect(app.layout.get().focus).toBe(Focus.Board);
  });

  it('moves focus between Files, Comments and the diff with the panel keys', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    app.press(Action.PanelLeft);
    expect(app.layout.get().focus).toBe(Focus.Files);
    app.press(Action.FocusDown);
    expect(app.layout.get().focus).toBe(Focus.Comments);
    app.press(Action.FocusUp);
    expect(app.layout.get().focus).toBe(Focus.Files);
    app.press(Action.PanelRight);
    expect(app.layout.get().focus).toBe(Focus.Diff);
  });

  it('takes j and k as the cursor of the diff, and of Files and Comments once they have focus', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    const current = app.review.current.get();
    client.answer('review/diffOf', {
      kind: 'readDiff',
      files: [
        { path: 'a.ts', status: { kind: 'modifiedFile' }, hunks: [{ header: '@@', lines: [{ kind: 'addedLine', content: 'a', newNumber: 1 }, { kind: 'addedLine', content: 'b', newNumber: 2 }] }] },
        { path: 'b.ts', status: { kind: 'modifiedFile' }, hunks: [{ header: '@@', lines: [{ kind: 'addedLine', content: 'c', newNumber: 1 }] }] },
      ],
    });
    await settled();
    await placed(client);
    app.press(Action.MoveDown);
    expect(current?.cursor()).toBe(1);
    app.press(Action.PanelLeft);
    app.press(Action.MoveDown);
    expect(current?.selectedFile()).toBe(1);
    app.press(Action.Open);
    expect(current?.cursor()).toBe(2);
  });

  it('asks where a comment would hang on c in the diff, and toggles resolved threads on r', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    const current = app.review.current.get();
    client.answer('review/diffOf', {
      kind: 'readDiff',
      files: [{ path: 'a.ts', status: { kind: 'modifiedFile' }, hunks: [{ header: '@@', lines: [{ kind: 'addedLine', content: 'a', newNumber: 1 }] }] }],
    });
    await settled();
    await placed(client);
    client.calls.length = 0;
    app.press(Action.Capture);
    expect(client.calls.map((call) => call.method)).toEqual(['review/anchorOf']);
    app.press(Action.Resolved);
    expect(current?.resolvedShown()).toBe(true);
    expect(client.calls.at(-2)).toMatchObject({ method: 'review/commentsInDiff', params: { resolvedShown: true } });
  });

  it('answers the confirm after the summary with y, and dismisses it with any other key', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    const current = app.review.current.get();
    await summaryKept(client, current);
    client.calls.length = 0;
    app.press(Action.MoveDown);
    expect(current?.submitPrompt()).toBe(false);
    expect(client.calls).toEqual([]);
    await summaryKept(client, current);
    client.calls.length = 0;
    app.press(Action.Yes);
    client.answerAll('review/summaryToSubmit', '');
    await settled();
    expect(client.calls.map((call) => call.method)).toEqual(['review/summaryToSubmit', 'review/submit']);
  });

  it('opens the summary on S, and ignores the keys of the diff while a box is open', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    const current = app.review.current.get();
    app.press(Action.Submit);
    client.answer('review/summaryToSubmit', '');
    await settled();
    expect(current?.box()).toBeDefined();
    app.press(Action.Back);
    expect(app.review.isOpen()).toBe(true);
  });

  it('leaves the review when the submit is taken', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    const current = app.review.current.get();
    await summaryKept(client, current);
    app.press(Action.Yes);
    client.answerAll('review/summaryToSubmit', '');
    await settled();
    client.answer('review/submit', { kind: 'writtenReview', pending: { kind: 'noPendingReview' } });
    await settled();
    expect(app.review.isOpen()).toBe(false);
    expect(app.layout.get().focus).toBe(Focus.Board);
  });
});

describe('the app and its window tabs', () => {
  it('opens on Base and chooses a tab on its digit', async () => {
    const { app } = await reviewing();
    expect(app.tabs.selected.get()).toBe(WindowTab.Base);
    app.press(Action.ShowPullRequests);
    expect(app.tabs.selected.get()).toBe(WindowTab.PullRequests);
    app.press(Action.ShowBoard);
    expect(app.tabs.selected.get()).toBe(WindowTab.Board);
    app.press(Action.ShowBase);
    expect(app.tabs.selected.get()).toBe(WindowTab.Base);
  });

  it('ends an open review and shows the tab chosen by its digit or a click', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    app.press(Action.ShowBoard);
    expect(app.review.isOpen()).toBe(false);
    expect(app.tabs.selected.get()).toBe(WindowTab.Board);
    await opened(client, app);
    app.press(Action.ShowBase);
    app.press(Action.Review);
    client.answer('review/pullRequestOn', { kind: 'headedPullRequest', pullRequest: PULL_REQUEST });
    await settled();
    app.chooseTab(WindowTab.PullRequests);
    expect(app.review.isOpen()).toBe(false);
    expect(app.tabs.selected.get()).toBe(WindowTab.PullRequests);
  });

  it('chooses no tab while the review\'s comment box or submit prompt is open', async () => {
    const { client, app } = await reviewing();
    await opened(client, app);
    const current = app.review.current.get();
    app.press(Action.Submit);
    client.answer('review/summaryToSubmit', '');
    await settled();
    app.chooseTab(WindowTab.Board);
    expect(app.review.isOpen(), 'a box holds what is not written yet').toBe(true);
    current?.cancelBox();
    await summaryKept(client, current);
    app.chooseTab(WindowTab.Board);
    expect(app.review.isOpen(), 'the submit prompt waits for its answer').toBe(true);
    expect(app.tabs.selected.get()).toBe(WindowTab.Base);
  });

  it('routes j and enter to the Board tab when it is shown, not to the Base panes', async () => {
    const { app } = await reviewing();
    app.press(Action.ShowBoard);
    app.press(Action.MoveDown);
    expect(app.board.selection.selected.get()).toBe('PROJ-2');
    app.press(Action.MoveUp);
    app.press(Action.MoveDown);
    app.press(Action.Open);
    expect(app.tabs.selected.get()).toBe(WindowTab.Base);
    expect(app.board.selection.selected.get()).toBe('PROJ-2');
    expect(app.layout.get().focus).toBe(Focus.Board);
  });

  it('returns to the Pull requests tab and its row when the review opened from it ends', async () => {
    const { client, app } = await reviewing();
    app.press(Action.ShowPullRequests);
    app.press(Action.Open);
    await settled();
    expect(app.review.current.get()?.number()).toBe(7);
    expect(app.layout.get().focus).toBe(Focus.Diff);
    app.review.leave();
    expect(app.tabs.selected.get()).toBe(WindowTab.PullRequests);
    expect(app.review.cursor.selected.get()).toBe(7);
    expect(app.layout.get().focus).toBe(Focus.Board);
    expect(client.calls.length).toBeGreaterThan(0);
  });

  it('asks review for the tickets\' pull requests again when the pull requests change on the Board tab', async () => {
    const { client, app } = await reviewing();
    app.press(Action.ShowBoard);
    client.calls.length = 0;
    client.push('review/changed', { kind: 'openPullRequests', pullRequests: [] });
    expect(client.calls.map((call) => call.method)).toEqual(['review/pullRequestOn', 'review/pullRequestOn']);
  });
});
