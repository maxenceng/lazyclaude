import type { TFunction } from 'i18next';
import { type ReactNode, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import type { AppModel } from './app';
import { BoardKeyBar } from './board/BoardKeyBar';
import { BoardPane, TicketPane } from './board/BoardScreen';
import { ConnectionScreen } from './connection/ConnectionScreen';
import { DocsKeyBar } from './docs/DocsKeyBar';
import { DocsPane, DocumentPane } from './docs/DocsScreen';
import { Alert, AppFrame, Button, Count, Lamp, Pane, StatusLine, Text } from './design-system';
import { ConfigurationKind } from './keys/configurationState';
import { Action } from './keys/bindings';
import { WindowKeys } from './keys/windowKeys';
import { Focus } from './layout';
import { StatusKind } from './protocol';
import { DockScreen } from './session/DockScreen';
import { LampColour, markerOfTicket, markersOf } from './session/markers';
import { DockKeyBar, SessionsKeyBar } from './session/SessionsKeyBar';
import { SessionsList } from './session/SessionsList';
import { useStore } from './shared/useStore';

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
      <Text tone="dim">{t('app.noTicketSelected')}</Text>
    </Pane>
  ),
  docs: null,
  arrangement: 'noDock' as const,
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
  const mouseReported = useStore(app.hosting.mouseReported);
  useStore(app.board.selection.selected);

  useEffect(() => {
    const keys = new WindowKeys((action) => app.press(action), { packaged, reserved: () => app.reserved() });
    return () => keys.remove();
  }, [app, packaged]);

  const markers = sessions === undefined ? [] : markersOf(sessions);
  const project = board.shown()?.root ?? t('app.project');
  const hasDock = dock.tabs.length > 0;
  const keyBar = {
    [Focus.Board]: <BoardKeyBar />,
    [Focus.Docs]: <DocsKeyBar />,
    [Focus.Sessions]: <SessionsKeyBar picking={false} />,
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

  const shown =
    unready === undefined
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
      : unboarded(unready, t);

  return (
    <AppFrame
      statusLine={
        <StatusLine
          tiles={markers.map((marker) => (
            <Lamp key={marker.session} colour={marker.colour} size="tile" number={marker.ticket} />
          ))}
          counts={COLOURS.map((colour) => (
            <Count key={colour} colour={colour} count={markers.filter((marker) => marker.colour === colour).length} />
          ))}
          project={project}
          warning={warning}
        />
      }
      keyBar={picker === undefined ? keyBar : <SessionsKeyBar picking />}
      board={shown.board}
      docs={shown.docs}
      main={shown.main}
      dock={
        hasDock ? <DockScreen hosting={app.hosting} sessions={sessions} focused={layout.focus === Focus.Dock} onFocus={() => app.focusOn(Focus.Dock)} /> : null
      }
      arrangement={shown.arrangement}
    />
  );
};
