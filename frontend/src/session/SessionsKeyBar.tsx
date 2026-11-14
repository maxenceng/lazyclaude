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

/** The keys that work while a hosted session's pane has focus, led by opening the link the pointer is on; selecting needs shift while `claude` takes the mouse. */
export const DockKeyBar = ({ mouseReported, hoveredLink }: { mouseReported: boolean; hoveredLink: string | undefined }) => {
  const { t } = useTranslation('session');
  const open = hoveredLink === undefined ? undefined : t('links.open', { url: hoveredLink });
  return <KeyBar lead={open} keys={[t('keyBar.copy'), t('keyBar.paste'), ...(mouseReported ? [t('keyBar.select')] : [])]} />;
};
