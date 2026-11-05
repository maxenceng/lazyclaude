import type { LinkOpener } from './opener';

/** An opener that records what it was asked to open, and refuses with `failure` when the test sets one. */
export class RecordingOpener implements LinkOpener {
  readonly urls: string[] = [];
  readonly paths: string[] = [];
  failure: Error | undefined;

  openUrl(url: string): Promise<void> {
    this.urls.push(url);
    return this.failure === undefined ? Promise.resolve() : Promise.reject(this.failure);
  }

  openPath(path: string): Promise<void> {
    this.paths.push(path);
    return this.failure === undefined ? Promise.resolve() : Promise.reject(this.failure);
  }
}
