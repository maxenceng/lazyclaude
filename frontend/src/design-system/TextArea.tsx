import { useEffect, useRef } from 'react';
import styles from './TextArea.module.css';

const CANCEL_KEY = 'escape';
const SAVE_KEY = 's';

interface Props {
  label: string;
  value: string;
  readOnly: boolean;
  onChange: (value: string) => void;
  onSave: () => void;
  onCancel: () => void;
}

/** A box the user writes in: it takes focus when it appears and every key typed in it is its own; ctrl+s saves and escape cancels, unless it ends an input method composition. */
export const TextArea = ({ label, value, readOnly, onChange, onSave, onCancel }: Props) => {
  const area = useRef<HTMLTextAreaElement>(null);
  useEffect(() => {
    area.current?.focus();
  }, []);
  return (
    <div data-typing="">
      <textarea
        ref={area}
        className={styles.area}
        aria-label={label}
        value={value}
        readOnly={readOnly}
        rows={6}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => {
          const key = event.key.toLowerCase();
          if (event.nativeEvent.isComposing) {
            return;
          }
          if (key === CANCEL_KEY) {
            event.preventDefault();
            onCancel();
          } else if (key === SAVE_KEY && event.ctrlKey) {
            event.preventDefault();
            onSave();
          }
        }}
      />
    </div>
  );
};
