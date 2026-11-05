import { useTranslation } from 'react-i18next';
import { Alert, Button, SessionRow as Row, StatusHeading, TicketList } from '../design-system';
import type { PastSessionView } from '../protocol';
import { type SessionRow, SessionRowKind } from './sessionRows';
import type { StartFailure } from './hosting';
import type { VerbPicker } from './verbPicker';

interface Props {
  rows: readonly SessionRow[];
  cursor: number;
  focused: boolean;
  picker: VerbPicker | undefined;
  failure: StartFailure | undefined;
  onPress: (index: number) => void;
  onPickVerb: (index: number) => void;
}

const titleOf = (session: PastSessionView): string | undefined => {
  if (session.title.kind === 'aiTitle') {
    return session.title.value;
  }
  return session.opening.kind === 'firstPrompt' ? session.opening.value : undefined;
};

/** The sessions of the selected ticket, in the ticket's pane: the ones hosted here, the older ones to resume, and the row that starts one. */
export const SessionsList = ({ rows, cursor, focused, picker, failure, onPress, onPickVerb }: Props) => {
  const { t } = useTranslation('session');
  const labelled = (row: SessionRow): { label: string; detail: string } => {
    switch (row.kind) {
      case SessionRowKind.Hosted:
        return { label: row.name ?? t('row.starting'), detail: t('row.hosted') };
      case SessionRowKind.Live:
        return { label: row.name, detail: row.age };
      case SessionRowKind.Past:
      case SessionRowKind.Unlinked:
        return { label: titleOf(row.session) ?? t('row.untitled', { id: row.session.id }), detail: row.session.age };
      case SessionRowKind.New:
        return { label: t('row.new'), detail: '' };
    }
  };
  const lampOf = (row: SessionRow) => (row.kind === SessionRowKind.Hosted || row.kind === SessionRowKind.Live ? row.lamp : undefined);
  const listed = rows.map((row, index) => ({ row, index }));
  const ticketRows = listed.filter(({ row }) => row.kind !== SessionRowKind.Unlinked);
  const olderRows = listed.filter(({ row }) => row.kind === SessionRowKind.Unlinked);
  const list = (entries: typeof listed) => (
    <TicketList>
      {entries.map(({ row, index }) => (
        <Row key={index} {...labelled(row)} selected={focused && picker === undefined && index === cursor} lamp={lampOf(row)} onSelect={() => onPress(index)} />
      ))}
    </TicketList>
  );
  return (
    <>
      {failure === undefined ? null : (
        <Alert title={t('failure.title')} action={<Button onClick={() => void failure.retry()}>{t('failure.retry')}</Button>}>
          {failure.message}
        </Alert>
      )}
      {picker === undefined ? null : (
        <div>
          <StatusHeading status={t('picker.title')} count={picker.verbs.length} />
          <TicketList>
            {picker.verbs.map((verb, index) => (
              <Row key={verb} label={t(`verb.${verb}`)} detail="" selected={index === picker.cursorAt()} lamp={undefined} onSelect={() => onPickVerb(index)} />
            ))}
          </TicketList>
        </div>
      )}
      {ticketRows.length === 0 ? null : (
        <div>
          <StatusHeading status={t('sessions.title')} count={ticketRows.length} />
          {list(ticketRows)}
        </div>
      )}
      {olderRows.length === 0 ? null : (
        <div>
          <StatusHeading status={t('sessions.older')} count={olderRows.length} />
          {list(olderRows)}
        </div>
      )}
    </>
  );
};
