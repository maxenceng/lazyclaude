import { useTranslation } from 'react-i18next';
import { Alert, Busy, Button, Pane, PullRequestRow, Rows, Text, Tone } from '../design-system';
import type { ReviewPullRequestView, ReviewPullRequestsView } from '../protocol';
import { ciOf, ciResultOf } from './pullRequestList';

interface Props {
  state: ReviewPullRequestsView;
  /** The open pull requests in the order they are listed. */
  list: readonly ReviewPullRequestView[];
  cursor: number | undefined;
  onOpen: (pullRequest: ReviewPullRequestView) => void;
  onRetry: () => void;
}

/** The Pull requests tab: every open pull request, one line each; a press opens its review. */
export const PullRequestsPane = ({ state, list, cursor, onOpen, onRetry }: Props) => {
  const { t } = useTranslation('review');
  return (
    <Pane title={t('pullRequests.title')} focused>
      {state.kind === 'checkingPullRequests' ? <Busy>{t('pullRequests.loading')}</Busy> : null}
      {state.kind === 'unavailablePullRequests' ? (
        <Alert title={t('pullRequests.fault')} action={<Button onClick={onRetry}>{t('pullRequests.retry')}</Button>}>
          {state.reason}
        </Alert>
      ) : null}
      {state.kind === 'openPullRequests' && list.length === 0 ? <Text tone={Tone.Dim}>{t('pullRequests.empty')}</Text> : null}
      {state.kind === 'openPullRequests' && list.length > 0 ? (
        <Rows>
          {list.map((pullRequest) => (
            <PullRequestRow
              key={pullRequest.number}
              ci={ciOf(ciResultOf(pullRequest))}
              number={pullRequest.number}
              title={pullRequest.title}
              head={pullRequest.head}
              selected={pullRequest.number === cursor}
              onSelect={() => onOpen(pullRequest)}
            />
          ))}
        </Rows>
      ) : null}
    </Pane>
  );
};
