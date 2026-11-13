import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Ci } from './CiGlyph';
import { ruleOf, stylesheet } from './stylesheet';
import { Cell, ColumnSize, Table, TableRow } from './Table';

const sheet = stylesheet('Table.module.css');
const heads = [
  { label: 'CI', size: ColumnSize.Fit },
  { label: 'Title', size: ColumnSize.Fill },
];

describe('a table', () => {
  it('names its columns and what it lists', () => {
    render(
      <Table label="Pull requests" heads={heads}>
        {null}
      </Table>,
    );
    expect(screen.getByRole('table', { name: 'Pull requests' })).toBeInTheDocument();
    expect(screen.getAllByRole('columnheader').map((head) => head.textContent)).toEqual(['CI', 'Title']);
  });

  it('marks the selected row as current and hands a press on a row to its owner', async () => {
    const onSelect = vi.fn();
    render(
      <Table label="Pull requests" heads={heads}>
        <TableRow stripe={Ci.Failed} selected onSelect={onSelect}>
          <Cell size={ColumnSize.Fit}>x</Cell>
          <Cell size={ColumnSize.Fill}>one</Cell>
        </TableRow>
        <TableRow stripe={undefined} selected={false} onSelect={() => {}}>
          <Cell size={ColumnSize.Fit}>y</Cell>
          <Cell size={ColumnSize.Fill}>two</Cell>
        </TableRow>
      </Table>,
    );
    const rows = screen.getAllByRole('row').slice(1);
    expect(rows[0]).toHaveAttribute('aria-current', 'true');
    expect(rows[1]).not.toHaveAttribute('aria-current');
    await userEvent.click(screen.getByText('one'));
    expect(onSelect).toHaveBeenCalled();
  });

  it('stripes a row by its CI result, and not at all without one', () => {
    render(
      <Table label="Pull requests" heads={heads}>
        <TableRow stripe={Ci.Passed} selected={false} onSelect={() => {}}>
          <Cell size={ColumnSize.Fit}>a</Cell>
          <Cell size={ColumnSize.Fill}>b</Cell>
        </TableRow>
        <TableRow stripe={undefined} selected={false} onSelect={() => {}}>
          <Cell size={ColumnSize.Fit}>c</Cell>
          <Cell size={ColumnSize.Fill}>d</Cell>
        </TableRow>
      </Table>,
    );
    const rows = screen.getAllByRole('row').slice(1);
    expect(rows.map((row) => row.getAttribute('data-stripe'))).toEqual(['passed', 'none']);
    expect(ruleOf(sheet, ".row[data-stripe='passed'] > td:first-child")).toContain('var(--ci-passed)');
    expect(ruleOf(sheet, '.selected'), 'a row with no checks stays readable on the cursor ground').toContain('--ci-ink-none: var(--cursor-ink)');
  });

  it('lifts the selected row on the cursor and gives the CI inks their on-light values there', () => {
    expect(ruleOf(sheet, '.selected')).toContain('background: var(--cursor-ground)');
    expect(ruleOf(sheet, '.selected')).toContain('--ci-ink-failed: var(--cursor-ci-failed, var(--ci-failed))');
  });
});
