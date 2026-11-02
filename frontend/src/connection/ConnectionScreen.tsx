import { useTranslation } from 'react-i18next';
import { Alert, Busy, Button, Text } from '../design-system';
import { StatusKind, type Unready } from '../protocol';

interface Props {
  status: Unready;
  onRestart: () => void;
}

/** What the window says while the sidecar is not there to show a board. */
export const ConnectionScreen = ({ status, onRestart }: Props) => {
  const { t } = useTranslation();
  switch (status.kind) {
    case StatusKind.Connecting:
      return <Busy>{t('connection.connecting')}</Busy>;
    case StatusKind.Refused:
      return <Alert title={t('connection.refusedTitle')}>{t('connection.refused', { actual: status.actual, expected: status.expected })}</Alert>;
    case StatusKind.Exited:
      return (
        <Alert title={t('connection.exitedTitle')} action={<Button onClick={onRestart}>{t('connection.restart')}</Button>}>
          {status.code === null ? t('connection.exitedWithoutCode') : t('connection.exitedWithCode', { code: status.code })}
          {status.stderr.length === 0 ? null : <Text mono>{status.stderr.join('\n')}</Text>}
        </Alert>
      );
  }
};
