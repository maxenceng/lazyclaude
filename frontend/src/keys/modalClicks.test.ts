import { afterEach, describe, expect, it } from 'vitest';
import { ModalClicks } from './modalClicks';

const surface = (modal: boolean) => {
  const element = document.body.appendChild(document.createElement('button'));
  if (modal) {
    element.setAttribute('data-modal', '');
  }
  return element;
};

const heard = (target: HTMLElement, type: string) => {
  const reached: string[] = [];
  target.addEventListener(type, () => reached.push(type));
  const event = new MouseEvent(type, { bubbles: true, cancelable: true });
  target.dispatchEvent(event);
  return { reached, prevented: event.defaultPrevented };
};

describe('the clicks a modal keeps from what is behind it', () => {
  let clicks: ModalClicks | undefined;
  afterEach(() => {
    clicks?.remove();
    document.body.replaceChildren();
  });

  it('stops a click outside every modal surface before the element under it sees it', () => {
    clicks = new ModalClicks(() => true);
    expect(heard(surface(false), 'click').reached).toEqual([]);
  });

  it('prevents a press outside, so it takes no focus from the popup', () => {
    clicks = new ModalClicks(() => true);
    expect(heard(surface(false), 'mousedown')).toEqual({ reached: [], prevented: true });
  });

  it('lets a click inside a modal surface through untouched', () => {
    clicks = new ModalClicks(() => true);
    expect(heard(surface(true), 'click')).toEqual({ reached: ['click'], prevented: false });
  });

  it('lets every click through while no modal is open', () => {
    clicks = new ModalClicks(() => false);
    expect(heard(surface(false), 'click')).toEqual({ reached: ['click'], prevented: false });
  });

  it('stops nothing once removed', () => {
    clicks = new ModalClicks(() => true);
    clicks.remove();
    expect(heard(surface(false), 'click').reached).toEqual(['click']);
  });
});
