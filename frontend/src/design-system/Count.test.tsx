import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Count } from './Count';
import { LampColour } from './Lamp';

describe('a lamp count', () => {
  it.each([
    [LampColour.Permission, 0, '0 permission'],
    [LampColour.Input, 1, '1 input'],
    [LampColour.Turn, 1, '1 your turn'],
  ])('words %s as what it counts', (colour, count, words) => {
    const { container } = render(<Count colour={colour} count={count} />);
    expect(container).toHaveTextContent(words);
    expect(screen.getByText(String(count)).tagName).toBe('B');
  });
});
