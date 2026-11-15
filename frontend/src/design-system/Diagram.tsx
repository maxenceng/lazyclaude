import { useEffect, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './Markdown.module.css';
import { loadMermaid } from './mermaidLoader';
import { readingFaces, themeOf } from './mermaidTheme';

interface Props {
  source: string;
}

type Drawn = { source: string; svg: string; width: number | undefined } | { source: string; failure: string };

const MESSAGE_LINES = 2;
const NOT_ID = /\W/g;
const PIXELS = /^\s*([\d.]+)(?:px)?\s*$/;
const VIEW_BOX_SEPARATOR = /[\s,]+/;
const MAX_WIDTH = /max-width:\s*([^;]+)/;
const VIEW_BOX_WIDTH = 2;

const pixelsOf = (value: string | null | undefined): number | undefined => {
  const width = Number(PIXELS.exec(value ?? '')?.[1]);
  return Number.isFinite(width) && width > 0 ? width : undefined;
};

/** The drawing's natural width, read off its root `<svg>`: its view box, else a pixel `width`, else the `max-width` mermaid sets; none when it names none. */
const widthOf = (svg: string): number | undefined => {
  const root = new DOMParser().parseFromString(svg, 'image/svg+xml').documentElement;
  const box = root.getAttribute('viewBox')?.trim().split(VIEW_BOX_SEPARATOR)[VIEW_BOX_WIDTH];
  return pixelsOf(box) ?? pixelsOf(root.getAttribute('width')) ?? pixelsOf(MAX_WIDTH.exec(root.getAttribute('style') ?? '')?.[1]);
};

const messageOf = (error: unknown): string => {
  return error instanceof Error ? error.message.split('\n').slice(0, MESSAGE_LINES).join(' ') : String(error);
};

const draw = async (source: string, id: string): Promise<Drawn> => {
  try {
    await Promise.all(readingFaces().map((face) => document.fonts.load(face)));
    await document.fonts.ready;
    const { default: mermaid } = await loadMermaid();
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'strict',
      suppressErrorRendering: true,
      look: 'classic',
      layout: 'elk',
      theme: 'base',
      themeVariables: themeOf(),
      flowchart: { useMaxWidth: false, minNodeWidth: 0 },
      sequence: { useMaxWidth: false },
    });
    await mermaid.parse(source);
    const { svg } = await mermaid.render(id, source);
    return { source, svg, width: widthOf(svg) };
  } catch (error) {
    return { source, failure: messageOf(error) };
  }
};

/**
 * A mermaid block: its source as a code block until mermaid.js, imported on demand, has drawn it, then the drawing on the same
 * sunk panel; a block that is not drawn, because its source does not parse or mermaid.js could not be loaded, keeps the code
 * block under one line saying why. Mermaid runs at `strict` security, since a document's markdown is not trusted input.
 */
export const Diagram = ({ source }: Props) => {
  const { t } = useTranslation('designSystem');
  const id = `m${useId().replace(NOT_ID, '')}`;
  const [drawn, setDrawn] = useState<Drawn>();
  useEffect(() => {
    let live = true;
    void draw(source, id).then((result) => {
      if (live) {
        setDrawn(result);
      }
    });
    return () => {
      live = false;
    };
  }, [source, id]);
  const current = drawn?.source === source ? drawn : undefined;
  if (current !== undefined && 'svg' in current) {
    const natural = current.width === undefined ? {} : { ['--natural' as string]: `${current.width}px` };
    return (
      <div className={styles.diagram} style={natural}>
        <div className={styles.diagramSvg} dangerouslySetInnerHTML={{ __html: current.svg }} />
      </div>
    );
  }
  return (
    <div className={styles.diagramFallback}>
      {current !== undefined && (
        <p className={styles.diagramFailure} role="status">
          <span className={styles.diagramFailureHead}>{t('diagram.notDrawn')}</span> {current.failure}
        </p>
      )}
      <pre>
        <code>{source}</code>
      </pre>
    </div>
  );
};
