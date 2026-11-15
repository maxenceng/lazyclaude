import { useEffect, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './Markdown.module.css';
import { loadMermaid } from './mermaidLoader';
import { readingFaces, themeOf } from './mermaidTheme';

interface Props {
  source: string;
}

type Drawn = { source: string; svg: string; width: number } | { source: string; failure: string };

const VIEW_BOX_WIDTH = /viewBox="[\d.-]+ [\d.-]+ ([\d.]+)/;
const MESSAGE_LINES = 2;
const FALLBACK_WIDTH = 600;
const NOT_ID = /\W/g;

const widthOf = (svg: string): number => Number(VIEW_BOX_WIDTH.exec(svg)?.[1] ?? FALLBACK_WIDTH);

const messageOf = (error: unknown): string => {
  return error instanceof Error ? error.message.split('\n').slice(0, MESSAGE_LINES).join(' ') : String(error);
};

const draw = async (source: string, id: string): Promise<Drawn> => {
  await Promise.all(readingFaces().map((face) => document.fonts.load(face)));
  await document.fonts.ready;
  const { default: mermaid } = await loadMermaid();
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'strict',
    suppressErrorRendering: true,
    theme: 'base',
    themeVariables: themeOf(),
    flowchart: { useMaxWidth: false },
    sequence: { useMaxWidth: false },
  });
  try {
    await mermaid.parse(source);
    const { svg } = await mermaid.render(id, source);
    return { source, svg, width: widthOf(svg) };
  } catch (error) {
    return { source, failure: messageOf(error) };
  }
};

/**
 * A mermaid block: its source as a code block until mermaid.js, imported on demand, has drawn it, then the drawing on the same
 * sunk panel; a block that does not parse keeps the code block under one line saying why. Mermaid runs at `strict` security,
 * since a document's markdown is not trusted input.
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
    return (
      <div className={styles.diagram} style={{ ['--natural' as string]: `${current.width}px` }}>
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
