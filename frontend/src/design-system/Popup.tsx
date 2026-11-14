import { type MouseEvent, type ReactNode, useId } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, ButtonTone } from './Button';
import { PaneAction, PaneActionKind } from './PaneAction';
import styles from './Popup.module.css';

/** One answer a popup takes: its words, the key bound to it as set in its chip, and what it does, which is what the key does. */
export interface PopupAnswer {
  label: string;
  chip: string;
  onClick: () => void;
}

interface Props {
  title: string;
  /** The answer that commits, at the foot's right end in the affirm tone. */
  main: PopupAnswer;
  /** The way out, left of the main answer; the close action in the title row takes it too. */
  out: PopupAnswer;
  /** A press outside the popup: given, it leaves the popup as its own way out does; absent, the press does nothing. */
  onDismiss?: () => void;
  /** What the popup says or asks; a popup with nothing to show between its title row and its answers leaves it out. */
  children?: ReactNode;
}

/** A press on the scrim itself, not on the popup inside it; it keeps the focus in the popup, since the window's modal guard lets a press on the scrim through. */
const dismissOn =
  (onDismiss: (() => void) | undefined) =>
  (event: MouseEvent): void => {
    if (event.target === event.currentTarget) {
      event.preventDefault();
      onDismiss?.();
    }
  };

/** The one frame of every popup: a lit title row with its close action, a body, and a sunk foot of answers, centred over the window dimmed by a scrim. A press, not a click, outside it dismisses it, so a selection dragged out of it does not; `data-modal` lets the mouse reach it alone. */
export const Popup = ({ title, main, out, onDismiss, children }: Props) => {
  const { t } = useTranslation('designSystem');
  const heading = useId();
  return (
    <div className={styles.scrim} role="presentation" data-modal="" onMouseDown={dismissOn(onDismiss)}>
      <section role="dialog" aria-modal="true" aria-labelledby={heading} className={styles.frame}>
        <div className={styles.head}>
          <h2 id={heading} className={styles.title}>
            {title}
          </h2>
          <PaneAction kind={PaneActionKind.Close} label={t('popup.close')} shortcut={out.chip} onClick={out.onClick} />
        </div>
        {children === undefined ? null : (
          <div className={styles.body} data-popup-body="">
            {children}
          </div>
        )}
        <div className={styles.foot} data-popup-foot="">
          <Button tone={ButtonTone.Quiet} chip={out.chip} onClick={out.onClick}>
            {out.label}
          </Button>
          <Button tone={ButtonTone.Affirm} chip={main.chip} onClick={main.onClick}>
            {main.label}
          </Button>
        </div>
      </section>
    </div>
  );
};
