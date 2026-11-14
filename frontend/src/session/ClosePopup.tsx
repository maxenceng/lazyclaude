import { useTranslation } from 'react-i18next';
import { Popup, Text } from '../design-system';

interface Props {
  /** Close ends the session; any other answer, esc, a press outside or Keep, keeps it running. */
  onAnswer: (close: boolean) => void;
}

/** The question a close asks of a session that is busy, waiting on you or unknown: y closes it, anything else keeps it. */
export const ClosePopup = ({ onAnswer }: Props) => {
  const { t } = useTranslation('session');
  return (
    <Popup
      title={t('close.title')}
      main={{ label: t('close.yes'), chip: t('close.yesKey'), onClick: () => onAnswer(true) }}
      out={{ label: t('close.no'), chip: t('close.noKey'), onClick: () => onAnswer(false) }}
      onDismiss={() => onAnswer(false)}
    >
      <Text tight>{t('close.body')}</Text>
    </Popup>
  );
};
