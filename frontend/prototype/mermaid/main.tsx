// PROTOTYPE: throwaway switcher for how a mermaid block is drawn in a document. Not shipped.
import { StrictMode, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import '../../src/design-system/global.css';
import '../ux-system/switcher.css';
import './mermaid.css';
import { Pane, PaneLayout } from '../../src/design-system';
import { Markdown } from '../../src/design-system/Markdown';
import { AFTER, BEFORE, DIAGRAMS, type DiagramKind } from './data';
import { Diagram, Look } from './Diagram';

const VARIANTS = [
  { key: 'R0', name: 'Today: the source', look: undefined },
  { key: 'M1', name: 'Sunk panel, like code', look: Look.Sunk },
  { key: 'M2', name: 'On the page, card nodes', look: Look.Bare },
  { key: 'M3', name: 'Line drawing between rules', look: Look.Line },
] as const;

const params = new URLSearchParams(location.search);
const kind = (Object.keys(DIAGRAMS).find((k) => k === params.get('diagram')) ?? 'graph') as DiagramKind;
const width = params.get('width') === 'half' ? 640 : 1180;
const noLink = () => undefined;

const Prototype = () => {
  const [index, setIndex] = useState(() => Math.max(0, VARIANTS.findIndex((v) => v.key === params.get('variant'))));
  const variant = VARIANTS[index]!;
  useEffect(() => {
    const url = new URL(location.href);
    url.searchParams.set('variant', variant.key);
    history.replaceState(null, '', url);
  }, [variant.key]);
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.code === 'ArrowRight') setIndex((i) => (i + 1) % VARIANTS.length);
      else if (event.code === 'ArrowLeft') setIndex((i) => (i + VARIANTS.length - 1) % VARIANTS.length);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);
  const source = DIAGRAMS[kind];
  return (
    <>
      <div className="proto-stage">
        <div style={{ width }}>
          <Pane title="docs/context-map.md" focused layout={PaneLayout.Flow}>
            <div style={{ containerType: 'inline-size', padding: 'var(--space-3) 26px var(--space-5)' }}>
              {variant.look === undefined ? (
                <Markdown source={`${BEFORE}\n\`\`\`mermaid\n${source}\n\`\`\`\n\n${AFTER}`} onLink={noLink} />
              ) : (
                <>
                  <Markdown source={BEFORE} onLink={noLink} />
                  <div style={{ display: 'grid', gridTemplateColumns: 'var(--reading-columns)', margin: 'var(--space-3) 0', fontSize: 'var(--reading-size)' }}>
                    <Diagram key={variant.key} source={source} look={variant.look} />
                  </div>
                  <Markdown source={AFTER} onLink={noLink} />
                </>
              )}
            </div>
          </Pane>
        </div>
      </div>
      {!params.has('shot') && (
        <div className="proto-switcher">
          {VARIANTS.map((v, i) => (
            <button key={v.key} type="button" className={i === index ? 'proto-on' : ''} onClick={() => setIndex(i)}>
              {v.key}
            </button>
          ))}
          <span>{variant.name}</span>
        </div>
      )}
    </>
  );
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Prototype />
  </StrictMode>,
);
