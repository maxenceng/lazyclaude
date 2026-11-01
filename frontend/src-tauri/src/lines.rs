//! Splits a byte stream into lines and remembers the first one.

#[derive(Default)]
pub struct Lines {
    pending: Vec<u8>,
    first: Option<String>,
}

impl Lines {
    pub fn feed(&mut self, bytes: &[u8]) -> Vec<String> {
        let mut complete = Vec::new();
        for byte in bytes {
            if *byte == b'\n' {
                let line = String::from_utf8_lossy(&self.pending).into_owned();
                self.pending.clear();
                if self.first.is_none() {
                    self.first = Some(line.clone());
                }
                complete.push(line);
            } else {
                self.pending.push(*byte);
            }
        }
        complete
    }

    pub fn first(&self) -> Option<&str> {
        self.first.as_deref()
    }
}

/// The last few lines of a stream, oldest first.
pub struct Tail {
    capacity: usize,
    lines: std::collections::VecDeque<String>,
}

impl Tail {
    pub fn new(capacity: usize) -> Self {
        Self {
            capacity,
            lines: std::collections::VecDeque::new(),
        }
    }

    pub fn push(&mut self, line: String) {
        if self.lines.len() == self.capacity {
            self.lines.pop_front();
        }
        self.lines.push_back(line);
    }

    pub fn lines(&self) -> Vec<String> {
        self.lines.iter().cloned().collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_line_split_across_two_reads_comes_out_whole_once() {
        let mut lines = Lines::default();
        assert!(lines.feed(b"{\"a\":").is_empty());
        assert_eq!(lines.feed(b"1}\n"), vec!["{\"a\":1}"]);
    }

    #[test]
    fn several_lines_in_one_read_come_out_in_order() {
        let mut lines = Lines::default();
        assert_eq!(lines.feed(b"one\ntwo\nthr"), vec!["one", "two"]);
        assert_eq!(lines.feed(b"ee\n"), vec!["three"]);
    }

    #[test]
    fn the_first_line_is_kept_after_later_lines_went_by() {
        let mut lines = Lines::default();
        assert_eq!(lines.first(), None);
        lines.feed(b"ready\nsecond\n");
        lines.feed(b"third\n");
        assert_eq!(lines.first(), Some("ready"));
    }

    #[test]
    fn a_multibyte_character_split_across_reads_survives() {
        let mut lines = Lines::default();
        let bytes = "é\n".as_bytes();
        assert!(lines.feed(&bytes[..1]).is_empty());
        assert_eq!(lines.feed(&bytes[1..]), vec!["é"]);
    }

    #[test]
    fn a_tail_keeps_only_the_last_lines() {
        let mut tail = Tail::new(2);
        for line in ["a", "b", "c"] {
            tail.push(line.to_string());
        }
        assert_eq!(tail.lines(), vec!["b", "c"]);
    }
}
