import { describe, expect, it } from 'vitest';
import { type FocusSource, WindowFocus } from './windowFocus';

/** A window whose focus answer and changes the test releases. */
class FakeFocus implements FocusSource {
  unlistened = 0;
  private notify: (focused: boolean) => void = () => {};
  private answer: (focused: boolean) => void = () => {};

  constructor(private readonly focused: boolean) {}

  isFocused(): Promise<boolean> {
    return new Promise((resolve) => (this.answer = resolve));
  }

  onFocusChanged(handler: (focused: boolean) => void): Promise<() => void> {
    this.notify = handler;
    return Promise.resolve(() => (this.unlistened += 1));
  }

  changed(next: boolean): void {
    this.notify(next);
  }

  answered(): void {
    this.answer(this.focused);
  }
}

const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('whether the window has focus', () => {
  it('is not focused until the window says so', () => {
    expect(new WindowFocus(new FakeFocus(true)).focused.get()).toBe(false);
  });

  it('takes the focus the window has at start', async () => {
    const window = new FakeFocus(true);
    const focus = new WindowFocus(window);
    await settle();
    window.answered();
    await settle();
    expect(focus.focused.get()).toBe(true);
  });

  it('follows the window as it gains and loses focus', async () => {
    const window = new FakeFocus(false);
    const focus = new WindowFocus(window);
    await settle();
    window.changed(true);
    expect(focus.focused.get()).toBe(true);
    window.changed(false);
    expect(focus.focused.get()).toBe(false);
  });

  it('does not let the answer at start overwrite a change that came first', async () => {
    const window = new FakeFocus(false);
    const focus = new WindowFocus(window);
    await settle();
    window.changed(true);
    window.answered();
    await settle();
    expect(focus.focused.get()).toBe(true);
  });

  it('stops listening once disposed', async () => {
    const window = new FakeFocus(false);
    const focus = new WindowFocus(window);
    await settle();
    focus.dispose();
    await settle();
    expect(window.unlistened).toBe(1);
  });
});
