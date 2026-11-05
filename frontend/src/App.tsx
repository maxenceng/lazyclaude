import { type ReactNode, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import type { AppModel } from './app';
import { BoardKeyBar } from './board/BoardKeyBar';
import { BoardPane, TicketPane } from './board/BoardScreen';
import { ConnectionScreen } from './connection/ConnectionScreen';
import { Alert, AppFrame, Button, Count, Lamp, Pane, StatusLine, Text } from './design-system';
import { ConfigurationKind } from './keys/configurationState';
import { Action } from './keys/bindings';
import { installKeys } from './keys/installKeys';
import { Focus } from './layout';
import { StatusKind } from './protocol';
import { DockScreen } from './session/DockScreen';
import { LampColour, markerOfTicket, markersOf } from './session/markers';
import { DockKeyBar, SessionsKeyBar } from './session/SessionsKeyBar';
import { SessionsList } from './session/SessionsList';
import { useStore } from './shared/useStore';

const COLOURS = [LampColour.Permission, LampColour.Input, LampColour.Turn] as const;

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

  useEffect(() => installKeys((action) => app.press(action), { packaged, reserved: () => app.reserved() }), [app, packaged]);

  const markers = sessions === undefined ? [] : markersOf(sessions);
  const project = board.shown()?.root ?? t('app.project');
  const hasDock = dock.tabs.length > 0;
  const keyBar = {
    [Focus.Board]: <BoardKeyBar />,
    [Focus.Sessions]: <SessionsKeyBar picking={false} />,
    [Focus.Dock]: <DockKeyBar mouseReported={mouseReported} />,
  }[layout.focus];

  const unboarded = (alternative: ReactNode) => ({
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
    arrangement: 'noDock' as const,
  });

  const shown = (() => {
    if (status.kind !== StatusKind.Ready) {
      return unboarded(<ConnectionScreen status={status} onRestart={() => void app.client.restart()} />);
    }
    if (configuration.kind === ConfigurationKind.Fault) {
      return unboarded(
        <Alert title={t('configuration.fault')} action={<Button onClick={() => void app.load()}>{t('configuration.retry')}</Button>}>
          {configuration.message}
        </Alert>,
      );
    }
    return {
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
      main: (
        <TicketPane board={app.board.store} selection={app.board.selection} focused={layout.focus === Focus.Sessions}>
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
    };
  })();

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
      main={shown.main}
      dock={
        hasDock ? <DockScreen hosting={app.hosting} sessions={sessions} focused={layout.focus === Focus.Dock} onFocus={() => app.focusOn(Focus.Dock)} /> : null
      }
      arrangement={shown.arrangement}
    />
  );
};
