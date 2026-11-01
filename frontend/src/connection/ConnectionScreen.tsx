import { Alert, Busy, Button, Text } from '../design-system';
import type { Status } from '../protocol';

interface Props {
  status: Exclude<Status, { kind: 'ready' }>;
  onRestart: () => void;
}

/** What the window says while the sidecar is not there to show a board. */
export function ConnectionScreen({ status, onRestart }: Props) {
  switch (status.kind) {
    case 'connecting':
      return <Busy>Starting the backend…</Busy>;
    case 'refused':
      return (
        <Alert title="This backend cannot be used">
          The backend speaks protocol {status.actual}; this window was built for protocol {status.expected}.
        </Alert>
      );
    case 'exited':
      return (
        <Alert title="The backend stopped" action={<Button onClick={onRestart}>Restart</Button>}>
          {status.code === null ? 'It ended with no exit code.' : `It ended with exit code ${status.code}.`}
          {status.stderr.length === 0 ? null : <Text mono>{status.stderr.join('\n')}</Text>}
        </Alert>
      );
  }
}
