import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Alert, Button, PickerBar, SessionRow as Row, StatusHeading, Rows } from '../design-system';
import type { SessionPastSessionView } from '../protocol';
import { type RunningState, type SessionRow, SessionRowKind } from './sessionRows';
import { ageOf } from './age';
import type { StartFailure } from './hosting';
import type { VerbPicker } from './verbPicker';

interface ListProps {
  rows: readonly SessionRow[];
  cursor: number;
  picker: VerbPicker | undefined;
  onPress: (index: number) => void;
}

const titleOf = (session: SessionPastSessionView): string | undefined => {
  if (session.title.kind === 'aiTitle') {
    return session.title.value;
  }
  return session.opening.kind === 'firstPrompt' ? session.opening.value : undefined;
};

const labelled = (row: SessionRow, t: TFunction<'session'>): { label: string } => {
  switch (row.kind) {
    case SessionRowKind.Hosted:
      return { label: row.name ?? t('row.starting') };
    case SessionRowKind.Live:
      return { label: row.name };
    case SessionRowKind.Past:
      return { label: titleOf(row.session) ?? t('row.untitled', { id: row.session.id }) };
    case SessionRowKind.New:
      return { label: t('row.new') };
  }
};

/** What Enter does on a row: a hosted session is attached to, a past one resumed; a live session of another window can be seen and not entered. */
const actionOf = (row: SessionRow, t: TFunction<'session'>): { label: string; shortcut: string } | undefined => {
  switch (row.kind) {
    case SessionRowKind.Hosted:
      return { label: t('row.attach'), shortcut: t('row.enterKey') };
    case SessionRowKind.Past:
      return { label: t('row.resume'), shortcut: t('row.enterKey') };
    case SessionRowKind.Live:
    case SessionRowKind.New:
      return undefined;
  }
};

const STATE_WORDS = {
  waitingForPermission: 'state.permission',
  waitingForInput: 'state.input',
  yourTurn: 'state.turn',
  busy: 'state.busy',
  unknown: 'state.unknown',
  starting: 'state.starting',
} as const satisfies Record<RunningState, string>;

/** The state a session row says in words: what it waits on, busy while it works, starting before its record is read, unknown when it cannot be, ended once it is past. */
const stateOf = (row: SessionRow, t: TFunction<'session'>): string | undefined => {
  switch (row.kind) {
    case SessionRowKind.Hosted:
    case SessionRowKind.Live:
      return t(STATE_WORDS[row.state]);
    case SessionRowKind.Past:
      return t('state.ended');
    case SessionRowKind.New:
      return undefined;
  }
};

/** When the row's session started, or last ran if it is past. */
const sinceOf = (row: SessionRow): string | undefined => {
  switch (row.kind) {
    case SessionRowKind.Hosted:
    case SessionRowKind.Live:
      return row.since;
    case SessionRowKind.Past:
      return row.session.lastActivity;
    case SessionRowKind.New:
      return undefined;
  }
};

const ageWords = (row: SessionRow, t: TFunction<'session'>): string | undefined => {
  const since = sinceOf(row);
  const age = since === undefined ? undefined : ageOf(since, Date.now());
  return age === undefined ? undefined : t(`age.${age.unit}`, { count: age.count });
};

const lampOf = (row: SessionRow) => (row.kind === SessionRowKind.Hosted || row.kind === SessionRowKind.Live ? row.lamp : undefined);

interface Listed {
  row: SessionRow;
  index: number;
}

interface RowListProps {
  entries: readonly Listed[];
  cursor: number | undefined;
  onPress: (index: number) => void;
}

/** Rows of sessions; the one at `cursor` is selected, none while the cursor is elsewhere. */
const RowList = ({ entries, cursor, onPress }: RowListProps) => {
  const { t } = useTranslation('session');
  return (
    <Rows>
      {entries.map(({ row, index }) => (
        <Row key={index} {...labelled(row, t)} state={stateOf(row, t)} age={ageWords(row, t)} action={actionOf(row, t)} selected={index === cursor} lamp={lampOf(row)} onSelect={() => onPress(index)} />
      ))}
    </Rows>
  );
};

/** Why a session could not be started, with Retry; none while nothing failed. It stands whether or not a ticket is under the cursor. */
export const SessionFailure = ({ failure }: { failure: StartFailure | undefined }) => {
  const { t } = useTranslation('session');
  return failure === undefined ? null : (
    <Alert title={t('failure.title')} action={<Button onClick={() => void failure.retry()}>{t('failure.retry')}</Button>}>
      {failure.message}
    </Alert>
  );
};

/** The sessions of the selected ticket, in the ticket's pane: the ones running and the past ones to resume. The row that starts one is `NewSessionBar`'s. */
export const SessionsList = ({ rows, cursor, picker, onPress }: ListProps) => {
  const { t } = useTranslation('session');
  const listed: Listed[] = rows.map((row, index) => ({ row, index }));
  const ticketRows = listed.filter(({ row }) => row.kind !== SessionRowKind.New);
  const selected = picker === undefined ? cursor : undefined;
  return (
    <>
      {ticketRows.length === 0 ? null : (
        <div>
          <StatusHeading status={t('sessions.title')} count={ticketRows.length} />
          <RowList entries={ticketRows} cursor={selected} onPress={onPress} />
        </div>
      )}
    </>
  );
};

interface BarProps {
  rows: readonly SessionRow[];
  cursor: number;
  picker: VerbPicker | undefined;
  onPress: (index: number) => void;
  onPickVerb: (index: number) => void;
  onDismiss: () => void;
}

/** The ticket's `new session` bar, pinned under its body; its picker of verbs grows upward from it. None while the ticket cannot start a session. */
export const NewSessionBar = ({ rows, cursor, picker, onPress, onPickVerb, onDismiss }: BarProps) => {
  const { t } = useTranslation('session');
  const index = rows.findIndex((row) => row.kind === SessionRowKind.New);
  return index === -1 ? null : (
    <PickerBar
      label={t('row.new')}
      shortcut={t('row.newKey')}
      popupLabel={t('picker.title')}
      selected={index === cursor && picker === undefined}
      open={picker !== undefined}
      onPress={() => (picker === undefined ? onPress(index) : onDismiss())}
      onDismiss={onDismiss}
    >
      {picker === undefined ? null : (
        <>
          <StatusHeading status={t('picker.title')} count={picker.verbs.length} />
          <Rows>
            {picker.verbs.map((verb, at) => (
              <Row key={verb} label={t(`verb.${verb}`)} selected={at === picker.cursorAt()} lamp={undefined} onSelect={() => onPickVerb(at)} />
            ))}
          </Rows>
        </>
      )}
    </PickerBar>
  );
};
