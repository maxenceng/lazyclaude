/** Tells the window every `interval` milliseconds that the page is running; the window offers Reload when it stops hearing it (ADR 0083). */
export class Heartbeat {
  private readonly timer: ReturnType<typeof setInterval>;

  constructor(
    private readonly beat: () => Promise<void>,
    interval: number,
  ) {
    this.tell();
    this.timer = setInterval(() => this.tell(), interval);
  }

  stop(): void {
    clearInterval(this.timer);
  }

  private tell(): void {
    this.beat().catch(() => {
      // deferred: a beat nothing hears (a page outside the window) is dropped — close it when the page has a place to say so.
    });
  }
}
