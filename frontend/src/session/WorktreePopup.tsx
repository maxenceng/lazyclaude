import { useTranslation } from 'react-i18next';
import { Popup, Text, Tone } from '../design-system';

interface Props {
  /** The worktree's path as shown. */
  shown: string;
  /** Remove takes the worktree away; any other answer, esc, a press outside, the close action or Keep, keeps it. */
  onAnswer: (remove: boolean) => void;
}

/** The question `d` asks of a clean worktree, as the TUI's removal does: y removes it, anything else keeps it. */
export const WorktreePopup = ({ shown, onAnswer }: Props) => {
  const { t } = useTranslation('session');
  return (
    <Popup
      title={t('removeWorktree.title')}
      main={{ label: t('removeWorktree.yes'), chip: t('removeWorktree.yesKey'), onClick: () => onAnswer(true) }}
      out={{ label: t('removeWorktree.no'), chip: t('removeWorktree.noKey'), onClick: () => onAnswer(false) }}
      onDismiss={() => onAnswer(false)}
    >
      <Text tight mono>
        {shown}
      </Text>
      <Text tone={Tone.Dim} tight>
        {t('removeWorktree.body')}
      </Text>
    </Popup>
  );
};
