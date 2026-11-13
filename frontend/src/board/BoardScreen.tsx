import type { TFunction } from 'i18next';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Busy,
  Button,
  Cards,
  Column,
  Columns,
  Detail,
  type Fact,
  FactsLine,
  type LampColour,
  type PanelTabsProps,
  type PullRequestCells,
  Pane,
  PaneActionKind,
  StatusHeading,
  Text,
  Tone,
  Rows,
  type TicketTypeTag,
  TypeColour,
  TicketCard,
  TicketRow,
  UnreadableRow,
} from '../design-system';
import type { BoardBoardView, BoardTicketView } from '../protocol';
import { useStore } from '../shared/useStore';
import { type BoardState, BoardStateKind, UnavailableCause } from './boardState';
import type { BoardStore } from './boardStore';
import { columnsOf, cursorTicketInColumns, isDone, orderedGroups, shownTickets, selectedIn, selectedInColumns, statusesInOrder } from './rows';
import type { Selection } from './selection';
import { statusColourOf } from './statusColours';
import { PullRequestRead, type TicketStanding } from './ticketStanding';

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
  onBackground: () => void;
  onRetry: () => void;
}

/** The types that carry a colour of their own; the `type:` labels are an open set, and any other is dim. */
const TYPE_COLOUR: ReadonlyMap<string, TypeColour> = new Map([
  ['feature', TypeColour.Feature],
  ['fix', TypeColour.Fix],
  ['chore', TypeColour.Chore],
]);

const typeOf = (ticket: BoardTicketView): TicketTypeTag | undefined =>
  ticket.type.kind === 'typeName' ? { name: ticket.type.value, colour: TYPE_COLOUR.get(ticket.type.value) } : undefined;

/** The Board tab: the Board as status columns of cards. A click on a card or on no card is handed up, which decides what it selects, opens or closes. */
export const BoardColumns = ({ board, selection, markerOf, pullRequestOf, onPress, onBackground, onRetry }: ColumnsProps) => {
  const { t } = useTranslation('board');
  const state = useStore(board.state);
  const chosen = useStore(selection.selected);
  const populated = state.shown();
  const cursor = populated === undefined ? undefined : selectedInColumns(populated, chosen);
  return (
    <Pane title={t('board.title')} focused>
      {populated === undefined ? (
        <Unread state={state} onRetry={onRetry} />
      ) : (
        <>
          <Columns onBackground={onBackground}>
            {columnsOf(populated).map((column) => (
              <Column key={column.status} label={t(`status.${column.status}`)}>
                <StatusHeading status={t(`status.${column.status}`)} count={column.total} colour={statusColourOf(column.status)} />
                <Cards>
                  {column.tickets.map((ticket) => (
                    <TicketCard
                      key={ticket.id}
                      id={ticket.id}
                      title={ticket.title}
                      type={typeOf(ticket)}
                      meta={ticket.otherLabels}
                      selected={ticket.id === cursor}
                      done={isDone(ticket.status)}
                      lamp={markerOf(ticket.id)}
                      pullRequest={pullRequestOf(ticket)}
                      onSelect={() => onPress(ticket, cursor)}
                    />
                  ))}
                </Cards>
              </Column>
            ))}
          </Columns>
          <Unreadable view={populated} />
        </>
      )}
    </Pane>
  );
};

interface PreviewProps extends ViewProps {
  onLink: (href: string) => void;
}

/** The Board tab's preview: the ticket under its cursor, detailed as Base's main view details it. */
export const BoardPreview = ({ board, selection, onLink }: PreviewProps) => {
  const { t } = useTranslation('board');
  const state = useStore(board.state);
  const chosen = useStore(selection.selected);
  const populated = state.shown();
  const ticket = populated === undefined ? undefined : cursorTicketInColumns(populated, chosen);
  return (
    <Pane title={t('ticket.title')} focused={false}>
      {ticket === undefined ? <Text tone={Tone.Dim}>{t('ticket.noneSelected')}</Text> : <TicketDetail ticket={ticket} onLink={onLink} />}
    </Pane>
  );
};

interface TicketProps extends ViewProps {
  focused: boolean;
  /** Whether a pull request is open on the ticket, for its facts line. */
  standing: TicketStanding;
  onLink: (href: string) => void;
  /** Given while a dock is open: the ticket's button hides the main view so the dock takes the column. */
  onHide?: () => void;
  /** Pinned under the ticket whatever its scroll. */
  foot?: ReactNode;
  /** Above the ticket, whether or not one is under the cursor: what failed and can be retried. */
  alert?: ReactNode;
  /** A click on the ticket, anywhere in its pane, hands it focus. */
  onFocus: () => void;
  children?: ReactNode;
}

/** The ticket under the cursor: its title, its facts line, what the screen shows under them, then its text. No ticket under the cursor leaves only that said. */
export const TicketPane = ({ board, selection, focused, standing, onLink, onHide, foot, alert, onFocus, children }: TicketProps) => {
  const { t } = useTranslation('board');
  const { ticket } = useBoardView({ board, selection });
  return (
    <Pane
      title={t('ticket.title')}
      focused={focused}
      foot={foot}
      onFocus={onFocus}
      actions={onHide === undefined ? [] : [{ kind: PaneActionKind.Hide, label: t('ticket.hide'), shortcut: t('ticket.hideKey'), onClick: onHide }]}
    >
      {alert}
      {ticket === undefined ? (
        <Text tone={Tone.Dim}>{t('ticket.noneSelected')}</Text>
      ) : (
        <Detail
          facts={[ticket.id]}
          title={ticket.title}
          summary={<FactsLine facts={factsOf(ticket, standing, t)} />}
          lead={children}
          body={ticket.body.kind === 'body' ? ticket.body.value : undefined}
          onLink={onLink}
        />
      )}
    </Pane>
  );
};

const PULL_REQUEST_WORDS = {
  [PullRequestRead.Reading]: 'facts.readingPullRequest',
  [PullRequestRead.Unavailable]: 'facts.pullRequestsUnavailable',
  [PullRequestRead.None]: 'facts.noPullRequest',
} as const;

const pullRequestFact = (standing: TicketStanding, t: TFunction<'board'>): Fact => {
  const label = t('facts.pullRequest');
  const { read, number } = standing.pullRequest;
  return read === PullRequestRead.Open ? { label, value: t('facts.pullRequestNumber', { number }) } : { label, value: t(PULL_REQUEST_WORDS[read]), tone: Tone.Dim };
};

/** Where the ticket stands, in the order it is read: its status, type, context, other labels and open pull request. What the ticket lacks is left out, except the pull request, which says there is none. Its branch is not shown: long text wraps the line. */
const factsOf = (ticket: BoardTicketView, standing: TicketStanding, t: TFunction<'board'>): Fact[] => {
  return [
    { label: t('facts.status'), value: t(`status.${ticket.status}`) },
    ...(ticket.type.kind === 'typeName' ? [{ label: t('facts.type'), value: ticket.type.value }] : []),
    ...(ticket.context.kind === 'contextName' ? [{ label: t('facts.context'), value: ticket.context.value }] : []),
    ...(ticket.otherLabels.length === 0 ? [] : [{ label: t('facts.labels'), value: ticket.otherLabels.join(', ') }]),
    pullRequestFact(standing, t),
  ];
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
      return <Text tone={Tone.Dim}>{t('board.empty')}</Text>;
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
      <Unreadable view={view} />
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

const Unreadable = ({ view }: { view: BoardBoardView }) => {
  const { t } = useTranslation('board');
  return view.unreadable.length === 0 ? null : (
    <div>
      <StatusHeading status={t('board.unreadable')} count={view.unreadable.length} />
      <Rows>
        {view.unreadable.map((ticket) => (
          <UnreadableRow key={ticket.id} id={ticket.id} reason={ticket.reason} />
        ))}
      </Rows>
    </div>
  );
};

const TicketDetail = ({ ticket, onLink }: { ticket: BoardTicketView; onLink: (href: string) => void }) => {
  const { t } = useTranslation('board');
  const facts = [
    ticket.id,
    t(`status.${ticket.status}`),
    ticket.context.kind === 'contextName' ? ticket.context.value : undefined,
    ticket.type.kind === 'typeName' ? ticket.type.value : undefined,
  ].filter((fact): fact is string => fact !== undefined);
  return <Detail facts={facts} title={ticket.title} body={ticket.body.kind === 'body' ? ticket.body.value : undefined} onLink={onLink} />;
};
