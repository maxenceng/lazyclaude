package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionView;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;

/**
 * The dock's tabs, one per hosted session in start order, and which is selected, by id so that it survives the sessions being
 * read again. A tab just started may be selected before the sessions that list it arrive.
 */
final class DockTabs {

    private List<HostedSessionView> tabs = List.of();
    private Optional<HostedSessionId> selected = Optional.empty();

    List<HostedSessionView> tabs() {
        return tabs;
    }

    boolean isEmpty() {
        return tabs.isEmpty();
    }

    /** The selected tab, else the first one. */
    Optional<HostedSessionView> selectedTab() {
        return selected.flatMap(this::tabOf).or(() -> tabs.stream().findFirst());
    }

    void select(HostedSessionId id) {
        selected = Optional.of(id);
    }

    /** Selects the tab before the selected one, and says whether there was one. */
    boolean selectPrevious() {
        return selectAt(selectedIndex() - 1);
    }

    /** Selects the tab after the selected one, and says whether there was one. */
    boolean selectNext() {
        return selectAt(selectedIndex() + 1);
    }

    int selectedIndex() {
        return selectedTab().map(tab -> indexOf(tab.id())).orElse(0);
    }

    /** Swaps in the tabs read again, and says what became of the selected tab. */
    TabClosing replace(List<HostedSessionView> next) {
        Optional<HostedSessionId> was = selectedTab().map(HostedSessionView::id);
        int index = selectedIndex();
        tabs = next;
        if (was.isEmpty() || tabOf(was.get()).isPresent()) {
            return TabClosing.NONE;
        }
        if (index < next.size()) {
            selected = Optional.of(next.get(index).id());
            return TabClosing.NEXT_TAKES_OVER;
        }
        selected = next.stream().reduce((first, last) -> last).map(HostedSessionView::id);
        return TabClosing.LAST_ONE_GONE;
    }

    private boolean selectAt(int index) {
        if (index < 0 || index >= tabs.size()) {
            return false;
        }
        selected = Optional.of(tabs.get(index).id());
        return true;
    }

    private Optional<HostedSessionView> tabOf(HostedSessionId id) {
        return tabs.stream().filter(tab -> tab.id().equals(id)).findFirst();
    }

    private int indexOf(HostedSessionId id) {
        return IntStream.range(0, tabs.size())
                .filter(index -> tabs.get(index).id().equals(id))
                .findFirst()
                .orElse(0);
    }
}
