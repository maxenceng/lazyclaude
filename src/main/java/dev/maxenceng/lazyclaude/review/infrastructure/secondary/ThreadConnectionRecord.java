package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

/** The threads a page of the threads query holds. */
record ThreadConnectionRecord(List<ThreadRecord> nodes) {}
