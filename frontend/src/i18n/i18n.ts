import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import board from './locales/en/board.json';
import common from './locales/en/common.json';
import designSystem from './locales/en/designSystem.json';
import docs from './locales/en/docs.json';
import session from './locales/en/session.json';

export const DEFAULT_LANGUAGE = 'en';

/** One namespace per context, `designSystem` for the primitives' own words, `common` for the composition side. */
export const resources = {
  en: { common, board, docs, session, designSystem },
} as const;

// deferred: English is the one language — close it when a second one is wanted, adding its locales and a parity test.
void i18n.use(initReactI18next).init({
  resources,
  lng: DEFAULT_LANGUAGE,
  fallbackLng: DEFAULT_LANGUAGE,
  defaultNS: 'common',
  ns: Object.keys(resources.en),
  initAsync: false,
  interpolation: { escapeValue: false },
  returnNull: false,
});

export { i18n };
