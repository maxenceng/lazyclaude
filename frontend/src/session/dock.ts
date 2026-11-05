/**
 * The hosted sessions' tabs, in start order, and which is selected, held by id so the selection survives the sessions being read again.
 * A session just started is a tab before the sessions that list it arrive; one whose `claude` exited stays closed while they still list it.
 */
export class Dock {
  constructor(
    readonly tabs: readonly string[] = [],
    private readonly selected: string | undefined = undefined,
    private readonly listed: ReadonlySet<string> = new Set(),
    private readonly closedIds: ReadonlySet<string> = new Set(),
  ) {}

  /** The selected tab, else the first one. */
  selectedTab(): string | undefined {
    return this.selected !== undefined && this.tabs.includes(this.selected) ? this.selected : this.tabs[0];
  }

  select(id: string): Dock {
    return new Dock(this.tabs, id, this.listed, this.closedIds);
  }

  opened(id: string): Dock {
    const tabs = this.tabs.includes(id) ? this.tabs : [...this.tabs, id];
    const closedIds = new Set(this.closedIds);
    closedIds.delete(id);
    return new Dock(tabs, id, this.listed, closedIds);
  }

  /** The tabs after the sessions were read again: the listed ones, and the ones opened that no list has shown yet. */
  listing(ids: readonly string[]): Dock {
    const open = ids.filter((id) => !this.closedIds.has(id));
    const pending = this.tabs.filter((id) => !this.listed.has(id) && !open.includes(id));
    const closedIds = new Set([...this.closedIds].filter((id) => ids.includes(id)));
    return this.replaced([...open, ...pending], new Set(open), closedIds);
  }

  closed(id: string): Dock {
    return this.replaced(
      this.tabs.filter((tab) => tab !== id),
      this.listed,
      new Set([...this.closedIds, id]),
    );
  }

  /** The dock with the tab before the selected one selected; none when it is the first. */
  previous(): Dock | undefined {
    const at = this.indexOfSelected() - 1;
    const id = this.tabs[at];
    return id === undefined ? undefined : this.select(id);
  }

  /** The dock with the tab after the selected one selected, held at the last. */
  next(): Dock {
    const id = this.tabs[this.indexOfSelected() + 1];
    return id === undefined ? this : this.select(id);
  }

  private indexOfSelected(): number {
    return Math.max(0, this.tabs.indexOf(this.selectedTab() ?? ''));
  }

  private replaced(tabs: string[], listed: ReadonlySet<string>, closedIds: ReadonlySet<string>): Dock {
    const was = this.selectedTab();
    if (was === undefined || tabs.includes(was)) {
      return new Dock(tabs, was, listed, closedIds);
    }
    const taken = tabs[Math.min(this.indexOfSelected(), tabs.length - 1)];
    return new Dock(tabs, taken, listed, closedIds);
  }
}
