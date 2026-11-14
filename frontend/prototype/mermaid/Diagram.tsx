// PROTOTYPE: one mermaid block drawn by mermaid.js, themed from tokens.css; the variants differ only in look. Not shipped.
import { useEffect, useId, useState } from 'react';

export enum Look {
  Sunk = 'sunk',
  Bare = 'bare',
  Line = 'line',
}

type Drawn = { kind: 'source' } | { kind: 'svg'; svg: string; width: number } | { kind: 'error'; message: string };

const token = (name: string) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

const themeOf = (look: Look) => {
  const text = token('--text');
  const dim = token('--dim');
  const ground = look === Look.Sunk ? token('--pane-sunk') : token('--pane');
  const node = { [Look.Sunk]: token('--pane-lift'), [Look.Bare]: token('--card'), [Look.Line]: ground }[look];
  const edge = { [Look.Sunk]: token('--seam'), [Look.Bare]: token('--card-edge'), [Look.Line]: dim }[look];
  return {
    background: ground,
    primaryColor: node,
    primaryTextColor: text,
    primaryBorderColor: edge,
    secondaryColor: node,
    tertiaryColor: ground,
    lineColor: dim,
    textColor: text,
    mainBkg: node,
    nodeBorder: edge,
    clusterBkg: ground,
    edgeLabelBackground: ground,
    actorBkg: node,
    actorBorder: edge,
    actorTextColor: text,
    actorLineColor: token('--seam'),
    signalColor: dim,
    signalTextColor: text,
    labelBoxBkgColor: node,
    labelTextColor: text,
    noteBkgColor: token('--pane-lift'),
    noteTextColor: text,
    noteBorderColor: edge,
    fontFamily: token('--font-read'),
    fontSize: '15px',
  };
};

export const Diagram = ({ source, look }: { source: string; look: Look }) => {
  const id = useId().replace(/:/g, '');
  const [drawn, setDrawn] = useState<Drawn>({ kind: 'source' });
  useEffect(() => {
    let live = true;
    void (async () => {
      await document.fonts.ready;
      const { default: mermaid } = await import('mermaid');
      mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: 'base', themeVariables: themeOf(look), flowchart: { useMaxWidth: false }, sequence: { useMaxWidth: false } });
      try {
        await mermaid.parse(source);
        const { svg } = await mermaid.render(`m${id}${look}`, source);
        const width = Number(/viewBox="[\d.-]+ [\d.-]+ ([\d.]+)/.exec(svg)?.[1] ?? 600);
        if (live) setDrawn({ kind: 'svg', svg, width });
      } catch (error) {
        if (live) setDrawn({ kind: 'error', message: error instanceof Error ? error.message.split('\n').slice(0, 2).join(' ') : String(error) });
      }
    })();
    return () => {
      live = false;
    };
  }, [source, look, id]);
  if (drawn.kind === 'svg') {
    return (
      <div className={`diagram diagram-${look}`} style={{ ['--natural' as string]: `${drawn.width}px` }}>
        <div className="diagram-svg" dangerouslySetInnerHTML={{ __html: drawn.svg }} />
      </div>
    );
  }
  return (
    <div className="diagram-fallback">
      {drawn.kind === 'error' && (
        <p className="diagram-error" role="status">
          <span className="diagram-error-head">Diagram not drawn</span> {drawn.message}
        </p>
      )}
      <pre>
        <code>{source}</code>
      </pre>
    </div>
  );
};
