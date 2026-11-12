import { useTranslation } from 'react-i18next';
import { KeyBar } from '../design-system';

/** The review's panels that carry their own keys. */
export enum ReviewFocus {
  Diff = 'diff',
  Comments = 'comments',
  Files = 'files',
}

/** The keys that work while a panel of a review has focus. */
export const ReviewKeyBar = ({ panel }: { panel: ReviewFocus }) => {
  const { t } = useTranslation('review');
  return <KeyBar keys={t(`keyBar.${panel}`, { returnObjects: true })} />;
};

/** The keys that work on the Pull requests tab: moving and reviewing only while a pull request is listed to move over. */
export const PullRequestsKeyBar = ({ listed, extra = [] }: { listed: boolean; extra?: readonly string[] }) => {
  const { t } = useTranslation('review');
  return <KeyBar keys={[...(listed ? [t('keyBar.pullRequests'), t('keyBar.pullRequestsOpen')] : []), ...extra]} />;
};
