# 37. The transcript view shows prompts, replies and tool calls

Date: 2026-10-17

## Status

Accepted. Builds on ADR 0023 (transcripts and their watch).

## Context

Selecting an outside or past session should show what it said, without lazyclaude hosting it.

## Decision

- **Rendered:** a user entry whose content is text (not a meta prompt) as a prompt behind `›`; an assistant `text` block as a reply; each
  `tool_use` block as one dim `⏺ <Tool>  <argument>` line. The argument is the first of `command`, `file_path`, `pattern`, `path`, `url`,
  `query`, `description`, `skill`, `prompt` the input holds, else its first text, on one line; none when it holds no text.
- **Hidden:** tool results (user entries of `tool_result` blocks), thinking, meta prompts and every other entry type.
- **A separate read** (`TranscriptReadPort`) from the metadata read the cache keeps: it keeps nothing, takes an offset and hands back the
  offset after the last complete line, so a half-written line is read once it is whole. An offset past the end of a shorter file reads
  from the start, and the result says so, so the shell replaces what it showed.
- **Found** by session id in the folder of the session's directory, else in any folder.
- **Tailing is driven by the shell:** each push of the sessions (which the transcript watch and the sweep both cause) reads the selected
  session from its offset, off the render thread, one read at a time. Only the selected session is read; hosted sessions and Elsewhere
  rows keep their facts.
- The view follows new entries while scrolled to the bottom; scrolling up lets go and `G` or reaching the bottom follows again.

## Consequences

- The first read of a very long transcript parses all of it.
- The facts view of a past session is gone: its row says its title and age, its main view its transcript.
