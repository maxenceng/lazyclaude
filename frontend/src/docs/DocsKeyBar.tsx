import { useTranslation } from 'react-i18next';
import { KeyBar } from '../design-system';

/** The keys that work while the Docs panel has focus. */
export const DocsKeyBar = () => {
  const { t } = useTranslation('docs');
  return <KeyBar keys={[t('keyBar.move'), t('keyBar.open')]} />;
};
