/** What opens a link outside the window: a web address in the system browser, a path in the system's default application. */
export interface LinkOpener {
  openUrl(url: string): Promise<void>;
  openPath(path: string): Promise<void>;
}
