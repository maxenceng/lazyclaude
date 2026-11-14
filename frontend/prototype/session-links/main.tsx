// PROTOTYPE: throwaway switcher for where a session link's hover hint sits. Not shipped.
import { StrictMode, useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import '@xterm/xterm/css/xterm.css';
import '../../src/design-system/global.css';
import '../ux-system/switcher.css';
import './links.css';
import { KeyBar, terminalLook } from '../../src/design-system';
import { createXterm } from '../../src/session/xterm';
import { LINES, OSC8_AT, PLAIN_AT } from './data';

interface Hint {
  url: string;
  x: number;
  y: number;
}

const VARIANTS = [
  { key: 'L0', name: 'Today: links are plain text' },
  { key: 'L1', name: 'Tooltip over the link' },
  { key: 'L2', name: 'Strip at the pane foot' },
  { key: 'L3', name: 'Entry in the key bar' },
] as const;

const BASE_KEYS = ['alt+h/l tabs', 'alt+z zoom', 'alt+w close', 'alt+m board'];
const URL_RE = /https?:\/\/[^\s)]+/g;
const params = new URLSearchParams(location.search);

const Pane = ({ variant }: { variant: string }) => {
  const host = useRef<HTMLDivElement>(null);
  const [hint, setHint] = useState<Hint | undefined>(undefined);
  const live = variant !== 'L0';
  useEffect(() => {
    const element = host.current!;
    const { terminal, fit } = createXterm(terminalLook(element));
    const show = (event: MouseEvent, url: string) => {
      const box = element.getBoundingClientRect();
      setHint({ url, x: event.clientX - box.left, y: event.clientY - box.top });
    };
    if (live) {
      terminal.options.linkHandler = {
        activate: () => undefined,
        hover: (event, url) => show(event, url),
        leave: () => setHint(undefined),
        allowNonHttpProtocols: false,
      };
      terminal.registerLinkProvider({
        provideLinks: (y, callback) => {
          const line = terminal.buffer.active.getLine(y - 1)?.translateToString(true) ?? '';
          const links = [...line.matchAll(URL_RE)].map((m) => ({
            text: m[0],
            range: { start: { x: m.index + 1, y }, end: { x: m.index + m[0].length, y } },
            activate: () => undefined,
            hover: (event: MouseEvent, text: string) => show(event, text),
            leave: () => setHint(undefined),
          }));
          callback(links.length ? links : undefined);
        },
      });
    }
    terminal.open(element);
    fit.fit();
    terminal.write(LINES.join('\r\n'));
    const cell = (at: { row: number; col: number }) => {
      const screen = element.querySelector('.xterm-screen')!.getBoundingClientRect();
      const w = screen.width / terminal.cols;
      const h = screen.height / terminal.rows;
      return { x: screen.left + (at.col + 0.5) * w, y: screen.top + (at.row + 0.5) * h };
    };
    Object.assign(window, { linkAt: (which: string) => cell(which === 'osc8' ? OSC8_AT : PLAIN_AT) });
    return () => terminal.dispose();
  }, [live]);
  const keys = variant === 'L3' && hint ? ['ctrl+click open ' + hint.url, ...BASE_KEYS] : BASE_KEYS;
  return (
    <div className="sl-window">
      <div className="sl-pane">
        <div className="sl-title"><span className="sl-tab-on">PROJ-1 claude</span><span className="sl-tab">PROJ-4 claude</span></div>
        <div className="sl-term-wrap">
          <div className="sl-term" ref={host} />
          {variant === 'L1' && hint ? (
            <div className="sl-tip" role="tooltip" style={{ left: hint.x, top: hint.y }}>
              <kbd>ctrl+click</kbd> open <span className="sl-url">{hint.url}</span>
            </div>
          ) : null}
          {variant === 'L2' && hint ? (
            <div className="sl-foot" role="status">
              <span className="sl-url">{hint.url}</span> <kbd>ctrl+click</kbd> open
            </div>
          ) : null}
        </div>
      </div>
      <KeyBar keys={keys} />
    </div>
  );
};

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
  return (
    <>
      <Pane key={variant.key} variant={variant.key} />
      {params.get('bare') === null ? (
        <div className="proto-switcher"><span className="proto-label">{variant.key} · {variant.name}</span></div>
      ) : null}
    </>
  );
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Prototype />
  </StrictMode>,
);
