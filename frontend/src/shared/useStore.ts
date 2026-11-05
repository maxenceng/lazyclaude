import { useCallback, useSyncExternalStore } from 'react';
import type { Store } from './store';

/** One store read: the snapshot, and a rerender when it changes. */
export const useStore = <T>(store: Store<T>): T => {
  const subscribe = useCallback((listener: () => void) => store.subscribe(listener), [store]);
  const snapshot = useCallback(() => store.get(), [store]);
  return useSyncExternalStore(subscribe, snapshot);
};
