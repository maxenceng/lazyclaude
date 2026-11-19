import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Lamp, LampColour, LampSize } from './Lamp';
import { ruleOf, stylesheet } from './stylesheet';

const lamp = stylesheet('Lamp.module.css');

describe('a lamp tile', () => {
  it('keeps a line for its words even when it has none, so the numbers of tiles line up', () => {
    render(<Lamp colour={LampColour.Off} size={LampSize.Tile} number="PROJ-1" />);
    const tile = screen.getByRole('img');
    expect(tile.querySelectorAll('b, i')).toHaveLength(2);
    expect(ruleOf(lamp, '.tile')).toContain('grid-template-rows: var(--lamp-number-row) var(--lamp-words-row)');
  });
});

describe('a clickable lamp tile', () => {
  it('is a button named like the tile, that calls its click', () => {
    const onClick = vi.fn();
    render(<Lamp colour={LampColour.Turn} size={LampSize.Tile} number="PROJ-1" onClick={onClick} />);
    fireEvent.click(screen.getByRole('button', { name: 'PROJ-1: your turn' }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('keeps the window focus on a press', () => {
    render(<Lamp colour={LampColour.Turn} size={LampSize.Tile} number="PROJ-1" onClick={vi.fn()} />);
    expect(fireEvent.mouseDown(screen.getByRole('button'))).toBe(false);
  });

  it('is not a button without a click', () => {
    render(<Lamp colour={LampColour.Turn} size={LampSize.Tile} number="PROJ-1" />);
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('shows the blue focus edge and no hover change', () => {
    expect(ruleOf(lamp, '.button:focus-visible')).toContain('outline: var(--focus-edge) solid var(--focus)');
    expect(ruleOf(lamp, '.button')).toContain('cursor: default');
    expect(lamp).not.toContain(':hover');
  });
});

describe('a lamp marker', () => {
  it('draws a session that asks nothing as a hollow dot in the ended-dot ink, apart from the your-turn dot', () => {
    render(<Lamp colour={LampColour.Off} size={LampSize.Marker} />);
    expect(screen.getByRole('img')).toHaveTextContent('○');
    expect(screen.getByRole('img')).not.toHaveTextContent('•');
    expect(ruleOf(lamp, '.marker.off')).toContain('color: var(--ended-dot)');
  });
});

describe('a lamp pill', () => {
  it.each([
    [LampColour.Permission, 'permission', 'waiting for permission'],
    [LampColour.Input, 'needs input', 'waiting for input'],
    [LampColour.Turn, 'your turn', 'your turn'],
  ])('says a %s session in words, and names it for a screen reader', (colour, words, name) => {
    render(<Lamp colour={colour} size={LampSize.Pill} />);
    expect(screen.getByRole('img')).toHaveTextContent(words);
    expect(screen.getByRole('img')).toHaveAccessibleName(name);
  });

  it('is named for a screen reader by the words it shows, where it is given its own', () => {
    render(<Lamp colour={LampColour.Off} size={LampSize.Pill} words="busy" />);
    expect(screen.getByRole('img')).toHaveAccessibleName('busy');
  });
});
