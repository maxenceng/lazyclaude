import { useTranslation } from 'react-i18next';
import { KeyBar } from '../design-system';

interface Props {
  /** Whether the main view shows a document, which `esc` closes before it leaves the panel. */
  documentShown: boolean;
  extra?: readonly string[];
}

/** The keys that work while the Docs panel has focus. */
export const DocsKeyBar = ({ documentShown, extra = [] }: Props) => {
  const { t } = useTranslation('docs');
  return <KeyBar keys={[t('keyBar.move'), t('keyBar.open'), ...(documentShown ? [t('keyBar.close')] : []), ...extra]} />;
};
