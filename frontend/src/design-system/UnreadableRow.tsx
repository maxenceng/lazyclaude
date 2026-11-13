import { useTranslation } from 'react-i18next';
import styles from './UnreadableRow.module.css';

/** A ticket that could not be read: its id, then why. */
export const UnreadableRow = ({ id, reason }: { id: string; reason: string }) => {
  const { t } = useTranslation('designSystem');
  return <li className={styles.row} data-row="">{t('unreadableRow.row', { id, reason })}</li>;
};
