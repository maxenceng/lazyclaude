package dev.maxenceng.lazyclaude.session.published;

/** Whether a ticket's branch is among the merged pull requests' heads, as far as the asker has read them. */
public enum MergeState {
    MERGED,
    NOT_MERGED,
    /** Review's merged read has not landed, or the asker does not read it. */
    UNKNOWN
}
