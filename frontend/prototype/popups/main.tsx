// PROTOTYPE: throwaway switcher for the popups variants, over the chosen window (ux-system R4). Not shipped.
import { StrictMode, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import '../../src/design-system/global.css';
import '../ux-system/switcher.css';
import { VariantR4 } from '../ux-system/VariantLayouts';
import { VariantP0 } from './VariantP0';

export const POPUPS = ['capture', 'capture-creating', 'capture-refused', 'quit', 'comment', 'comment-refused', 'reply', 'submit'] as const;
export type PopupKind = (typeof POPUPS)[number];
export interface PopupProps {
  popup: PopupKind;
}

const VARIANTS = [{ key: 'P0', name: 'Popups: today', Component: VariantP0 }];

const ON_REVIEW = new Set<PopupKind>(['comment', 'comment-refused', 'reply', 'submit']);

const params = new URLSearchParams(location.search);

const Prototype = () => {
  const [index, setIndex] = useState(() => Math.max(0, VARIANTS.findIndex((v) => v.key === params.get('variant'))));
  const [popup, setPopup] = useState<PopupKind>(() => POPUPS.find((p) => p === params.get('popup')) ?? 'capture');
  const variant = VARIANTS[index]!;
  useEffect(() => {
    const url = new URL(location.href);
    url.searchParams.set('variant', variant.key);
    url.searchParams.set('popup', popup);
    history.replaceState(null, '', url);
  }, [variant.key, popup]);
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.code === 'ArrowRight') setIndex((i) => (i + 1) % VARIANTS.length);
      else if (event.code === 'ArrowLeft') setIndex((i) => (i + VARIANTS.length - 1) % VARIANTS.length);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);
  const { Component } = variant;
  const review = ON_REVIEW.has(popup);
  return (
    <>
      <VariantR4 focus={review ? 'ticket' : 'board'} documentShown={false} prView={review ? 'review' : 'off'} onPrView={() => undefined} />
      <Component popup={popup} />
      {params.get('bare') === null ? (
        <div className="proto-switcher">
          <span className="proto-label">{variant.key} · {variant.name}</span>
          <span className="proto-sep" />
          {POPUPS.map((p) => (
            <button key={p} className={p === popup ? 'proto-on' : ''} onClick={() => setPopup(p)}>{p}</button>
          ))}
        </div>
      ) : null}
    </>
  );
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Prototype />
  </StrictMode>,
);
