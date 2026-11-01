import { useSyncExternalStore } from 'react';
import type { Store } from '../protocol';

/** One store read: the snapshot, and a rerender when it changes. */
export function useStore<T>(store: Store<T>): T {
  return useSyncExternalStore(store.subscribe, store.get);
}
