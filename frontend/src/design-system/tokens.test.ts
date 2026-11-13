import { describe, expect, it } from 'vitest';
import { stylesheet } from './stylesheet';

const TOKEN = /--([a-z0-9-]+):\s*(#[0-9a-fA-F]{6})\s*;/g;
const MARK_CONTRAST = 4.5;
const CHANNEL_SPAN = 255;
const LINEAR_KNEE = 0.03928;
const LINEAR_SLOPE = 12.92;
const GAMMA_OFFSET = 0.055;
const GAMMA_SCALE = 1.055;
const GAMMA = 2.4;
const FLARE = 0.05;
const RED_WEIGHT = 0.2126;
const GREEN_WEIGHT = 0.7152;
const BLUE_WEIGHT = 0.0722;
const HEX_RADIX = 16;

const tokens = new Map<string, string>(Array.from(stylesheet('tokens.css').matchAll(TOKEN), (match) => [match[1] ?? '', match[2] ?? '']));

const colourOf = (name: string): string => {
  const value = tokens.get(name);
  if (value === undefined) {
    throw new Error(`no colour token --${name}`);
  }
  return value;
};

const linear = (channel: number): number => {
  const unit = channel / CHANNEL_SPAN;
  return unit <= LINEAR_KNEE ? unit / LINEAR_SLOPE : ((unit + GAMMA_OFFSET) / GAMMA_SCALE) ** GAMMA;
};

const luminance = (hex: string): number => {
  const [red = 0, green = 0, blue = 0] = [1, 3, 5].map((at) => linear(Number.parseInt(hex.slice(at, at + 2), HEX_RADIX)));
  return RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue;
};

const contrast = (ink: string, ground: string): number => {
  const [light = 0, dark = 0] = [luminance(colourOf(ink)), luminance(colourOf(ground))].sort((a, b) => b - a);
  return (light + FLARE) / (dark + FLARE);
};

describe('the dim marks', () => {
  it.each([
    ['the unlit lamp ink', 'lamp-off-ink', 'lamp-off'],
    ['the terminal bright black', 'ansi-bright-black', 'terminal-ground'],
    ['an ended session dot', 'ended-dot', 'pane'],
    ['a lit title row', 'lit-ink', 'lit'],
    ['a dim label on a pane', 'dim', 'pane'],
    ['a dim label on a lifted pane', 'dim', 'pane-lift'],
    ['the done ticket ink', 'dim-done', 'pane'],
    ['a session row that waits on nothing', 'dim', 'card'],
    ['a session detail on a your-turn row', 'lamp-turn-tile-dim', 'lamp-turn-tile'],
    ['a session detail on a needs-input row', 'lamp-turn-tile-dim', 'lamp-input-tile'],
    ['a session detail on a permission row', 'lamp-turn-tile-dim', 'lamp-permission-tile'],
    ['the text of a needs-input row', 'text', 'lamp-input-tile'],
    ['the text of a permission row', 'text', 'lamp-permission-tile'],
  ])('%s reaches 4.5:1 against its ground', (_name, ink, ground) => {
    expect(contrast(ink, ground)).toBeGreaterThanOrEqual(MARK_CONTRAST);
  });
});

describe('the colours that carry meaning', () => {
  it.each(['status-in-progress', 'status-in-review', 'status-todo', 'status-draft', 'status-done', 'type-feature', 'type-fix', 'type-chore'])(
    '%s reads on a pane',
    (ink) => {
      expect(contrast(ink, 'pane')).toBeGreaterThanOrEqual(MARK_CONTRAST);
    },
  );
});
