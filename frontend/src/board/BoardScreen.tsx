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
  type LampColour,
  type PullRequestCells,
  Pane,
  PaneActionKind,
  StatusHeading,
  Text,
  Tone,
  Rows,
  TicketCard,
  TicketRow,
  UnreadableRow,
} from '../design-system';
import type { BoardBoardView, BoardTicketView } from '../protocol';
import { useStore } from '../shared/useStore';
import { type BoardState, BoardStateKind, UnavailableCause } from './boardState';
import type { BoardStore } from './boardStore';
import { columnsOf, cursorTicketInColumns, goesToBase, isFolded, orderedGroups, selectedIn, selectedInColumns } from './rows';
import type { Selection } from './selection';

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
}

/** The Board: the tickets by status. A press on the selected ticket opens it. */
export const BoardPane = ({ board, selection, focused, markerOf, onOpen, onFocus, onRetry }: BoardProps) => {
  const { t } = useTranslation('board');
  const { state, populated, cursor } = useBoardView({ board, selection });
  return (
    <Pane title={t('board.title')} focused={focused}>
      {populated === undefined ? (
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

/** The words a card's footer names after its number: its type, then its other labels. */
const metaOf = (ticket: BoardTicketView): string[] => [
  ...(ticket.type.kind === 'typeName' ? [ticket.type.value] : []),
  ...ticket.otherLabels,
];

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
                <StatusHeading status={t(`status.${column.status}`)} count={column.total} />
                <Cards>
                  {column.tickets.map((ticket) => (
                    <TicketCard
                      key={ticket.id}
                      id={ticket.id}
                      title={ticket.title}
                      meta={metaOf(ticket)}
                      selected={ticket.id === cursor}
                      done={!goesToBase(ticket)}
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
  onLink: (href: string) => void;
  /** Given while a dock is open: the ticket's button hides the main view so the dock takes the column. */
  onHide?: () => void;
  children?: ReactNode;
}

/** The ticket under the cursor, detailed, and whatever the screen shows beneath it. */
export const TicketPane = ({ board, selection, focused, onLink, onHide, children }: TicketProps) => {
  const { t } = useTranslation('board');
  const { ticket } = useBoardView({ board, selection });
  return (
    <Pane
      title={t('ticket.title')}
      focused={focused}
      action={onHide === undefined ? undefined : { kind: PaneActionKind.Close, label: t('ticket.hide'), onClick: onHide }}
    >
      {ticket === undefined ? <Text tone={Tone.Dim}>{t('ticket.noneSelected')}</Text> : <TicketDetail ticket={ticket} onLink={onLink} />}
      {children}
    </Pane>
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
          <StatusHeading status={t(`status.${group.status}`)} count={group.tickets.length} />
          <Rows>
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
          </Rows>
        </div>
      ))}
      <Unreadable view={view} />
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
