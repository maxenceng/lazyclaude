// PROTOTYPE: throwaway switcher for the window-wide visual system variants. Not shipped.
import { StrictMode, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import '../../src/design-system/global.css';
import { PANES, type Pane } from './data';
import './switcher.css';
import { VariantA } from './VariantA';
import { VariantB } from './VariantB';
import { VariantC } from './VariantC';
import { VariantD } from './VariantD';
import { VariantE, VariantF, VariantG, VariantH, VariantI, VariantJ, VariantK } from './VariantLayouts';

export interface VariantProps {
  focus: Pane;
  documentShown: boolean;
  boardTab?: boolean;
  onBoardTab?: (on: boolean) => void;
}

const VARIANTS = [
  { key: 'A', name: 'Lit title', Component: VariantA },
  { key: 'B', name: 'Framed panes', Component: VariantB },
  { key: 'C', name: 'Terminal boxes', Component: VariantC },
  { key: 'D', name: 'Chosen', Component: VariantD },
  { key: 'E', name: 'Wider, two-line rows', Component: VariantE },
  { key: 'F', name: 'Board and Docs as tabs', Component: VariantF },
  { key: 'G', name: 'Docs in a rail', Component: VariantG },
  { key: 'H', name: 'Chosen layout', Component: VariantH },
  { key: 'I', name: 'Board tab: side preview', Component: VariantI },
  { key: 'J', name: 'Board tab: preview on demand', Component: VariantJ },
  { key: 'K', name: 'Board tab: compact, folded', Component: VariantK },
];

const HAS_BOARD_TAB = new Set(['I', 'J', 'K']);

const DEFAULT = VARIANTS.findIndex((v) => v.key === 'I');

const initial = () => {
  const wanted = new URLSearchParams(location.search).get('variant');
  const found = VARIANTS.findIndex((v) => v.key === wanted);
  return found === -1 ? DEFAULT : found;
};

const DIGIT_PANE: Record<string, Pane> = { Digit1: 'board', Digit2: 'docs', Digit3: 'ticket', Digit4: 'dock' };

const Prototype = () => {
  const [index, setIndex] = useState(initial);
  const [focus, setFocus] = useState<Pane>(() => PANES.find((p) => p === new URLSearchParams(location.search).get('focus')) ?? 'board');
  const [documentShown, setDocumentShown] = useState(() => new URLSearchParams(location.search).get('doc') === 'open');
  const [boardTab, setBoardTab] = useState(() => new URLSearchParams(location.search).get('tab') !== 'base');
  const variant = VARIANTS[index] ?? VARIANTS[DEFAULT]!;

  useEffect(() => {
    const url = new URL(location.href);
    url.searchParams.set('variant', variant.key);
    history.replaceState(null, '', url);
  }, [variant.key]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.code === 'ArrowRight') setIndex((i) => (i + 1) % VARIANTS.length);
      else if (event.code === 'ArrowLeft') setIndex((i) => (i + VARIANTS.length - 1) % VARIANTS.length);
      else if (event.code === 'Tab') {
        event.preventDefault();
        setFocus((f) => PANES[(PANES.indexOf(f) + 1) % PANES.length]!);
      } else if (event.code === 'KeyD') setDocumentShown((d) => !d);
      else if (DIGIT_PANE[event.code] !== undefined) setFocus(DIGIT_PANE[event.code]!);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const { Component } = variant;
  return (
    <>
      <Component focus={focus} documentShown={documentShown} boardTab={HAS_BOARD_TAB.has(variant.key) && boardTab} onBoardTab={setBoardTab} />
      <div className="proto-switcher">
        <button onClick={() => setIndex((i) => (i + VARIANTS.length - 1) % VARIANTS.length)}>←</button>
        <span className="proto-label">
          {variant.key} · {variant.name}
        </span>
        <button onClick={() => setIndex((i) => (i + 1) % VARIANTS.length)}>→</button>
        <span className="proto-sep" />
        {PANES.map((pane) => (
          <button key={pane} className={pane === focus ? 'proto-on' : ''} onClick={() => setFocus(pane)}>
            {pane}
          </button>
        ))}
        <span className="proto-sep" />
        <button className={documentShown ? 'proto-on' : ''} onClick={() => setDocumentShown((d) => !d)}>
          doc shown
        </button>
        {HAS_BOARD_TAB.has(variant.key) && (
          <button className={boardTab ? 'proto-on' : ''} onClick={() => setBoardTab((b) => !b)}>
            board tab
          </button>
        )}
        <span className="proto-hint">←/→ variant · Tab focus · d doc</span>
      </div>
    </>
  );
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Prototype />
  </StrictMode>,
);
