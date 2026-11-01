import {
  Alert,
  Busy,
  Button,
  Detail,
  type LampColour,
  Pane,
  StatusHeading,
  Text,
  TicketList,
  TicketRow,
  UnreadableRow,
} from '../design-system';
import type { BoardView, TicketView } from '../protocol';
import { useStore } from '../shared/useStore';
import type { BoardState, BoardStore } from './boardStore';
import { isFolded, orderedGroups, selectedIn, statusLabel } from './rows';
import type { Selection } from './selection';

interface Props {
  board: BoardStore;
  selection: Selection;
  markerOf: (ticket: string) => LampColour | undefined;
  onOpen: (ticket: string) => void;
  onRetry: () => void;
}

/** The Board: the tickets by status with the one under the cursor detailed beside them. */
export function BoardScreen({ board, selection, markerOf, onOpen, onRetry }: Props) {
  const state = useStore(board.state);
  const chosen = useStore(selection.selected);
  const populated = state.kind === 'populated' ? state.view : undefined;
  const cursor = populated === undefined ? undefined : selectedIn(populated, chosen);
  const ticket = populated?.groups.flatMap((group) => group.tickets).find((candidate) => candidate.id === cursor);

  const press = (id: string) => (id === cursor ? onOpen(id) : selection.select(id));

  return (
    <>
      <Pane title="Board" focused>
        {populated === undefined ? (
          <Unread state={state} onRetry={onRetry} />
        ) : (
          <Tickets view={populated} cursor={cursor} markerOf={markerOf} onPress={press} />
        )}
      </Pane>
      <Pane title="Ticket" focused={false}>
        {ticket === undefined ? <Text tone="dim">No ticket selected.</Text> : <TicketDetail ticket={ticket} />}
      </Pane>
    </>
  );
}

function Unread({ state, onRetry }: { state: BoardState; onRetry: () => void }) {
  switch (state.kind) {
    case 'loading':
      return <Busy>Reading the board…</Busy>;
    case 'empty':
      return <Text tone="dim">No tickets on the board.</Text>;
    case 'unavailable':
      return (
        <>
          <Text>{state.cause === 'noProject' ? 'This directory is not a project' : 'The board cannot be read'}</Text>
          <Text tone="dim">{state.reason}</Text>
        </>
      );
    case 'fault':
      return (
        <Alert title="The board could not be read" action={<Button onClick={onRetry}>Retry</Button>}>
          {state.message}
        </Alert>
      );
    case 'populated':
      return null;
  }
}

interface TicketsProps {
  view: BoardView;
  cursor: string | undefined;
  markerOf: (ticket: string) => LampColour | undefined;
  onPress: (ticket: string) => void;
}

function Tickets({ view, cursor, markerOf, onPress }: TicketsProps) {
  return (
    <>
      {orderedGroups(view).map((group) => (
        <div key={group.status}>
          <StatusHeading status={statusLabel(group.status)} count={group.tickets.length} />
          <TicketList>
            {(isFolded(group.status) ? [] : group.tickets).map((ticket) => (
              <TicketRow
                key={ticket.id}
                id={ticket.id}
                title={ticket.title}
                selected={ticket.id === cursor}
                done={group.status === 'DONE'}
                lamp={markerOf(ticket.id)}
                onSelect={() => onPress(ticket.id)}
              />
            ))}
          </TicketList>
        </div>
      ))}
      {view.unreadable.length === 0 ? null : (
        <div>
          <StatusHeading status="unreadable" count={view.unreadable.length} />
          <TicketList>
            {view.unreadable.map((ticket) => (
              <UnreadableRow key={ticket.id} id={ticket.id} reason={ticket.reason} />
            ))}
          </TicketList>
        </div>
      )}
    </>
  );
}

function TicketDetail({ ticket }: { ticket: TicketView }) {
  const facts = [
    ticket.id,
    statusLabel(ticket.status),
    ticket.context.kind === 'contextName' ? ticket.context.value : undefined,
    ticket.type.kind === 'typeName' ? ticket.type.value : undefined,
  ].filter((fact): fact is string => fact !== undefined);
  return <Detail facts={facts} title={ticket.title} body={ticket.body.kind === 'body' ? ticket.body.value : undefined} />;
}

