package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.StatusVerbs;

/** The verbs {@code config.yml} gave the statuses it names; a status it leaves out is not in the list. */
public interface ConfiguredVerbsPort {

    List<StatusVerbs> configured();
}
