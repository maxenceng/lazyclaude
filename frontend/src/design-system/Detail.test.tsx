import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Detail } from './Detail';

describe('the detail of what is selected', () => {
  it('typesets the body as markdown, the way a document is', () => {
    render(<Detail facts={['PROJ-1']} title="A ticket" body={'## Why\n\nBecause **it matters**.'} onLink={() => {}} />);
    expect(screen.getByRole('heading', { level: 2, name: 'Why' })).toBeInTheDocument();
    expect(screen.getByText('it matters').tagName).toBe('STRONG');
  });

  it('hands a link in the body to the page', async () => {
    const onLink = vi.fn();
    render(<Detail facts={[]} title="A ticket" body="See [the docs](https://example.com)." onLink={onLink} />);
    await userEvent.click(screen.getByRole('link', { name: 'the docs' }));
    expect(onLink).toHaveBeenCalledWith('https://example.com');
  });

  it('shows no body when the ticket has none', () => {
    render(<Detail facts={[]} title="A ticket" body={undefined} onLink={() => {}} />);
    expect(screen.getAllByRole('article')).toHaveLength(1);
    expect(screen.getByRole('heading', { name: 'A ticket' })).toBeInTheDocument();
  });

  it('sets the summary and the lead between the title and the body', () => {
    render(<Detail facts={[]} title="A ticket" summary={<p>where it stands</p>} lead={<p>what to do</p>} body="The text." onLink={() => {}} />);
    const text = screen.getAllByRole('article')[0]?.textContent ?? '';
    expect(text.indexOf('A ticket')).toBeLessThan(text.indexOf('where it stands'));
    expect(text.indexOf('where it stands')).toBeLessThan(text.indexOf('what to do'));
    expect(text.indexOf('what to do')).toBeLessThan(text.indexOf('The text.'));
  });
});
