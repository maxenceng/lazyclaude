import { type ReactNode, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import type { App as AppStores } from './app';
import { BoardScreen } from './board/BoardScreen';
import { selectedIn } from './board/rows';
import { ConnectionScreen } from './connection/ConnectionScreen';
import { Alert, AppFrame, Button, Count, KeyBar, Lamp, Pane, StatusLine, Text } from './design-system';
import { Action } from './keys/bindings';
import { ConfigurationKind } from './keys/configurationState';
import { installKeys } from './keys/installKeys';
import { StatusKind } from './protocol';
import { LampColour, markerOfTicket, markersOf } from './session/markers';
import { useStore } from './shared/useStore';

const COLOURS = [LampColour.Permission, LampColour.Input, LampColour.Turn] as const;

interface Props {
  app: AppStores;
  packaged: boolean;
  onOpen: (ticket: string) => void;
}

/** The window: the status line of lamps over the Board, or over what stands in for it while the backend is not there. */
export const App = ({ app, packaged, onOpen }: Props) => {
  const { t } = useTranslation();
  const status = useStore(app.client.status);
  const sessions = useStore(app.sessions.sessions);
  const board = useStore(app.board.state);
  const configuration = useStore(app.configuration);

  useEffect(
    () =>
      installKeys(
        (action) => {
          const view = app.board.state.get().shown();
          if (view === undefined) {
            return;
          }
          if (action === Action.MoveDown || action === Action.MoveUp) {
            app.selection.move(view, action === Action.MoveDown ? 1 : -1);
          } else if (action === Action.Open) {
            const cursor = selectedIn(view, app.selection.selected.get());
            if (cursor !== undefined) {
              onOpen(cursor);
            }
          }
        },
        { packaged, reserved: app.reserved },
      ),
    [app, packaged, onOpen],
  );

  const markers = sessions === undefined ? [] : markersOf(sessions);
  const project = board.shown()?.root ?? t('app.project');
  const unboarded = (alternative: ReactNode) => (
    <>
      <Pane title={t('app.boardTitle')} focused>
        {alternative}
      </Pane>
      <Pane title={t('app.ticketTitle')} focused={false}>
        <Text tone="dim">{t('app.noTicketSelected')}</Text>
      </Pane>
    </>
  );

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
        />
      }
      keyBar={<KeyBar keys={[t('keyBar.move'), t('keyBar.open')]} />}
    >
      {status.kind !== StatusKind.Ready ? (
        unboarded(<ConnectionScreen status={status} onRestart={() => void app.client.restart()} />)
      ) : configuration.kind === ConfigurationKind.Fault ? (
        unboarded(
          <Alert title={t('configuration.fault')} action={<Button onClick={() => void app.load()}>{t('configuration.retry')}</Button>}>
            {configuration.message}
          </Alert>,
        )
      ) : (
        <BoardScreen
          board={app.board}
          selection={app.selection}
          markerOf={(ticket) => (sessions === undefined ? undefined : markerOfTicket(sessions, ticket))}
          onOpen={onOpen}
          onRetry={() => void app.load()}
        />
      )}
    </AppFrame>
  );
};
