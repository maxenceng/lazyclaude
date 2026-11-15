import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { loadMermaid } from './mermaidLoader';
import { Markdown } from './Markdown';

vi.mock('./mermaidLoader', () => ({ loadMermaid: vi.fn() }));

const mermaid = { initialize: vi.fn(), parse: vi.fn(), render: vi.fn() };
const SVG = '<svg viewBox="0 0 400 100"><text>drawn</text></svg>';
const block = (language: string, text: string) => `\`\`\`${language}\n${text}\n\`\`\``;

const read = (source: string) => render(<Markdown source={source} onLink={() => {}} />);

const fonts = { load: vi.fn(), ready: Promise.resolve() };
Object.defineProperty(document, 'fonts', { configurable: true, value: fonts });

beforeEach(() => {
  vi.resetAllMocks();
  fonts.load.mockResolvedValue([]);
  vi.mocked(loadMermaid).mockResolvedValue({ default: mermaid } as unknown as Awaited<ReturnType<typeof loadMermaid>>);
  mermaid.parse.mockResolvedValue(true);
  mermaid.render.mockResolvedValue({ svg: SVG });
});

describe('a mermaid block', () => {
  it('shows its source as a code block until the drawing is ready', async () => {
    read(block('mermaid', 'graph TD\n a --> b'));
    expect(screen.getByText(/graph TD/).closest('pre')).not.toBeNull();
    await waitFor(() => expect(screen.getByText('drawn')).toBeInTheDocument());
  });

  it('is replaced by its drawing once ready', async () => {
    const { container } = read(block('mermaid', 'graph TD\n a --> b'));
    await waitFor(() => expect(container.querySelector('svg')).not.toBeNull());
    expect(container.querySelector('pre')).toBeNull();
    expect(mermaid.render).toHaveBeenCalledWith(expect.stringMatching(/^m\w+$/), 'graph TD\n a --> b');
  });
});

describe('a fenced block that is not mermaid', () => {
  it.each(['java', ''])('stays a plain code block for language "%s" and never loads mermaid', (language) => {
    read(block(language, 'int x;'));
    expect(screen.getByText('int x;').closest('pre')).not.toBeNull();
    expect(loadMermaid).not.toHaveBeenCalled();
  });

  it('never loads mermaid for a document without a diagram', () => {
    read('# Title\n\nText\n');
    expect(loadMermaid).not.toHaveBeenCalled();
  });
});

describe('a mermaid block that does not parse', () => {
  it('keeps its source as a code block under one line giving Mermaid\'s message', async () => {
    mermaid.parse.mockRejectedValue(new Error('Parse error on line 2\nsecond\nthird'));
    read(block('mermaid', 'graph TD\n shell[x'));
    const note = await screen.findByRole('status');
    expect(note).toHaveTextContent('Diagram not drawn Parse error on line 2 second');
    expect(note).not.toHaveTextContent('third');
    expect(screen.getByText(/shell\[x/).closest('pre')).not.toBeNull();
  });

  it('leaves the rest of the document alone and still draws another block that parses', async () => {
    mermaid.parse.mockImplementation((text: string) => (text.includes('bad') ? Promise.reject(new Error('nope')) : Promise.resolve(true)));
    const { container } = read(`Before\n\n${block('mermaid', 'bad')}\n\n${block('mermaid', 'good')}\n\nAfter`);
    await waitFor(() => expect(container.querySelector('svg')).not.toBeNull());
    expect(await screen.findByRole('status')).toHaveTextContent('nope');
    expect(screen.getByText('Before')).toBeInTheDocument();
    expect(screen.getByText('After')).toBeInTheDocument();
    expect(container.querySelectorAll('svg')).toHaveLength(1);
  });

  it('says it did not draw when render itself fails', async () => {
    mermaid.render.mockRejectedValue(new Error('render broke'));
    read(block('mermaid', 'graph TD'));
    expect(await screen.findByRole('status')).toHaveTextContent('Diagram not drawn render broke');
  });
});

describe('how mermaid is set up', () => {
  it('renders at strict security, so a click directive or an HTML label runs nothing', async () => {
    read(block('mermaid', 'graph TD'));
    await waitFor(() => expect(mermaid.render).toHaveBeenCalled());
    expect(mermaid.initialize).toHaveBeenCalledWith(expect.objectContaining({ securityLevel: 'strict', startOnLoad: false }));
  });

  it('reads its theme from the design tokens', async () => {
    document.documentElement.style.setProperty('--pane-lift', '#112233');
    document.documentElement.style.setProperty('--seam', '#445566');
    document.documentElement.style.setProperty('--dim', '#778899');
    document.documentElement.style.setProperty('--font-read', 'Barlow, sans-serif');
    read(block('mermaid', 'graph TD'));
    await waitFor(() => expect(mermaid.initialize).toHaveBeenCalled());
    const config = mermaid.initialize.mock.calls[0]?.[0] as { theme: string; themeVariables: Record<string, string> };
    expect(config.theme).toBe('base');
    expect(config.themeVariables).toMatchObject({ primaryColor: '#112233', primaryBorderColor: '#445566', lineColor: '#778899', fontFamily: 'Barlow, sans-serif' });
  });

  it('draws only after the regular, semibold and italic reading faces are loaded', async () => {
    let loaded!: () => void;
    fonts.load.mockReturnValue(
      new Promise<unknown[]>((resolve) => {
        loaded = () => resolve([]);
      }),
    );
    read(block('mermaid', 'graph TD'));
    await waitFor(() => expect(fonts.load).toHaveBeenCalledTimes(3));
    expect(fonts.load.mock.calls.map((call: unknown[]) => call[0])).toEqual([
      expect.stringMatching(/^400 \d+px Barlow/),
      expect.stringMatching(/^600 \d+px Barlow/),
      expect.stringMatching(/^italic 400 \d+px Barlow/),
    ]);
    expect(mermaid.render).not.toHaveBeenCalled();
    loaded();
    await waitFor(() => expect(mermaid.render).toHaveBeenCalled());
  });
});
