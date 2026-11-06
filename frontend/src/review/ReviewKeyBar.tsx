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
