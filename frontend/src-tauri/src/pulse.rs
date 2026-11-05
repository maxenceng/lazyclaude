//! Whether the page still answers: it beats through `page_alive`, and a window that stops hearing it offers Reload. It knows no method of the sidecar.

use std::sync::Mutex;
use std::time::{Duration, Instant};

/// How long the page may be silent before the window offers Reload; a page loading is allowed this long too.
pub const SILENCE_LIMIT: Duration = Duration::from_secs(10);

/// What the window should do after hearing, or not hearing, the page.
#[derive(Debug, PartialEq, Eq)]
pub enum Verdict {
    Nothing,
    OfferReload,
    WithdrawReload,
}

struct Heard {
    at: Instant,
    offered: bool,
}

/// The last beat of the page and whether Reload is on offer.
pub struct Pulse {
    heard: Mutex<Heard>,
    limit: Duration,
}

impl Pulse {
    pub fn new(now: Instant, limit: Duration) -> Self {
        Self {
            heard: Mutex::new(Heard {
                at: now,
                offered: false,
            }),
            limit,
        }
    }

    pub fn beat(&self, now: Instant) {
        self.lock().at = now;
    }

    /// Offers Reload once the page has been silent for the limit, and withdraws it once the page beats again.
    pub fn check(&self, now: Instant) -> Verdict {
        let mut heard = self.lock();
        let silent = now.saturating_duration_since(heard.at) >= self.limit;
        match (silent, heard.offered) {
            (true, false) => {
                heard.offered = true;
                Verdict::OfferReload
            }
            (false, true) => {
                heard.offered = false;
                Verdict::WithdrawReload
            }
            _ => Verdict::Nothing,
        }
    }

    /// The window reloaded the page: it has a new limit to load in, and nothing is on offer.
    pub fn reloaded(&self, now: Instant) {
        let mut heard = self.lock();
        heard.at = now;
        heard.offered = false;
    }

    fn lock(&self) -> std::sync::MutexGuard<'_, Heard> {
        self.heard.lock().unwrap_or_else(|poisoned| poisoned.into_inner())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const LIMIT: Duration = Duration::from_secs(10);

    fn after(start: Instant, seconds: u64) -> Instant {
        start + Duration::from_secs(seconds)
    }

    #[test]
    fn a_page_that_beats_in_time_is_left_alone() {
        let start = Instant::now();
        let pulse = Pulse::new(start, LIMIT);
        pulse.beat(after(start, 5));
        assert_eq!(pulse.check(after(start, 14)), Verdict::Nothing);
    }

    #[test]
    fn a_page_silent_for_the_limit_is_offered_reload_once() {
        let start = Instant::now();
        let pulse = Pulse::new(start, LIMIT);
        assert_eq!(pulse.check(after(start, 9)), Verdict::Nothing);
        assert_eq!(pulse.check(after(start, 10)), Verdict::OfferReload);
        assert_eq!(pulse.check(after(start, 11)), Verdict::Nothing);
    }

    #[test]
    fn a_page_that_beats_again_has_the_offer_withdrawn() {
        let start = Instant::now();
        let pulse = Pulse::new(start, LIMIT);
        assert_eq!(pulse.check(after(start, 12)), Verdict::OfferReload);
        pulse.beat(after(start, 13));
        assert_eq!(pulse.check(after(start, 14)), Verdict::WithdrawReload);
        assert_eq!(pulse.check(after(start, 15)), Verdict::Nothing);
    }

    #[test]
    fn a_reloaded_page_has_the_whole_limit_to_load() {
        let start = Instant::now();
        let pulse = Pulse::new(start, LIMIT);
        assert_eq!(pulse.check(after(start, 12)), Verdict::OfferReload);
        pulse.reloaded(after(start, 12));
        assert_eq!(pulse.check(after(start, 20)), Verdict::Nothing);
        assert_eq!(pulse.check(after(start, 22)), Verdict::OfferReload);
    }

    #[test]
    fn a_clock_that_runs_backwards_is_not_silence() {
        let start = Instant::now();
        let pulse = Pulse::new(after(start, 20), LIMIT);
        assert_eq!(pulse.check(start), Verdict::Nothing);
    }
}
