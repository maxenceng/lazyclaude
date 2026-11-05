import { Store } from './store';

/**
 * The window's notice: what one of your gestures did not do, such as a link that would not open. Unlike a warning it is
 * no degraded mode and never touches the backend's: the next selection or followed link clears it.
 */
export class NoticeStore {
  readonly notice: Store<string | undefined> = new Store<string | undefined>(undefined);

  raise(text: string): void {
    this.notice.set(text);
  }

  clear(): void {
    if (this.notice.get() !== undefined) {
      this.notice.set(undefined);
    }
  }
}
