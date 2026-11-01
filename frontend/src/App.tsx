import { useEffect } from 'react';
import type { App as AppStores } from './app';
import { BoardScreen } from './board/BoardScreen';
import { selectedIn } from './board/rows';
import { ConnectionScreen } from './connection/ConnectionScreen';
import { Alert, AppFrame, Button, Count, KeyBar, Lamp, Pane, StatusLine, Text } from './design-system';
import { installKeys } from './keys/installKeys';
import { markerOfTicket, markersOf } from './session/markers';
import { useStore } from './shared/useStore';

const BOARD_KEYS = ['j/k move', 'enter open'];
const COLOURS = ['permission', 'input', 'turn'] as const;

interface Props {
  app: AppStores;
  packaged: boolean;
  onOpen: (ticket: string) => void;
}

/** The window: the status line of lamps over the Board, or over what stands in for it while the backend is not there. */
export function App({ app, packaged, onOpen }: Props) {
  const status = useStore(app.client.status);
  const sessions = useStore(app.sessions.sessions);
  const board = useStore(app.board.state);
  const configuration = useStore(app.configuration);

  useEffect(
    () =>
      installKeys(
        (action) => {
          const state = app.board.state.get();
          if (state.kind !== 'populated') {
            return;
          }
          if (action === 'moveDown' || action === 'moveUp') {
            app.selection.move(state.view, action === 'moveDown' ? 1 : -1);
          } else if (action === 'open') {
            const cursor = selectedIn(state.view, app.selection.selected.get());
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
  const project = board.kind === 'populated' ? board.view.root : 'lazyclaude';

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
      keyBar={<KeyBar keys={BOARD_KEYS} />}
    >
      {status.kind === 'ready' && configuration.kind === 'fault' ? (
        <>
          <Pane title="Board" focused>
            <Alert title="The configuration could not be read" action={<Button onClick={() => void app.load()}>Retry</Button>}>
              {configuration.message}
            </Alert>
          </Pane>
          <Pane title="Ticket" focused={false}>
            <Text tone="dim">No ticket selected.</Text>
          </Pane>
        </>
      ) : status.kind === 'ready' ? (
        <BoardScreen
          board={app.board}
          selection={app.selection}
          markerOf={(ticket) => (sessions === undefined ? undefined : markerOfTicket(sessions, ticket))}
          onOpen={onOpen}
          onRetry={() => void app.load()}
        />
      ) : (
        <>
          <Pane title="Board" focused>
            <ConnectionScreen status={status} onRestart={() => void app.client.restart()} />
          </Pane>
          <Pane title="Ticket" focused={false}>
            <Text tone="dim">No ticket selected.</Text>
          </Pane>
        </>
      )}
    </AppFrame>
  );
}
