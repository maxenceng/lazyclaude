// PROTOTYPE: throwaway switcher for the window-wide visual system variants. Not shipped.
import { StrictMode, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import '../../src/design-system/global.css';
import { PANES, type Pane } from './data';
import type { PrView } from './Review';
import './switcher.css';
import { VariantA } from './VariantA';
import { VariantB } from './VariantB';
import { VariantC } from './VariantC';
import { VariantD } from './VariantD';
import { VariantE, VariantF, VariantG, VariantH, VariantI, VariantJ, VariantK, VariantR0, VariantR1, VariantR2, VariantR3, VariantR4 } from './VariantLayouts';

export interface VariantProps {
  focus: Pane;
  documentShown: boolean;
  boardTab?: boolean;
  onBoardTab?: (on: boolean) => void;
  prView?: PrView;
  onPrView?: (view: PrView) => void;
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
  { key: 'R0', name: 'Review: today', Component: VariantR0 },
  { key: 'R1', name: 'Review: files rail, inline threads', Component: VariantR1 },
  { key: 'R2', name: 'Review: files | diff | threads', Component: VariantR2 },
  { key: 'R3', name: 'Review: tabs, margin notes', Component: VariantR3 },
  { key: 'R4', name: 'Review: chosen', Component: VariantR4 },
];

const HAS_PR_TAB = new Set(['R0', 'R1', 'R2', 'R3', 'R4']);

const HAS_BOARD_TAB = new Set(['I', 'J', 'K']);

const DEFAULT = VARIANTS.findIndex((v) => v.key === 'R4');

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
  const [prView, setPrView] = useState<PrView>(() => (new URLSearchParams(location.search).get('pr') === 'review' ? 'review' : new URLSearchParams(location.search).get('tab') === 'base' ? 'off' : 'list'));
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
      <Component focus={focus} documentShown={documentShown} boardTab={HAS_BOARD_TAB.has(variant.key) && boardTab}
        onBoardTab={setBoardTab}
        prView={HAS_PR_TAB.has(variant.key) ? prView : 'off'}
        onPrView={setPrView}
      />
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
        {HAS_PR_TAB.has(variant.key) && (
          <>
            <button className={prView === 'list' ? 'proto-on' : ''} onClick={() => setPrView('list')}>
              prs tab
            </button>
            <button className={prView === 'review' ? 'proto-on' : ''} onClick={() => setPrView('review')}>
              review
            </button>
          </>
        )}
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
