import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FactsLine } from './FactsLine';
import { Tone } from './Text';

describe('a facts line', () => {
  it('sets each value under its label', () => {
    render(<FactsLine facts={[{ label: 'status', value: 'todo' }, { label: 'worktree', value: 'none', tone: Tone.Dim }]} />);
    expect(screen.getByText('status')).toBeInTheDocument();
    expect(screen.getByText('todo')).toBeInTheDocument();
    expect(screen.getByText('none')).toBeInTheDocument();
  });
});
