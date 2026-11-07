import { useEffect, useRef } from 'react';
import styles from './TextInput.module.css';

const CANCEL_KEY = 'escape';
const SUBMIT_KEY = 'enter';

interface Props {
  label: string;
  value: string;
  readOnly: boolean;
  onChange: (value: string) => void;
  onSubmit: () => void;
  onCancel: () => void;
}

/** A one-line native input that takes focus when it appears: paste, selection and word deletion are the webview's own, every key typed in it is its own but enter, which submits, and escape, which cancels. */
export const TextInput = ({ label, value, readOnly, onChange, onSubmit, onCancel }: Props) => {
  const input = useRef<HTMLInputElement>(null);
  useEffect(() => {
    input.current?.focus();
  }, []);
  return (
    <div data-typing="">
      <input
        ref={input}
        className={styles.input}
        type="text"
        aria-label={label}
        value={value}
        readOnly={readOnly}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => {
          const key = event.key.toLowerCase();
          if (event.nativeEvent.isComposing) {
            return;
          }
          if (key === CANCEL_KEY) {
            event.preventDefault();
            onCancel();
          } else if (key === SUBMIT_KEY) {
            event.preventDefault();
            onSubmit();
          }
        }}
      />
    </div>
  );
};
