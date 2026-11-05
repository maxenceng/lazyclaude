import { describe, expect, it } from 'vitest';
import { createWindowFocus, type FocusSource } from './windowFocus';

const source = (focused: boolean) => {
  let notify: (focused: boolean) => void = () => {};
  let unlistened = 0;
  let answer: (focused: boolean) => void = () => {};
  const fake: FocusSource = {
    isFocused: () => new Promise((resolve) => (answer = resolve)),
    onFocusChanged: (handler) => {
      notify = handler;
      return Promise.resolve(() => (unlistened += 1));
    },
  };
  return { fake, changed: (next: boolean) => notify(next), answered: () => answer(focused), unlistened: () => unlistened };
};

const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('whether the window has focus', () => {
  it('is not focused until the window says so', () => {
    expect(createWindowFocus(source(true).fake).focused.get()).toBe(false);
  });

  it('takes the focus the window has at start', async () => {
    const window = source(true);
    const focus = createWindowFocus(window.fake);
    await settle();
    window.answered();
    await settle();
    expect(focus.focused.get()).toBe(true);
  });

  it('follows the window as it gains and loses focus', async () => {
    const window = source(false);
    const focus = createWindowFocus(window.fake);
    await settle();
    window.changed(true);
    expect(focus.focused.get()).toBe(true);
    window.changed(false);
    expect(focus.focused.get()).toBe(false);
  });

  it('does not let the answer at start overwrite a change that came first', async () => {
    const window = source(false);
    const focus = createWindowFocus(window.fake);
    await settle();
    window.changed(true);
    window.answered();
    await settle();
    expect(focus.focused.get()).toBe(true);
  });

  it('stops listening once disposed', async () => {
    const window = source(false);
    const focus = createWindowFocus(window.fake);
    await settle();
    focus.dispose();
    await settle();
    expect(window.unlistened()).toBe(1);
  });
});
