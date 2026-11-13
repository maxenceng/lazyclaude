import type { TFunction } from 'i18next';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Busy,
  Button,
  Column,
  Columns,
  Detail,
  type Fact,
  FactsLine,
  type LampColour,
  type PaneChip,
  type PanelTabsProps,
  type PullRequestCells,
  Pane,
  PaneActionKind,
  PaneLayout,
  StatusHeading,
  Text,
  Tone,
  Rows,
  type TicketTypeTag,
  TypeColour,
  TicketCard,
  TicketRow,
} from '../design-system';
import type { BoardBoardView, BoardTicketView } from '../protocol';
import { useStore } from '../shared/useStore';
import { type BoardState, BoardStateKind, type EmptyBoard, type PopulatedBoard, type UnavailableBoard, UnavailableCause } from './boardState';
import type { BoardStore } from './boardStore';
import { columnsOf, isDone, orderedGroups, shownTickets, selectedIn, statusesInOrder } from './rows';
import type { Selection } from './selection';
import { statusColourOf } from './statusColours';
import { PullRequestRead, type TicketPullRequest } from './ticketPullRequest';

interface ViewProps {
  board: BoardStore;
  selection: Selection;
}

const useBoardView = ({ board, selection }: ViewProps) => {
  const state = useStore(board.state);
  const chosen = useStore(selection.selected);
  const populated = state.shown();
  const cursor = populated === undefined ? undefined : selectedIn(populated, chosen);
  const ticket = populated?.groups.flatMap((group) => group.tickets).find((candidate) => candidate.id === cursor);
  return { state, populated, cursor, ticket };
};

interface BoardProps extends ViewProps {
  focused: boolean;
  markerOf: (ticket: string) => LampColour | undefined;
  onOpen: (ticket: string) => void;
  onFocus: () => void;
  onRetry: () => void;
  /** The left column's panel tabs, which the Board's title row shows in place of its title. */
  panelTabs: PanelTabsProps;
  /** Given while there is a project to capture into: the title's button opens the capture popup, as `c` does. */
  onCapture?: () => void;
}

/** The Board: the tickets by status, every status at 0 while none is on it. A press on the selected ticket opens it. */
export const BoardPane = ({ board, selection, focused, markerOf, onOpen, onFocus, onRetry, panelTabs, onCapture }: BoardProps) => {
  const { t } = useTranslation('board');
  const { state, populated, cursor } = useBoardView({ board, selection });
  return (
    <Pane
      title={t('board.title')}
      focused={focused}
      tabs={panelTabs}
      actions={onCapture === undefined ? [] : [{ kind: PaneActionKind.Add, label: t('board.capture'), shortcut: t('board.captureKey'), onClick: onCapture }]}
    >
      {state.kind === BoardStateKind.Empty ? (
        <EmptyGroups />
      ) : populated === undefined ? (
        <Unread state={state} onRetry={onRetry} />
      ) : (
        <Tickets
          view={populated}
          cursor={cursor}
          markerOf={markerOf}
          onPress={(id) => {
            onFocus();
            return id === cursor ? onOpen(id) : selection.select(id);
          }}
        />
      )}
    </Pane>
  );
};

interface ColumnsProps extends ViewProps {
  markerOf: (ticket: string) => LampColour | undefined;
  pullRequestOf: (ticket: BoardTicketView) => PullRequestCells | undefined;
  onPress: (ticket: BoardTicketView, cursor: string | undefined) => void;
  onRetry: () => void;
  /** Given while there is a project to capture into: the title's button opens the capture popup, as `c` does. */
  onCapture?: () => void;
}

/** The types that carry a colour of their own; the `type:` labels are an open set, and any other is dim. */
const TYPE_COLOUR: ReadonlyMap<string, TypeColour> = new Map([
  ['feature', TypeColour.Feature],
  ['fix', TypeColour.Fix],
  ['chore', TypeColour.Chore],
]);

const typeOf = (ticket: BoardTicketView): TicketTypeTag | undefined =>
  ticket.type.kind === 'typeName' ? { name: ticket.type.value, colour: TYPE_COLOUR.get(ticket.type.value) } : undefined;

/** Whether the Board tab draws its columns: once the board is read, or known unavailable, the reason then standing above them (ADR 0105); not while it is read, nor after a fault, which offers a retry instead. */
const showsColumns = (state: BoardState): state is PopulatedBoard | EmptyBoard | UnavailableBoard => state.kind === BoardStateKind.Populated || state.kind === BoardStateKind.Empty || state.kind === BoardStateKind.Unavailable;

/** The Board tab: the Board as five equal status columns of cards, each at 0 and `none` while it holds nothing. A click on a card is handed up, which decides what it selects or opens. */
export const BoardColumns = ({ board, selection, markerOf, pullRequestOf, onPress, onRetry, onCapture }: ColumnsProps) => {
  const { t } = useTranslation('board');
  const state = useStore(board.state);
  const chosen = useStore(selection.selected);
  const populated = state.shown();
  const cursor = populated === undefined ? undefined : selectedIn(populated, chosen);
  const columns = populated === undefined ? statusesInOrder().map((status) => ({ status, tickets: [], total: 0 })) : columnsOf(populated);
  return (
    <Pane
      title={t('board.title')}
      focused
      layout={PaneLayout.Fill}
      actions={onCapture === undefined ? [] : [{ kind: PaneActionKind.Add, label: t('board.capture'), shortcut: t('board.captureKey'), onClick: onCapture }]}
    >
      {showsColumns(state) ? (
        <>
          {state.kind === BoardStateKind.Unavailable ? <Unread state={state} onRetry={onRetry} /> : null}
          <Columns>
            {columns.map((column) => (
              <Column
                key={column.status}
                label={t(`status.${column.status}`)}
                heading={<StatusHeading status={t(`status.${column.status}`)} count={column.total} colour={statusColourOf(column.status)} />}
                empty={t('board.none')}
              >
                {column.tickets.map((ticket) => (
                  <TicketCard
                    key={ticket.id}
                    id={ticket.id}
                    title={ticket.title}
                    type={typeOf(ticket)}
                    selected={ticket.id === cursor}
                    lamp={markerOf(ticket.id)}
                    pullRequest={pullRequestOf(ticket)}
                    onSelect={() => onPress(ticket, cursor)}
                  />
                ))}
              </Column>
            ))}
          </Columns>
        </>
      ) : (
        <Unread state={state} onRetry={onRetry} />
      )}
    </Pane>
  );
};

interface TicketProps extends ViewProps {
  focused: boolean;
  /** Whether a pull request is open on the ticket, for its facts line. */
  pullRequest: TicketPullRequest;
  onLink: (href: string) => void;
  /** Given while a dock is open: the ticket's button hides the main view so the dock takes the column. */
  onHide?: () => void;
  /** Pinned under the ticket whatever its scroll. */
  foot?: ReactNode;
  /** Above the ticket, whether or not one is under the cursor: what failed and can be retried. */
  alert?: ReactNode;
  /** A click on the ticket, anywhere in its pane, hands it focus; a preview has none to give. */
  onFocus?: () => void;
  children?: ReactNode;
}

/** The ticket under the cursor: its title, its facts line, what the screen shows under them, then its text. No ticket under the cursor leaves only that said. */
export const TicketPane = ({ board, selection, focused, pullRequest, onLink, onHide, foot, alert, onFocus, children }: TicketProps) => {
  const { t } = useTranslation('board');
  const { ticket } = useBoardView({ board, selection });
  return (
    <Pane
      title={t('ticket.title')}
      focused={focused}
      foot={foot}
      chips={ticket === undefined ? [] : chipsOf(ticket, t)}
      onFocus={onFocus}
      actions={onHide === undefined ? [] : [{ kind: PaneActionKind.Hide, label: t('ticket.hide'), shortcut: t('ticket.hideKey'), onClick: onHide }]}
    >
      {alert}
      {ticket === undefined ? (
        <Text tone={Tone.Dim}>{t('ticket.noneSelected')}</Text>
      ) : (
        <Detail
          facts={[t('ticket.number', { id: ticket.id })]}
          title={ticket.title}
          summary={<FactsLine facts={factsOf(ticket, pullRequest, t)} />}
          lead={children}
          body={ticket.body.kind === 'body' ? ticket.body.value : undefined}
          onLink={onLink}
        />
      )}
    </Pane>
  );
};

/** The words beside the ticket's title: its status in its colour, then its context and type where it has them. */
const chipsOf = (ticket: BoardTicketView, t: TFunction<'board'>): PaneChip[] => [
  { label: t(`status.${ticket.status}`), colour: statusColourOf(ticket.status) },
  ...(ticket.context.kind === 'contextName' ? [{ label: ticket.context.value }] : []),
  ...(ticket.type.kind === 'typeName' ? [{ label: ticket.type.value }] : []),
];

const PULL_REQUEST_WORDS = {
  [PullRequestRead.Reading]: 'facts.readingPullRequest',
  [PullRequestRead.Unavailable]: 'facts.pullRequestsUnavailable',
  [PullRequestRead.None]: 'facts.noPullRequest',
} as const;

const pullRequestFact = ({ read, number }: TicketPullRequest, t: TFunction<'board'>): Fact => {
  const label = t('facts.pullRequest');
  return read === PullRequestRead.Open ? { label, value: t('facts.pullRequestNumber', { number }) } : { label, value: t(PULL_REQUEST_WORDS[read]), tone: Tone.Dim };
};

/** Where the ticket stands, in the order it is read: its status, type, context, other labels and open pull request. What the ticket lacks is left out, except the pull request, which says there is none. Its branch is not shown: long text wraps the line. */
const factsOf = (ticket: BoardTicketView, pullRequest: TicketPullRequest, t: TFunction<'board'>): Fact[] => {
  return [
    { label: t('facts.status'), value: t(`status.${ticket.status}`) },
    ...(ticket.type.kind === 'typeName' ? [{ label: t('facts.type'), value: ticket.type.value }] : []),
    ...(ticket.context.kind === 'contextName' ? [{ label: t('facts.context'), value: ticket.context.value }] : []),
    ...(ticket.otherLabels.length === 0 ? [] : [{ label: t('facts.labels'), value: ticket.otherLabels.join(', ') }]),
    pullRequestFact(pullRequest, t),
  ];
};

const UNAVAILABLE = {
  [UnavailableCause.NoProject]: 'board.noProject',
  [UnavailableCause.UnavailableBoard]: 'board.unavailableBoard',
} as const satisfies Record<UnavailableCause, string>;

const Unread = ({ state, onRetry }: { state: Exclude<BoardState, EmptyBoard>; onRetry: () => void }) => {
  const { t } = useTranslation('board');
  switch (state.kind) {
    case BoardStateKind.Loading:
      return <Busy>{t('board.loading')}</Busy>;
    case BoardStateKind.Unavailable:
      return (
        <>
          <Text>{t(UNAVAILABLE[state.cause])}</Text>
          <Text tone={Tone.Dim}>{state.reason}</Text>
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
  view: BoardBoardView;
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
          <StatusHeading status={t(`status.${group.status}`)} count={group.tickets.length} colour={statusColourOf(group.status)} />
          <Rows>
            {shownTickets(group).map((ticket) => (
              <TicketRow
                key={ticket.id}
                id={ticket.id}
                title={ticket.title}
                selected={ticket.id === cursor}
                done={isDone(group.status)}
                lamp={markerOf(ticket.id)}
                type={typeOf(ticket)}
                onSelect={() => onPress(ticket.id)}
              />
            ))}
          </Rows>
        </div>
      ))}
    </>
  );
};

/** The board with no ticket: every status keeps its heading, at 0. */
const EmptyGroups = () => {
  const { t } = useTranslation('board');
  return (
    <>
      {statusesInOrder().map((status) => (
        <StatusHeading key={status} status={t(`status.${status}`)} count={0} colour={statusColourOf(status)} />
      ))}
    </>
  );
};
