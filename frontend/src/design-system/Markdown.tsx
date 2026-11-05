import { Fragment, type ReactNode, useEffect, useMemo, useRef } from 'react';
import styles from './Markdown.module.css';
import { type Block, BlockKind, type Inline, InlineKind, blocksOf, inlineOf } from './markdown';

/** Where a document was asked to open: at a heading's slug, or at its top; `opened` changes with each ask, so asking again scrolls again. */
export interface Target {
  fragment: string | undefined;
  opened: number;
}

interface Props {
  source: string;
  onLink: (href: string) => void;
  target?: Target;
}

const HEADINGS = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'] as const;

const runs = (inlines: Inline[], onLink: (href: string) => void): ReactNode => {
  return inlines.map((inline, index) => {
    switch (inline.kind) {
      case InlineKind.Text:
        return <Fragment key={index}>{inline.text}</Fragment>;
      case InlineKind.Code:
        return <code key={index}>{inline.text}</code>;
      case InlineKind.Strong:
        return <strong key={index}>{runs(inline.children, onLink)}</strong>;
      case InlineKind.Emphasis:
        return <em key={index}>{runs(inline.children, onLink)}</em>;
      case InlineKind.Link:
        return (
          <a
            key={index}
            href={inline.href}
            onClick={(event) => {
              event.preventDefault();
              onLink(inline.href);
            }}
            onAuxClick={(event) => event.preventDefault()}
          >
            {runs(inline.children, onLink)}
          </a>
        );
    }
  });
};

const text = (source: string, onLink: (href: string) => void): ReactNode => runs(inlineOf(source), onLink);

const blockElement = (block: Block, onLink: (href: string) => void): ReactNode => {
  switch (block.kind) {
    case BlockKind.Heading: {
      const Level = HEADINGS[block.level - 1] ?? 'h6';
      return <Level id={block.slug}>{text(block.text, onLink)}</Level>;
    }
    case BlockKind.Paragraph:
      return <p>{text(block.text, onLink)}</p>;
    case BlockKind.Code:
      return (
        <pre>
          <code>{block.text}</code>
        </pre>
      );
    case BlockKind.List: {
      const items = block.items.map((item, index) => <li key={index}>{text(item, onLink)}</li>);
      return block.ordered ? <ol>{items}</ol> : <ul>{items}</ul>;
    }
    case BlockKind.Quote:
      return <blockquote>{text(block.text, onLink)}</blockquote>;
    case BlockKind.Rule:
      return <hr />;
    case BlockKind.Table:
      return (
        <table>
          <thead>
            <tr>
              {block.header.map((cell, index) => (
                <th key={index}>{text(cell, onLink)}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {block.rows.map((row, index) => (
              <tr key={index}>
                {row.map((cell, at) => (
                  <td key={at}>{text(cell, onLink)}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      );
  }
};

/**
 * Markdown typeset in the design system's reading type: the one primitive that sets a ticket's body and a document. Raw HTML
 * is never read as HTML, and a link never navigates: it is handed to `onLink`, and a heading carries its slug as its id so a
 * fragment can find it.
 */
export const Markdown = ({ source, onLink, target }: Props) => {
  const blocks = useMemo(() => blocksOf(source), [source]);
  const article = useRef<HTMLElement>(null);
  const targeted = target !== undefined;
  const opened = target?.opened;
  const fragment = target?.fragment;
  useEffect(() => {
    if (!targeted || article.current === null) {
      return;
    }
    const heading = [...article.current.querySelectorAll('h1, h2, h3, h4, h5, h6')].find((candidate) => candidate.id === fragment);
    (heading ?? article.current).scrollIntoView({ block: 'start' });
  }, [targeted, opened, fragment, source]);
  return (
    <article ref={article} className={styles.markdown}>
      {blocks.map((block, index) => (
        <Fragment key={index}>{blockElement(block, onLink)}</Fragment>
      ))}
    </article>
  );
};
