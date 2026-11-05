import { useTranslation } from 'react-i18next';
import { KeyBar } from '../design-system';

/** The keys that work while the Board has focus. */
export const BoardKeyBar = () => {
  const { t } = useTranslation('board');
  return <KeyBar keys={[t('keyBar.move'), t('keyBar.open'), t('keyBar.new'), t('keyBar.newUnlinked')]} />;
};
