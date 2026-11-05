import { KeptModes } from './privateModes';

const PREFIX = 'lazyclaude.modes.';

export interface ModeMemory {
  remember(session: string, kept: KeptModes): void;
  recall(session: string): KeptModes | undefined;
  forget(session: string): void;
}

const isShape = (value: unknown): value is ReturnType<KeptModes['toJSON']> => {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const { modes, keyboard } = value as Record<string, unknown>;
  return Array.isArray(modes) && Array.isArray(keyboard);
};

/** The modes each session's emulator was set to, in `storage` (sessionStorage in the window: it survives a reload and not a quit, ADR 0057). */
export const createModeMemory = (storage: Storage): ModeMemory => ({
  remember: (session, kept) => storage.setItem(PREFIX + session, JSON.stringify(kept)),
  recall(session) {
    const text = storage.getItem(PREFIX + session);
    if (text === null) {
      return undefined;
    }
    try {
      const json: unknown = JSON.parse(text);
      return isShape(json) ? KeptModes.fromJSON(json) : undefined;
    } catch {
      return undefined;
    }
  },
  forget: (session) => storage.removeItem(PREFIX + session),
});
