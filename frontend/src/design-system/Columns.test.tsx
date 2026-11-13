import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Column, Columns } from './Columns';
import { ruleOf, stylesheet } from './stylesheet';

const sheet = stylesheet('Columns.module.css');

describe('columns', () => {
  it('share the width equally, whatever each holds', () => {
    expect(ruleOf(sheet, '.columns')).toContain('grid-auto-columns: minmax(0, 1fr)');
    expect(ruleOf(sheet, '.columns'), 'a column that holds long text must not widen').not.toContain('overflow-x');
  });

  it('say none in a column that holds nothing, and nothing in one that does', () => {
    render(
      <Columns>
        <Column label="empty" empty="none" />
        <Column label="full" empty="none">
          <p>a card</p>
        </Column>
      </Columns>,
    );
    expect(screen.getByRole('group', { name: 'empty' })).toHaveTextContent('none');
    expect(screen.getByRole('group', { name: 'full' })).not.toHaveTextContent('none');
  });
});
