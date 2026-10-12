package dev.maxenceng.lazyclaude.shell;

/** What the dock's tabs becoming the sessions read again did to the selected tab: nothing, a tab after it took over, or none was left after it. */
enum TabClosing {
    NONE,
    NEXT_TAKES_OVER,
    LAST_ONE_GONE
}
