import type { TFunction } from 'i18next';
import { type ReactNode, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import type { AppModel } from './app';
import { BoardKeyBar, BoardTabKeyBar } from './board/BoardKeyBar';
import { BoardColumns, BoardPane, TicketPane } from './board/BoardScreen';
import { inProgressCount } from './board/rows';
import { ConnectionScreen } from './connection/ConnectionScreen';
import { DocsKeyBar } from './docs/DocsKeyBar';
import { DocsPane, DocumentPane } from './docs/DocsScreen';
import { Alert, AppFrame, Arrangement, Button, CiGlyph, Count, Lamp, LampSize, Pane, StatusLine, Text, Tone, WindowTabs } from './design-system';
import { ConfigurationKind } from './keys/configurationState';
import { Action } from './keys/bindings';
import { WindowKeys } from './keys/windowKeys';
import { Focus, WindowTab } from './layout';
import { StatusKind } from './protocol';
import { cellsOf, ciOf, worstResult } from './review/pullRequestList';
import { PullRequestsPane } from './review/PullRequestsScreen';
import { CommentsPane, DiffPane, FilesPane, ReviewDialogs, ReviewPanel } from './review/ReviewScreen';
import { PullRequestsKeyBar, ReviewFocus, ReviewKeyBar } from './review/ReviewKeyBar';
import { DockScreen, DockStrip } from './session/DockScreen';
import { LampColour, markerOfTicket, markersOf } from './session/markers';
import { DockKeyBar, SessionsKeyBar } from './session/SessionsKeyBar';
import { SessionsList } from './session/SessionsList';
import { useStore } from './shared/useStore';

const TAB_OF: ReadonlyMap<string, WindowTab> = new Map<string, WindowTab>(Object.values(WindowTab).map((tab) => [tab, tab]));

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
  useStore(app.board.selection.selected);
  useStore(app.review.cursor.selected);
  useStore(app.review.tickets.joined);

  useEffect(() => {
    const keys = new WindowKeys((action) => app.press(action), { packaged, reserved: () => app.reserved() });
    return () => keys.remove();
  }, [app, packaged]);

  const markers = sessions === undefined ? [] : markersOf(sessions);
  const project = board.shown()?.root ?? t('app.project');
  const hasDock = dock.tabs.length > 0;
  const tabKeys = [t('keyBar.tabs')];
  const keyBar = {
    [Focus.Board]: <BoardKeyBar extra={tabKeys} />,
    [Focus.Docs]: <DocsKeyBar extra={tabKeys} />,
    [Focus.Sessions]: <SessionsKeyBar picking={false} extra={tabKeys} />,
    [Focus.Files]: <ReviewKeyBar panel={ReviewFocus.Files} />,
    [Focus.Comments]: <ReviewKeyBar panel={ReviewFocus.Comments} />,
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
          docs: <CommentsPane review={review} focused={layout.focus === Focus.Comments} onFocus={() => app.focusOn(Focus.Comments)} />,
          foot: <ReviewPanel review={review} />,
          main: <DiffPane review={review} focused={layout.focus === Focus.Diff} onFocus={() => app.focusOn(Focus.Diff)} onRetry={() => review.read()} />,
          arrangement: layout.arrangement(hasDock),
        };

  const populatedBoard = board.shown();
  const pullRequestList = app.tabs.pullRequests();
  const tabScreen =
    tab === WindowTab.Board
      ? {
          bar: <BoardTabKeyBar extra={tabKeys} />,
          pane: (
            <BoardColumns
              board={app.board.store}
              selection={app.board.selection}
              markerOf={(ticket) => (sessions === undefined ? undefined : markerOfTicket(sessions, ticket))}
              pullRequestOf={(ticket) => {
                const pullRequest = app.review.tickets.of(ticket.branch);
                return pullRequest === undefined ? undefined : cellsOf(pullRequest);
              }}
              onOpen={(ticket) => app.tabs.openTicket(ticket)}
              onRetry={() => void app.load()}
            />
          ),
        }
      : {
          bar: <PullRequestsKeyBar extra={tabKeys} />,
          pane: (
            <PullRequestsPane
              state={pullRequests}
              list={pullRequestList}
              cursor={app.review.cursor.cursorIn(pullRequestList)?.number}
              onOpen={(pullRequest) => app.tabs.openPullRequest(pullRequest)}
              onRetry={() => app.retryPullRequests()}
            />
          ),
        };
  const tabbed =
    tab === WindowTab.Base || unready !== undefined
      ? undefined
      : {
          board: tabScreen.pane,
          docs: null,
          main: null,
          arrangement: Arrangement.Tab,
        };

  const shown =
    reviewing ??
    tabbed ??
    (unready === undefined
      ? {
          board: (
            <BoardPane
              board={app.board.store}
              selection={app.board.selection}
              focused={layout.focus === Focus.Board}
              markerOf={(ticket) => (sessions === undefined ? undefined : markerOfTicket(sessions, ticket))}
              onOpen={() => app.press(Action.Open)}
              onFocus={() => app.focusOn(Focus.Board)}
              onRetry={() => void app.load()}
            />
          ),
          docs: (
            <DocsPane
              docs={app.docs}
              focused={layout.focus === Focus.Docs}
              onFocus={() => app.focusOn(Focus.Docs)}
              onRetry={() => void app.docs.load()}
            />
          ),
          main: layout.showsDocument() ? (
            <DocumentPane docs={app.docs} focused={false} />
          ) : (
            <TicketPane
              board={app.board.store}
              selection={app.board.selection}
              focused={layout.focus === Focus.Sessions}
              onLink={(href) => app.docs.followFromTicket(href)}
            >
              <SessionsList
                rows={app.sessions.rows()}
                cursor={cursor}
                focused={layout.focus === Focus.Sessions}
                picker={picker}
                failure={failure}
                onPress={(index) => app.pressRow(index)}
                onPickVerb={(index) => void app.sessions.pick(index)}
              />
            </TicketPane>
          ),
          arrangement: layout.arrangement(hasDock),
        }
      : unboarded(unready, t));

  const dockScreen =
    tabbed === undefined ? (
      <DockScreen hosting={app.hosting} sessions={sessions} focused={layout.focus === Focus.Dock} onFocus={() => app.focusOn(Focus.Dock)} />
    ) : (
      <DockStrip hosting={app.hosting} sessions={sessions} onSelect={(id) => app.openDockTab(id)} />
    );

  return (
    <>
      <AppFrame
        statusLine={
          <StatusLine
            tabs={
              <WindowTabs
                tabs={[
                  { id: WindowTab.Base, label: t('tabs.base') },
                  {
                    id: WindowTab.Board,
                    label: t('tabs.board'),
                    finding: populatedBoard === undefined ? undefined : <span>{inProgressCount(populatedBoard)}</span>,
                  },
                  {
                    id: WindowTab.PullRequests,
                    label: t('tabs.pullRequests'),
                    finding:
                      pullRequests.kind === 'openPullRequests' ? (
                        <>
                          <span>{pullRequests.pullRequests.length}</span>
                          <CiGlyph ci={ciOf(worstResult(pullRequests.pullRequests))} />
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
        keyBar={picker === undefined ? (tabbed === undefined ? keyBar : tabScreen.bar) : <SessionsKeyBar picking />}
        board={shown.board}
        docs={shown.docs}
        foot={reviewing?.foot}
        main={shown.main}
        dock={dock.tabs.length === 0 ? null : dockScreen}
        arrangement={shown.arrangement}
      />
        {review === undefined ? null : <ReviewDialogs review={review} />}
    </>
  );
};
