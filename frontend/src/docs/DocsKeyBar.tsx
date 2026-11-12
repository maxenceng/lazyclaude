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
  return <KeyBar keys={[t('keyBar.move'), t('keyBar.open'), ...(documentShown ? [t('keyBar.close'), t('keyBar.toDocument')] : []), ...extra]} />;
};

/** The keys that work while the shown document has focus: it scrolls, goes back to the Docs panel, or closes to the ticket. */
export const DocumentKeyBar = ({ extra = [] }: { extra?: readonly string[] }) => {
  const { t } = useTranslation('docs');
  return <KeyBar keys={[t('keyBar.scroll'), t('keyBar.halfPage'), t('keyBar.toDocs'), t('keyBar.close'), ...extra]} />;
};
