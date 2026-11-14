import { useTranslation } from 'react-i18next';
import { KeyBar } from '../design-system';

interface Props {
  /** Whether a ticket is under the cursor: with none, moving, opening and starting a session act on nothing and are left off. */
  ticket: boolean;
  /** Whether there is a project to capture a draft into. */
  capture: boolean;
  extra?: readonly string[];
}

/** The keys that work while the Board has focus. */
export const BoardKeyBar = ({ ticket, capture, extra = [] }: Props) => {
  const { t } = useTranslation('board');
  const ticketKeys = ticket ? [t('keyBar.move'), t('keyBar.open'), t('keyBar.new')] : [];
  return <KeyBar keys={[...ticketKeys, ...(capture ? [t('keyBar.capture')] : []), ...extra]} />;
};

/** The keys that work on the Board tab: with no ticket under the cursor, moving and going to it act on nothing and are left off. */
export const BoardTabKeyBar = ({ ticket, capture, extra = [] }: Props) => {
  const { t } = useTranslation('board');
  const ticketKeys = ticket ? [t('keyBar.columns'), t('keyBar.openTicket')] : [];
  return <KeyBar keys={[...ticketKeys, ...(capture ? [t('keyBar.capture')] : []), ...extra]} />;
};

/** The keys that work while the capture popup is open: nothing else does. */
export const CaptureKeyBar = () => {
  const { t } = useTranslation('board');
  return <KeyBar keys={[t('keyBar.start'), t('keyBar.cancel')]} />;
};
