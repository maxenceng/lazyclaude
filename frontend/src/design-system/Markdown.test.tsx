import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Markdown } from './Markdown';

const SOURCE = '# Title\n\nSome **bold** and `code` text with a [link](other.md#part).\n\n## Part\n\n- one\n- two\n\n| a | b |\n| - | - |\n| 1 | 2 |\n\n```\nlet x = 1\n```\n';

afterEach(() => {
  Element.prototype.scrollIntoView = () => {};
});

describe('a typeset document', () => {
  it('sets headings, lists, tables and code as their elements, each heading carrying its slug', () => {
    render(<Markdown source={SOURCE} onLink={() => {}} />);
    expect(screen.getByRole('heading', { level: 1, name: 'Title' })).toHaveAttribute('id', 'title');
    expect(screen.getByRole('heading', { level: 2, name: 'Part' })).toHaveAttribute('id', 'part');
    expect(screen.getAllByRole('listitem').map((item) => item.textContent)).toEqual(['one', 'two']);
    expect(screen.getByRole('table')).toHaveTextContent('12');
    expect(screen.getByText('let x = 1')).toBeInTheDocument();
    expect(screen.getByText('bold').tagName).toBe('STRONG');
  });

  it('hands a clicked link to the page and never navigates', async () => {
    const onLink = vi.fn();
    render(<Markdown source={SOURCE} onLink={onLink} />);
    const click = new MouseEvent('click', { bubbles: true, cancelable: true });
    screen.getByRole('link', { name: 'link' }).dispatchEvent(click);
    expect(click.defaultPrevented).toBe(true);
    expect(onLink).toHaveBeenCalledWith('other.md#part');
    await userEvent.click(screen.getByRole('link', { name: 'link' }));
    expect(onLink).toHaveBeenCalledTimes(2);
  });

  it('scrolls to the heading a target names', () => {
    const scroll = vi.fn();
    Element.prototype.scrollIntoView = scroll;
    render(<Markdown source={SOURCE} onLink={() => {}} target={{ fragment: 'part', opened: 1 }} />);
    expect(scroll).toHaveBeenCalledTimes(1);
    expect((scroll.mock.contexts[0] as HTMLElement).id).toBe('part');
  });

  it('scrolls to the top of the document when the target names no heading', () => {
    const scroll = vi.fn();
    Element.prototype.scrollIntoView = scroll;
    render(<Markdown source={SOURCE} onLink={() => {}} target={{ fragment: 'missing', opened: 1 }} />);
    expect(scroll).toHaveBeenCalledTimes(1);
    expect((scroll.mock.contexts[0] as HTMLElement).tagName).toBe('ARTICLE');
  });

  it('keeps its place when the same document is read again with new text', () => {
    const scroll = vi.fn();
    Element.prototype.scrollIntoView = scroll;
    const target = { fragment: 'part', opened: 1 };
    const { rerender } = render(<Markdown source={SOURCE} onLink={() => {}} target={target} />);
    rerender(<Markdown source={`${SOURCE}\nMore.\n`} onLink={() => {}} target={{ ...target }} />);
    expect(scroll).toHaveBeenCalledTimes(1);
  });

  it('scrolls nowhere without a target', () => {
    const scroll = vi.fn();
    Element.prototype.scrollIntoView = scroll;
    render(<Markdown source={SOURCE} onLink={() => {}} />);
    expect(scroll).not.toHaveBeenCalled();
  });
});
