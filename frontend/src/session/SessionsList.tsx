import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Alert, Button, PickerBar, SessionRow as Row, StatusHeading, Rows } from '../design-system';
import type { SessionPastSessionView } from '../protocol';
import { type SessionRow, SessionRowKind } from './sessionRows';
import type { StartFailure } from './hosting';
import type { VerbPicker } from './verbPicker';

interface ListProps {
  rows: readonly SessionRow[];
  cursor: number;
  picker: VerbPicker | undefined;
  failure: StartFailure | undefined;
  onPress: (index: number) => void;
}

const titleOf = (session: SessionPastSessionView): string | undefined => {
  if (session.title.kind === 'aiTitle') {
    return session.title.value;
  }
  return session.opening.kind === 'firstPrompt' ? session.opening.value : undefined;
};

const labelled = (row: SessionRow, t: TFunction<'session'>): { label: string; detail: string } => {
  switch (row.kind) {
    case SessionRowKind.Hosted:
      return { label: row.name ?? t('row.starting'), detail: t('row.hosted') };
    case SessionRowKind.Live:
      return { label: row.name, detail: '' };
    case SessionRowKind.Past:
    case SessionRowKind.Unlinked:
      return { label: titleOf(row.session) ?? t('row.untitled', { id: row.session.id }), detail: '' };
    case SessionRowKind.New:
      return { label: t('row.new'), detail: '' };
  }
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
        <Row key={index} {...labelled(row, t)} selected={index === cursor} lamp={lampOf(row)} onSelect={() => onPress(index)} />
      ))}
    </Rows>
  );
};

/** The sessions of the selected ticket, in the ticket's pane: the ones hosted here and the older ones to resume. The row that starts one is `NewSessionBar`'s. */
export const SessionsList = ({ rows, cursor, picker, failure, onPress }: ListProps) => {
  const { t } = useTranslation('session');
  const listed: Listed[] = rows.map((row, index) => ({ row, index }));
  const ticketRows = listed.filter(({ row }) => row.kind !== SessionRowKind.Unlinked && row.kind !== SessionRowKind.New);
  const olderRows = listed.filter(({ row }) => row.kind === SessionRowKind.Unlinked);
  const selected = picker === undefined ? cursor : undefined;
  return (
    <>
      {failure === undefined ? null : (
        <Alert title={t('failure.title')} action={<Button onClick={() => void failure.retry()}>{t('failure.retry')}</Button>}>
          {failure.message}
        </Alert>
      )}
      {ticketRows.length === 0 ? null : (
        <div>
          <StatusHeading status={t('sessions.title')} count={ticketRows.length} />
          <RowList entries={ticketRows} cursor={selected} onPress={onPress} />
        </div>
      )}
      {olderRows.length === 0 ? null : (
        <div>
          <StatusHeading status={t('sessions.older')} count={olderRows.length} />
          <RowList entries={olderRows} cursor={selected} onPress={onPress} />
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
              <Row key={verb} label={t(`verb.${verb}`)} detail="" selected={at === picker.cursorAt()} lamp={undefined} onSelect={() => onPickVerb(at)} />
            ))}
          </Rows>
        </>
      )}
    </PickerBar>
  );
};
