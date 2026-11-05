import '@testing-library/jest-dom/vitest';
import './i18n/i18n';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

afterEach(() => {
  cleanup();
  sessionStorage.clear();
});

class MediaQueryStub {
  readonly matches = false;

  constructor(readonly media: string) {}

  addEventListener() {}
  removeEventListener() {}
  addListener() {}
  removeListener() {}
}

Object.defineProperty(window, 'matchMedia', {
  configurable: true,
  value: (query: string) => new MediaQueryStub(query),
});

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
Object.defineProperty(globalThis, 'ResizeObserver', { configurable: true, value: ResizeObserverStub });
