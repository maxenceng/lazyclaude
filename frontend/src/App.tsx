import type { TFunction } from 'i18next';
import { type ReactNode, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import type { AppModel } from './app';
import { BoardKeyBar, BoardTabKeyBar, CaptureKeyBar } from './board/BoardKeyBar';
import { CapturePopup } from './board/CapturePopup';
import { BoardColumns, BoardPane, TicketPane } from './board/BoardScreen';
import { inProgressCount, ticketTotal } from './board/rows';
import { statusColourOf } from './board/statusColours';
import { ConnectionScreen } from './connection/ConnectionScreen';
import { DocsKeyBar, DocumentKeyBar } from './docs/DocsKeyBar';
import { DocsPane, DocumentPane } from './docs/DocsScreen';
import { Alert, AppFrame, Arrangement, Button, CiGlyph, Count, Lamp, LampSize, Pane, Popup, StatusLine, Text, Tone, WindowMenu, WindowTabBar } from './design-system';
import { ConfigurationKind } from './keys/configurationState';
import { Action } from './keys/bindings';
import { ModalClicks } from './keys/modalClicks';
import { WindowKeys } from './keys/windowKeys';
import { Focus, type Panel, WindowTab } from './layout';
import { PullRequestRead, type TicketPullRequest } from './board/ticketPullRequest';
import { StatusKind } from './protocol';
import { cellsOf, ciOf, worstResult } from './review/pullRequestList';
import { PullRequestsPane } from './review/PullRequestsScreen';
import { DiffPane, FilesPane, ReviewPopups } from './review/ReviewScreen';
import { PullRequestsKeyBar, ReviewFocus, ReviewKeyBar } from './review/ReviewKeyBar';
import { DockScreen, DockStrip } from './session/DockScreen';
import { LampColour, markerOfTicket, markersOf } from './session/markers';
import { DockKeyBar, SessionsKeyBar } from './session/SessionsKeyBar';
import { NewSessionBar, SessionFailure, SessionsList } from './session/SessionsList';
import { ticketPullRequestOf } from './ticketPullRequest';
import { useStore } from './shared/useStore';

const TAB_OF: ReadonlyMap<string, WindowTab> = new Map<string, WindowTab>(Object.values(WindowTab).map((tab) => [tab, tab]));

const PANEL_OF: ReadonlyMap<string, Panel> = new Map<string, Panel>([
  [Focus.Board, Focus.Board],
  [Focus.Docs, Focus.Docs],
]);

const NO_PULL_REQUEST: TicketPullRequest = { read: PullRequestRead.None, number: undefined };

const COLOURS = [LampColour.Permission, LampColour.Input, LampColour.Turn] as const;

/** What stands in for the Board while the backend is not there or its configuration is not read: `alternative`, and no ticket. */
const unboarded = (alternative: ReactNode, t: TFunction) => ({
  board: (
    <Pane title={t('app.boardTitle')} focused>
      {alternative}
    </Pane>
  ),
  main: (
    <Pane title={t('app.ticketTitle')} focused={false}>
      <Text tone={Tone.Dim}>{t('app.noTicketSelected')}</Text>
    </Pane>
  ),
  docs: null,
  arrangement: Arrangement.NoDock,
});

interface Props {
  app: AppModel;
  packaged: boolean;
}

/** The window: the status line of lamps over the Board, the ticket's sessions and the dock, or over what stands in for the Board while the backend is not there. */
export const App = ({ app, packaged }: Props) => {
  const { t } = useTranslation();
  const status = useStore(app.client.status);
  const sessions = useStore(app.sessionStore.sessions);
  const board = useStore(app.board.store.state);
  const configuration = useStore(app.configuration.state);
  const layout = useStore(app.layout);
  const dock = useStore(app.hosting.dock);
  const picker = useStore(app.sessions.picker);
  const cursor = useStore(app.sessions.cursor);
  const failure = useStore(app.hosting.failure);
  const warning = useStore(app.warnings.warning);
  const notice = useStore(app.notices.notice);
  const mouseReported = useStore(app.hosting.mouseReported);
  const review = useStore(app.review.current);
  const tab = useStore(app.tabs.selected);
  const pullRequests = useStore(app.review.pullRequests.state);
  const capturing = useStore(app.board.capture.state).isOpen();
  const quitAsked = useStore(app.quitting.asking);
  useStore(app.board.selection.selected);
  useStore(app.review.cursor.selected);
  useStore(app.review.ticketPullRequests.joined);
  useStore(app.docs.store.state);

  useEffect(() => {
    const keys = new WindowKeys((action) => app.press(action), { packaged, reserved: () => app.reserved() });
    const clicks = new ModalClicks(() => app.isModal());
    return () => {
      keys.remove();
      clicks.remove();
    };
  }, [app, packaged]);

  const markers = sessions === undefined ? [] : markersOf(sessions);
  const project = app.board.root() ?? t('app.project');
  const selectedTicket = app.board.selectedTicket();
  const populatedBoard = board.shown();
  const hasDock = dock.tabs.length > 0;
  const dockKey = layout.mainHidden ? 'keyBar.showTicket' : layout.dockMinimised ? 'keyBar.restoreDock' : 'keyBar.minimiseDock';
  const dockKeys = hasDock ? [t(dockKey)] : [];
  const windowTabKeys = [t('keyBar.tabs')];
  const tabKeys = [...windowTabKeys, ...dockKeys];
  const panelKeys = [t(layout.panel === Focus.Board ? 'keyBar.panelDocs' : 'keyBar.panelBoard'), ...tabKeys];
  const panelTabs = {
    tabs: [
      { id: Focus.Board, label: t('panels.board'), count: populatedBoard === undefined ? 0 : ticketTotal(populatedBoard) },
      { id: Focus.Docs, label: t('panels.docs'), count: app.docs.count() },
    ],
    selected: layout.panel,
    onSelect: (id: string) => app.showPanel(PANEL_OF.get(id) ?? Focus.Board),
  };
  const keyBar = {
    [Focus.Board]: <BoardKeyBar ticket={selectedTicket !== undefined} capture={app.board.root() !== undefined} extra={panelKeys} />,
    [Focus.Docs]: <DocsKeyBar documentShown={layout.showsDocumentInView()} extra={panelKeys} />,
    [Focus.Document]: <DocumentKeyBar extra={tabKeys} />,
    [Focus.Sessions]: <SessionsKeyBar picking={false} extra={panelKeys} />,
    [Focus.Files]: <ReviewKeyBar panel={ReviewFocus.Files} />,
    [Focus.Diff]: <ReviewKeyBar panel={ReviewFocus.Diff} />,
    [Focus.Dock]: <DockKeyBar mouseReported={mouseReported} />,
  }[layout.focus];

  const unready =
    status.kind !== StatusKind.Ready ? (
      <ConnectionScreen status={status} onRestart={() => void app.client.restart()} />
    ) : configuration.kind === ConfigurationKind.Fault ? (
      <Alert title={t('configuration.fault')} action={<Button onClick={() => void app.load()}>{t('configuration.retry')}</Button>}>
        {configuration.message}
      </Alert>
    ) : undefined;

  const reviewing =
    review === undefined
      ? undefined
      : {
          board: <FilesPane review={review} focused={layout.focus === Focus.Files} onFocus={() => app.focusOn(Focus.Files)} />,
          docs: null,
          main: <DiffPane review={review} focused={layout.focus === Focus.Diff} onFocus={() => app.focusOn(Focus.Diff)} onRetry={() => review.read()} onClose={() => app.review.leave()} />,
          arrangement: layout.arrangement(hasDock),
        };

  const pullRequestList = app.tabs.pullRequests();
  const tabScreen =
    tab === WindowTab.Board
      ? {
          bar: <BoardTabKeyBar ticket={selectedTicket !== undefined} capture={app.board.root() !== undefined} extra={windowTabKeys} />,
          pane: (
            <BoardColumns
              board={app.board.store}
              selection={app.board.selection}
              markerOf={(ticket) => (sessions === undefined ? undefined : markerOfTicket(sessions, ticket))}
              pullRequestOf={(ticket) => {
                const pullRequest = app.review.ticketPullRequests.of(ticket.branch);
                return pullRequest === undefined ? undefined : cellsOf(pullRequest);
              }}
              onPress={(ticket, cursor) => app.tabs.pressTicket(ticket, cursor)}
              onRetry={() => void app.load()}
              onCapture={app.board.root() === undefined ? undefined : () => app.capture()}
            />
          ),
          preview: (
            <TicketPane
              board={app.board.store}
              selection={app.board.selection}
              focused={false}
              pullRequest={selectedTicket === undefined ? NO_PULL_REQUEST : ticketPullRequestOf(pullRequests, app.review.ticketPullRequests.of(selectedTicket.branch))}
              onLink={(href) => {
                app.chooseTab(WindowTab.Base);
                app.docs.followFromTicket(href);
              }}
            >
              <SessionsList
                rows={app.sessions.rows()}
                cursor={cursor}
                picker={picker}
                onPress={(index) => app.tabs.pressSessionRow(index)}
              />
            </TicketPane>
          ),
          arrangement: Arrangement.TabPreview,
        }
      : {
          bar: <PullRequestsKeyBar listed={pullRequestList.length > 0} extra={windowTabKeys} />,
          pane: (
            <PullRequestsPane
              state={pullRequests}
              list={pullRequestList}
              cursor={app.review.cursor.cursorIn(pullRequestList)?.number}
              now={new Date()}
              onOpen={(pullRequest) => app.tabs.openPullRequest(pullRequest)}
              onRetry={() => app.retryPullRequests()}
            />
          ),
          preview: null,
          arrangement: Arrangement.Tab,
        };
  const tabbed =
    tab === WindowTab.Base || unready !== undefined || reviewing !== undefined
      ? undefined
      : {
          board: tabScreen.pane,
          docs: null,
          main: tabScreen.preview,
          arrangement: tabScreen.arrangement,
        };

  const shown =
    reviewing ??
    tabbed ??
    (unready === undefined
      ? {
          board:
            layout.panel === Focus.Docs ? (
              <DocsPane
                docs={app.docs}
                focused={layout.focus === Focus.Docs}
                panelTabs={panelTabs}
                onFocus={() => app.focusOn(Focus.Docs)}
                onRetry={() => void app.docs.load()}
              />
            ) : (
            <BoardPane
              board={app.board.store}
              panelTabs={panelTabs}
              selection={app.board.selection}
              focused={layout.focus === Focus.Board}
              markerOf={(ticket) => (sessions === undefined ? undefined : markerOfTicket(sessions, ticket))}
              onOpen={() => app.press(Action.Open)}
              onFocus={() => app.focusOn(Focus.Board)}
              onRetry={() => void app.load()}
              onCapture={app.board.root() === undefined ? undefined : () => app.capture()}
            />
            ),
          docs: null,
          main: layout.showsDocument() ? (
            <DocumentPane
              docs={app.docs}
              focused={layout.focus === Focus.Document}
              onFocus={() => app.focusOn(Focus.Document)}
              onClose={() => app.closeDocument()}
            />
          ) : (
            <TicketPane
              board={app.board.store}
              selection={app.board.selection}
              focused={layout.focus === Focus.Sessions}
              pullRequest={selectedTicket === undefined ? NO_PULL_REQUEST : ticketPullRequestOf(pullRequests, app.review.ticketPullRequests.of(selectedTicket.branch))}
              onLink={(href) => app.docs.followFromTicket(href)}
              alert={<SessionFailure failure={failure} />}
              onHide={hasDock ? () => app.hideMainView() : undefined}
              onFocus={() => app.focusOn(Focus.Sessions)}
              foot={
                <NewSessionBar
                  rows={app.sessions.rows()}
                  cursor={cursor}
                  picker={picker}
                  onPress={(index) => app.pressRow(index)}
                  onPickVerb={(index) => void app.sessions.pick(index)}
                  onDismiss={() => app.sessions.cancelPick()}
                />
              }
            >
              <SessionsList
                rows={app.sessions.rows()}
                cursor={cursor}
                picker={picker}
                onPress={(index) => app.pressRow(index)}
              />
            </TicketPane>
          ),
          arrangement: layout.arrangement(hasDock),
        }
      : unboarded(unready, t));

  const dockScreen =
    tabbed !== undefined ? (
      <DockStrip hosting={app.hosting} sessions={sessions} onSelect={(id) => app.openDockTab(id)} />
    ) : layout.arrangement(hasDock) === Arrangement.Minimised ? (
      <DockStrip hosting={app.hosting} sessions={sessions} onSelect={(id) => app.focusDockTab(id)} onRestore={() => app.toggleDockMinimised()} />
    ) : (
      <DockScreen
        hosting={app.hosting}
        sessions={sessions}
        focused={layout.focus === Focus.Dock}
        onFocus={() => app.focusOn(Focus.Dock)}
        zoomed={layout.zoomed}
        zoomKey={app.zoomKey()}
        onMinimise={() => app.toggleDockMinimised()}
        onZoom={() => app.toggleZoom()}
      />
    );

  return (
    <>
      <AppFrame
        statusLine={
          <StatusLine
            menu={<WindowMenu name={t('app.name')} items={[{ label: t('quit.button'), shortcut: t('quit.key'), onSelect: () => app.quit() }]} />}
            tabs={
              <WindowTabBar
                tabs={[
                  { id: WindowTab.Base, label: t('tabs.base') },
                  {
                    id: WindowTab.Board,
                    label: t('tabs.board'),
                    finding:
                      populatedBoard === undefined ? undefined : (
                        <>
                          <b>{inProgressCount(populatedBoard)}</b> <small>{t('tabs.inProgress')}</small>
                        </>
                      ),
                    findingColour: statusColourOf('IN_PROGRESS'),
                  },
                  {
                    id: WindowTab.PullRequests,
                    label: t('tabs.pullRequests'),
                    finding:
                      pullRequests.kind === 'openPullRequests' ? (
                        <>
                          <b>{pullRequests.pullRequests.length}</b> <small>{t('tabs.open')}</small>
                          {worstResult(pullRequests.pullRequests) === undefined ? null : <CiGlyph ci={ciOf(worstResult(pullRequests.pullRequests))} />}
                        </>
                      ) : undefined,
                  },
                ]}
                selected={tab}
                onSelect={(id) => app.chooseTab(TAB_OF.get(id) ?? WindowTab.Base)}
              />
            }
            tiles={markers.map((marker) => (
              <Lamp key={marker.session} colour={marker.colour} size={LampSize.Tile} number={marker.ticket} />
            ))}
            counts={COLOURS.map((colour) => (
              <Count key={colour} colour={colour} count={markers.filter((marker) => marker.colour === colour).length} />
            ))}
            project={project}
            warning={warning}
            notice={notice}
          />
        }
        keyBar={capturing ? <CaptureKeyBar /> : picker === undefined ? (tabbed === undefined ? keyBar : tabScreen.bar) : <SessionsKeyBar picking />}
        board={shown.board}
        docs={shown.docs}
        main={shown.main}
        dock={dock.tabs.length === 0 ? null : dockScreen}
        arrangement={shown.arrangement}
      />
      {review === undefined ? null : <ReviewPopups review={review} />}
      <CapturePopup capture={app.board.capture} />
      {quitAsked === undefined ? null : <QuitPopup hosted={quitAsked} onAnswer={(yes) => app.quitting.answer(yes)} />}
    </>
  );
};

/** The question `q` asks while sessions are hosted, as the TUI's quit prompt does: y quits, anything else stays. */
const QuitPopup = ({ hosted, onAnswer }: { hosted: number; onAnswer: (yes: boolean) => void }) => {
  const { t } = useTranslation();
  return (
    <Popup
      title={t('quit.title')}
      main={{ label: t('quit.yes'), chip: t('quit.yesKey'), onClick: () => onAnswer(true) }}
      out={{ label: t('quit.no'), chip: t('quit.noKey'), onClick: () => onAnswer(false) }}
      onDismiss={() => onAnswer(false)}
    >
      <Text tight>{t('quit.hosted', { count: hosted })}</Text>
    </Popup>
  );
};
