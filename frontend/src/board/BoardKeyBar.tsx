import { useTranslation } from 'react-i18next';
import { KeyBar } from '../design-system';

/** The keys that work while the Board has focus. */
export const BoardKeyBar = ({ extra = [] }: { extra?: readonly string[] }) => {
  const { t } = useTranslation('board');
  return <KeyBar keys={[t('keyBar.move'), t('keyBar.open'), t('keyBar.new'), t('keyBar.newUnlinked'), ...extra]} />;
};

/** The keys that work on the Board tab. */
export const BoardTabKeyBar = ({ extra = [] }: { extra?: readonly string[] }) => {
  const { t } = useTranslation('board');
  return <KeyBar keys={[t('keyBar.columns'), t('keyBar.openTicket'), ...extra]} />;
};
