package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

/** The comments of a thread as the threads query writes them. */
record ThreadCommentConnectionRecord(List<ThreadCommentRecord> nodes) {}
