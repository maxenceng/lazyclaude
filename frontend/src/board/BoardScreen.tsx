import { useTranslation } from 'react-i18next';
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
import { type BoardState, BoardStateKind, UnavailableCause } from './boardState';
import type { BoardStore } from './boardStore';
import { isFolded, orderedGroups, selectedIn } from './rows';
import type { Selection } from './selection';

interface Props {
  board: BoardStore;
  selection: Selection;
  markerOf: (ticket: string) => LampColour | undefined;
  onOpen: (ticket: string) => void;
  onRetry: () => void;
}

/** The Board: the tickets by status with the one under the cursor detailed beside them. */
export const BoardScreen = ({ board, selection, markerOf, onOpen, onRetry }: Props) => {
  const { t } = useTranslation('board');
  const state = useStore(board.state);
  const chosen = useStore(selection.selected);
  const populated = state.shown();
  const cursor = populated === undefined ? undefined : selectedIn(populated, chosen);
  const ticket = populated?.groups.flatMap((group) => group.tickets).find((candidate) => candidate.id === cursor);

  const press = (id: string) => (id === cursor ? onOpen(id) : selection.select(id));

  return (
    <>
      <Pane title={t('board.title')} focused>
        {populated === undefined ? (
          <Unread state={state} onRetry={onRetry} />
        ) : (
          <Tickets view={populated} cursor={cursor} markerOf={markerOf} onPress={press} />
        )}
      </Pane>
      <Pane title={t('ticket.title')} focused={false}>
        {ticket === undefined ? <Text tone="dim">{t('ticket.noneSelected')}</Text> : <TicketDetail ticket={ticket} />}
      </Pane>
    </>
  );
};

const UNAVAILABLE = {
  [UnavailableCause.NoProject]: 'board.noProject',
  [UnavailableCause.UnavailableBoard]: 'board.unavailableBoard',
} as const satisfies Record<UnavailableCause, string>;

const Unread = ({ state, onRetry }: { state: BoardState; onRetry: () => void }) => {
  const { t } = useTranslation('board');
  switch (state.kind) {
    case BoardStateKind.Loading:
      return <Busy>{t('board.loading')}</Busy>;
    case BoardStateKind.Empty:
      return <Text tone="dim">{t('board.empty')}</Text>;
    case BoardStateKind.Unavailable:
      return (
        <>
          <Text>{t(UNAVAILABLE[state.cause])}</Text>
          <Text tone="dim">{state.reason}</Text>
        </>
      );
    case BoardStateKind.Fault:
      return (
        <Alert title={t('board.fault')} action={<Button onClick={onRetry}>{t('board.retry')}</Button>}>
          {state.message}
        </Alert>
      );
    case BoardStateKind.Populated:
      return null;
  }
};

interface TicketsProps {
  view: BoardView;
  cursor: string | undefined;
  markerOf: (ticket: string) => LampColour | undefined;
  onPress: (ticket: string) => void;
}

const Tickets = ({ view, cursor, markerOf, onPress }: TicketsProps) => {
  const { t } = useTranslation('board');
  return (
    <>
      {orderedGroups(view).map((group) => (
        <div key={group.status}>
          <StatusHeading status={t(`status.${group.status}`)} count={group.tickets.length} />
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
          <StatusHeading status={t('board.unreadable')} count={view.unreadable.length} />
          <TicketList>
            {view.unreadable.map((ticket) => (
              <UnreadableRow key={ticket.id} id={ticket.id} reason={ticket.reason} />
            ))}
          </TicketList>
        </div>
      )}
    </>
  );
};

const TicketDetail = ({ ticket }: { ticket: TicketView }) => {
  const { t } = useTranslation('board');
  const facts = [
    ticket.id,
    t(`status.${ticket.status}`),
    ticket.context.kind === 'contextName' ? ticket.context.value : undefined,
    ticket.type.kind === 'typeName' ? ticket.type.value : undefined,
  ].filter((fact): fact is string => fact !== undefined);
  return <Detail facts={facts} title={ticket.title} body={ticket.body.kind === 'body' ? ticket.body.value : undefined} />;
};
