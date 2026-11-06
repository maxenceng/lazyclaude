import { useTranslation } from 'react-i18next';
import { KeyBar } from '../design-system';

export type ReviewPanelFocus = 'diff' | 'comments' | 'files';

/** The keys that work while a panel of a review has focus. */
export const ReviewKeyBar = ({ panel }: { panel: ReviewPanelFocus }) => {
  const { t } = useTranslation('review');
  return <KeyBar keys={t(`keyBar.${panel}`, { returnObjects: true })} />;
};
