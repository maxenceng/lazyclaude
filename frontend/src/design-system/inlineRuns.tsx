import { Fragment, type ReactNode } from 'react';
import { type Inline, InlineKind } from './markdown';

/** Inline runs as elements: code, strong, emphasis, and a link handed to `onLink`, or read as its words when there is none to follow it. */
export const runs = (inlines: Inline[], onLink: ((href: string) => void) | undefined): ReactNode => {
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
        if (onLink === undefined) {
          return <Fragment key={index}>{runs(inline.children, onLink)}</Fragment>;
        }
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

