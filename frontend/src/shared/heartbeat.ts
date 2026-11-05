/** Tells the window every `interval` milliseconds that the page is running; the window offers Reload when it stops hearing it (ADR 0083). */
export const startHeartbeat = (beat: () => Promise<void>, interval: number): (() => void) => {
  const tell = () => {
    beat().catch(() => {
      // deferred: a beat nothing hears (a page outside the window) is dropped — close it when the page has a place to say so.
    });
  };
  tell();
  const timer = setInterval(tell, interval);
  return () => clearInterval(timer);
};
