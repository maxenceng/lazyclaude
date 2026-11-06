import { useTranslation } from 'react-i18next';
import { KeyBar } from '../design-system';

/** The keys that work while the ticket's sessions have focus, or over its verb picker while that is open. */
export const SessionsKeyBar = ({ picking, extra = [] }: { picking: boolean; extra?: readonly string[] }) => {
  const { t } = useTranslation('session');
  const keys = picking
    ? [t('keyBar.move'), t('keyBar.start'), t('keyBar.cancel')]
    : [t('keyBar.move'), t('keyBar.open'), t('keyBar.back'), t('keyBar.new'), ...extra];
  return <KeyBar keys={keys} />;
};

/** The keys that work while a hosted session's pane has focus; selecting needs shift while `claude` takes the mouse. */
export const DockKeyBar = ({ mouseReported }: { mouseReported: boolean }) => {
  const { t } = useTranslation('session');
  return <KeyBar keys={[t('keyBar.copy'), t('keyBar.paste'), ...(mouseReported ? [t('keyBar.select')] : [])]} />;
};
