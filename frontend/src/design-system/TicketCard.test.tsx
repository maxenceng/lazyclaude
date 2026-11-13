import { render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Ci } from './CiGlyph';
import { LampColour } from './Lamp';
import { TicketCard } from './TicketCard';
import { TypeColour } from './TypeTag';
import { ruleOf, stylesheet } from './stylesheet';

const card = stylesheet('TicketCard.module.css');

describe('a ticket card', () => {
  it('scrolls into its column when selected, so a card below the fold stays in sight', () => {
    const scrolled = vi.spyOn(Element.prototype, 'scrollIntoView');
    const { rerender } = render(<TicketCard id="PROJ-1" title="A ticket" selected={false} lamp={undefined} onSelect={() => {}} />);
    expect(scrolled).not.toHaveBeenCalled();
    rerender(<TicketCard id="PROJ-1" title="A ticket" selected lamp={undefined} onSelect={() => {}} />);
    expect(scrolled.mock.contexts).toEqual([screen.getByRole('button')]);
    scrolled.mockRestore();
  });

  it('puts the whole title over a footer of its number and type', () => {
    render(<TicketCard id="PROJ-1" title="A ticket" type={{ name: 'feature', colour: TypeColour.Feature }} selected={false} lamp={undefined} onSelect={() => {}} />);
    expect(screen.getByRole('button')).toHaveTextContent('A ticketPROJ-1feature');
  });

  it('carries its draft mark and its CI result in the footer', () => {
    render(<TicketCard id="PROJ-1" title="A ticket" selected={false} lamp={undefined} pullRequest={{ marked: true, ci: Ci.Failed }} onSelect={() => {}} />);
    const button = screen.getByRole('button');
    expect(within(button).getByRole('img', { name: 'draft or without checks' })).toBeInTheDocument();
    expect(within(button).getByRole('img', { name: 'checks failed' })).toBeInTheDocument();
  });

  it('has a left edge in its type colour and the type written in its footer', () => {
    render(<TicketCard id="PROJ-1" title="A ticket" type={{ name: 'fix', colour: TypeColour.Fix }} selected={false} lamp={undefined} onSelect={() => {}} />);
    expect(screen.getByRole('button'), 'the edge reads its colour from the type').toHaveAttribute('data-type', 'fix');
    expect(ruleOf(card, ".card[data-type='fix']")).toContain('var(--type-fix)');
    expect(screen.getByText('fix')).toBeInTheDocument();
  });

  it('has no coloured edge and no type in its footer without a type', () => {
    render(<TicketCard id="PROJ-1" title="A draft" type={undefined} selected={false} lamp={undefined} onSelect={() => {}} />);
    expect(screen.getByRole('button'), 'no type means no edge colour to read').not.toHaveAttribute('data-type');
    expect(screen.getByRole('button')).toHaveTextContent('A draftPROJ-1');
  });

  it('keeps an untyped card\'s edge uncoloured when a waiting session tints it', () => {
    render(<TicketCard id="PROJ-1" title="A draft" type={undefined} selected={false} lamp={LampColour.Input} onSelect={() => {}} />);
    expect(screen.getByRole('button')).not.toHaveAttribute('data-type');
    expect(ruleOf(card, '.card[data-lamp]:not([data-type])'), 'the lamp tint must not read as a type edge').toContain('border-left-color: var(--card-edge)');
  });

  it('keeps a type outside the coloured ones written, with an edge of no colour of its own', () => {
    render(<TicketCard id="PROJ-1" title="A ticket" type={{ name: 'spike', colour: undefined }} selected={false} lamp={undefined} onSelect={() => {}} />);
    expect(screen.getByText('spike')).toBeInTheDocument();
    expect(screen.getByRole('button')).toHaveAttribute('data-type', 'other');
  });

  it.each([
    [LampColour.Permission, 'permission'],
    [LampColour.Input, 'needs input'],
    [LampColour.Turn, 'your turn'],
  ])('carries the worded pill of a %s session and is tinted in its colour', (lamp, words) => {
    render(<TicketCard id="PROJ-1" title="A ticket" selected={false} lamp={lamp} onSelect={() => {}} />);
    expect(within(screen.getByRole('button')).getByRole('img')).toHaveTextContent(words);
    expect(screen.getByRole('button')).toHaveAttribute('data-lamp', lamp);
    expect(ruleOf(card, `.card[data-lamp='${lamp}']`)).toContain(`var(--lamp-${lamp}-tile)`);
  });

  it('carries no pill and no tint when no session waits', () => {
    render(<TicketCard id="PROJ-1" title="A ticket" selected={false} lamp={undefined} onSelect={() => {}} />);
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.getByRole('button')).not.toHaveAttribute('data-lamp');
  });

  it('marks the selected card by a lifted ground and a brighter border, never an outline', () => {
    expect(card, 'an outline drawn outside the card clashed with the hover').not.toMatch(/outline/);
    expect(ruleOf(card, '.card.selected:not([data-lamp])')).toContain('background: var(--cursor-card)');
    expect(ruleOf(card, '.card.selected')).toContain('border-top-color: var(--cursor-border)');
    expect(ruleOf(card, '.card.selected')).toContain('border-right-color: var(--cursor-border)');
    expect(ruleOf(card, '.card.selected')).toContain('border-bottom-color: var(--cursor-border)');
    expect(ruleOf(card, '.card.selected'), 'the type edge keeps its colour when selected').not.toContain('border-left-color');
  });

  it('lifts its ground one step on hover, short of the selected card, and leaves its border and a tint alone', () => {
    const hover = ruleOf(card, '.card:not([data-lamp]):not(.selected):hover');
    expect(hover).toContain('background: var(--card-hover)');
    expect(hover).not.toContain('border');
  });

  it('looks the same whatever its column: no state class dims, dashes or flattens it', () => {
    expect(card).not.toMatch(/\.done|dashed|transparent/);
  });
});
