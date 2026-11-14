import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ruleOf, stylesheet } from './stylesheet';
import { StatusColour, StatusHeading } from './StatusHeading';

const sheet = stylesheet('StatusHeading.module.css');

describe('a status heading', () => {
  it.each([
    [StatusColour.InProgress, 'status-in-progress'],
    [StatusColour.InReview, 'status-in-review'],
    [StatusColour.Todo, 'status-todo'],
    [StatusColour.Draft, 'status-draft'],
    [StatusColour.Done, 'status-done'],
  ])('carries the %s colour from its token', (status, token) => {
    render(<StatusHeading status="the status" count={1} colour={status} />);
    expect(screen.getByText('the status').parentElement).toHaveAttribute('data-status', status);
    expect(ruleOf(sheet, `.heading[data-status='${status}']`)).toContain(`--status-ink: var(--${token})`);
  });

  it('is underlined in its own status colour, not in the seam between panes', () => {
    expect(ruleOf(sheet, '.heading'), 'the prototype underlines each heading in its status colour at half strength').toContain(
      'border-bottom: 2px solid color-mix(in srgb, var(--status-ink) 50%, transparent)',
    );
    expect(ruleOf(sheet, '.heading')).toContain('color: var(--status-ink)');
  });

  it('stays dim when its heading means no status', () => {
    render(<StatusHeading status="unreadable" count={1} />);
    expect(screen.getByText('unreadable').parentElement).not.toHaveAttribute('data-status');
  });
});
