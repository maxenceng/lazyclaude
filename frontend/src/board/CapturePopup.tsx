import { useTranslation } from 'react-i18next';
import { Popup, Text, TextInput, Tone } from '../design-system';
import { useStore } from '../shared/useStore';
import type { CaptureController } from './captureController';
import { CaptureStateKind } from './captureState';

/** The popup a draft ticket's title is typed in: a native input, then "creating…" or "⚠ reason" under it. */
export const CapturePopup = ({ capture }: { capture: CaptureController }) => {
  const { t } = useTranslation('board');
  const state = useStore(capture.state);
  if (state.kind === CaptureStateKind.Closed) {
    return null;
  }
  return (
    <Popup
      title={t('capture.title')}
      main={{ label: t('capture.create'), chip: t('capture.createKey'), onClick: () => capture.create() }}
      out={{ label: t('capture.cancel'), chip: t('capture.cancelKey'), onClick: () => capture.cancel() }}
    >
      <TextInput
        label={t('capture.label')}
        value={state.title}
        readOnly={state.kind === CaptureStateKind.Creating}
        onChange={(title) => capture.edit(title)}
        onSubmit={() => capture.create()}
        onCancel={() => capture.cancel()}
      />
      {state.kind === CaptureStateKind.Creating ? (
        <div role="status">
          <Text tone={Tone.Dim} tight>{t('capture.creating')}</Text>
        </div>
      ) : null}
      {state.kind === CaptureStateKind.Refused ? (
        <div role="alert">
          <Text tone={Tone.Warning} tight>{t('capture.refused', { reason: state.reason })}</Text>
        </div>
      ) : null}
    </Popup>
  );
};
