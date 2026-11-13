import { useTranslation } from 'react-i18next';
import { Alert, Badge, BadgeTone, Busy, Button, Cell, CellTone, CiGlyph, type ColumnHead, ColumnSize, Pane, Table, TableRow, Text, Tone } from '../design-system';
import type { ReviewPullRequestView, ReviewPullRequestsView } from '../protocol';
import { Elapsed, ageOf } from './age';
import { ciOf, ciResultOf } from './pullRequestList';

interface Props {
  state: ReviewPullRequestsView;
  /** The open pull requests in the order they are listed. */
  list: readonly ReviewPullRequestView[];
  cursor: number | undefined;
  /** The instant the ages are counted to. */
  now: Date;
  onOpen: (pullRequest: ReviewPullRequestView) => void;
  onRetry: () => void;
}

enum Column {
  Ci = 'ci',
  Ticket = 'ticket',
  Title = 'title',
  Branch = 'branch',
  Comments = 'comments',
  Age = 'age',
}

const COLUMNS: readonly { key: Column; size: ColumnHead['size'] }[] = [
  { key: Column.Ci, size: ColumnSize.Fit },
  { key: Column.Ticket, size: ColumnSize.Fit },
  { key: Column.Title, size: ColumnSize.Fill },
  { key: Column.Branch, size: ColumnSize.Trim },
  { key: Column.Comments, size: ColumnSize.Fit },
  { key: Column.Age, size: ColumnSize.Fit },
];

const CI_WORDS = { FAILED: 'failed', PENDING: 'pending', PASSED: 'passed' } as const;

const Row = ({ pullRequest, selected, now, onOpen }: { pullRequest: ReviewPullRequestView; selected: boolean; now: Date; onOpen: () => void }) => {
  const { t } = useTranslation('review');
  const result = ciResultOf(pullRequest);
  const ci = ciOf(result);
  const age = ageOf(pullRequest.openedAt, now);
  return (
    <TableRow stripe={ci} selected={selected} onSelect={onOpen}>
      <Cell size={ColumnSize.Fit}>
        <CiGlyph ci={ci} word={t(result === undefined ? 'pullRequests.ci.none' : `pullRequests.ci.${CI_WORDS[result]}`)} />
      </Cell>
      <Cell size={ColumnSize.Fit} mono>
        {pullRequest.closing.kind === 'closesIssue' ? (
          pullRequest.closing.number
        ) : (
          <span role="img" aria-label={t('pullRequests.noTicket')}>
            –
          </span>
        )}
      </Cell>
      <Cell size={ColumnSize.Fill} note={`#${pullRequest.number}`}>
        {pullRequest.title}
      </Cell>
      <Cell size={ColumnSize.Trim} tone={CellTone.Dim} mono>
        {pullRequest.head}
      </Cell>
      <Cell size={ColumnSize.Fit} tone={CellTone.Dim}>
        {t('pullRequests.commentCount', { count: pullRequest.comments })}
        {pullRequest.pendingReview ? <Badge tone={BadgeTone.Amber}>{t('pullRequests.pendingBadge')}</Badge> : null}
      </Cell>
      <Cell size={ColumnSize.Fit} tone={CellTone.Dim}>
        {age.unit === Elapsed.Now || age.unit === Elapsed.Unknown ? t(`pullRequests.age.${age.unit}`) : t(`pullRequests.age.${age.unit}`, { count: age.count })}
      </Cell>
    </TableRow>
  );
};

/** The Pull requests tab: every open pull request as a row of a full-width table, its CI read first; a press on a row opens its review. */
export const PullRequestsPane = ({ state, list, cursor, now, onOpen, onRetry }: Props) => {
  const { t } = useTranslation('review');
  return (
    <Pane title={t('pullRequests.title')} focused chips={state.kind === 'openPullRequests' && list.length > 0 ? [{ label: t('pullRequests.open', { count: list.length }) }] : []}>
      {state.kind === 'checkingPullRequests' ? <Busy>{t('pullRequests.loading')}</Busy> : null}
      {state.kind === 'unavailablePullRequests' ? (
        <Alert title={t('pullRequests.fault')} action={<Button onClick={onRetry}>{t('pullRequests.retry')}</Button>}>
          {state.reason}
        </Alert>
      ) : null}
      {state.kind === 'openPullRequests' && list.length === 0 ? <Text tone={Tone.Dim}>{t('pullRequests.empty')}</Text> : null}
      {state.kind === 'openPullRequests' && list.length > 0 ? (
        <Table label={t('pullRequests.label')} heads={COLUMNS.map((column) => ({ label: t(`pullRequests.columns.${column.key}`), size: column.size }))}>
          {list.map((pullRequest) => (
            <Row key={pullRequest.number} pullRequest={pullRequest} selected={pullRequest.number === cursor} now={now} onOpen={() => onOpen(pullRequest)} />
          ))}
        </Table>
      ) : null}
    </Pane>
  );
};
