import { useCallback, useRef, useState } from 'react';
import { useDismissal } from './dismissal';
import { useTranslation } from 'react-i18next';
import styles from './WindowMenu.module.css';

export interface WindowMenuItem {
  label: string;
  shortcut: string;
  onSelect: () => void;
}

interface Props {
  name: string;
  items: readonly WindowMenuItem[];
}

/** The window's own menu at the far left of the status line: the app's name with a caret, opening what belongs to the window rather than to any pane. `esc` or a click outside closes it. */
export const WindowMenu = ({ name, items }: Props) => {
  const { t } = useTranslation('designSystem');
  const [open, setOpen] = useState(false);
  const wrap = useRef<HTMLDivElement>(null);

  const close = useCallback(() => setOpen(false), []);
  useDismissal(wrap, open, close, true);

  return (
    <div ref={wrap} className={styles.wrap}>
      <button
        type="button"
        className={styles.button}
        aria-haspopup="menu"
        aria-expanded={open}
        title={t('windowMenu.title')}
        onMouseDown={(event) => event.preventDefault()}
        onClick={() => setOpen(!open)}
      >
        <span className={styles.name}>{name}</span>
        <svg className={styles.caret} viewBox="0 0 12 12" aria-hidden="true">
          <path d="M2 4 L6 8 L10 4" />
        </svg>
      </button>
      {open ? (
        <div className={styles.list} role="menu" aria-label={t('windowMenu.title')}>
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              className={styles.item}
              aria-label={t('windowMenu.item', { action: item.label, key: item.shortcut })}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => {
                setOpen(false);
                item.onSelect();
              }}
            >
              <span>{item.label}</span>
              <kbd className={styles.key}>{item.shortcut}</kbd>
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
};
