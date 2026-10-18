package dev.maxenceng.lazyclaude.review.published;

/** What the pull request does to a file; a renamed one carries the path it had. */
public sealed interface FileStatus permits AddedFile, ModifiedFile, DeletedFile, RenamedFile {}
