import { Arrangement } from './design-system';

export enum Focus {
  Board = 'board',
  Docs = 'docs',
  Sessions = 'sessions',
  Files = 'files',
  Comments = 'comments',
  Diff = 'diff',
  Dock = 'dock',
}

/** What the window shows: Base is the window without tabs, Board the status columns, Pull requests every open pull request. */
export enum WindowTab {
  Base = 'base',
  Board = 'board',
  PullRequests = 'pullRequests',
}

/** The panes that fill the main view: a ticket's sessions, and a review's diff. */
const isMainView = (focus: Focus): boolean => focus === Focus.Sessions || focus === Focus.Diff;

interface Parts {
  focus: Focus;
  zoomed: boolean;
  mainHidden: boolean;
  documentShown: boolean;
  dockMinimised: boolean;
}

/**
 * Which pane has focus, whether the dock is zoomed over the window, whether the main view is hidden and whether the dock is
 * minimised to its strip, as the TUI keeps them, and whether the main view shows a document: it does from the Docs panel taking
 * focus until the Board or the sessions do, or until it is closed.
 * The main view hidden and the dock minimised exclude each other: whichever was asked last stands.
 */
export class Layout {
  constructor(
    readonly focus: Focus = Focus.Board,
    readonly zoomed: boolean = false,
    readonly mainHidden: boolean = false,
    private readonly documentShown: boolean = false,
    readonly dockMinimised: boolean = false,
  ) {}

  showsDocument(): boolean {
    return this.documentShown;
  }

  /** The main view goes back to the selected ticket; focus stays where it is. */
  closeDocument(): Layout {
    return this.with({ documentShown: false });
  }

  /** The main view shows the selected document again: the Docs cursor moved, or Enter. */
  showDocument(): Layout {
    return this.with({ documentShown: true });
  }

  /** The pane takes focus; a main view that is hidden gives it to the dock, a minimised dock is restored to take it, and a change of focus ends the zoom. */
  focusOn(requested: Focus): Layout {
    const target = isMainView(requested) && this.mainHidden ? Focus.Dock : requested;
    const document = target === Focus.Dock ? this.documentShown : target === Focus.Docs;
    return this.with({
      focus: target,
      zoomed: target === this.focus && this.zoomed,
      documentShown: document,
      dockMinimised: target === Focus.Dock ? false : this.dockMinimised,
    });
  }

  /** Focus goes up to `above`, showing the main view again if that is where it goes. */
  goUp(above: Focus): Layout {
    return this.with({ mainHidden: isMainView(above) ? false : this.mainHidden }).focusOn(above);
  }

  toggleZoom(): Layout {
    if (this.zoomed) {
      return this.leaveZoom();
    }
    return this.with({ zoomed: this.focus === Focus.Dock });
  }

  leaveZoom(): Layout {
    return this.with({ zoomed: false });
  }

  /** `m`: the main view hides, unfolding a minimised dock; hidden already, it shows again. */
  toggleMainView(): Layout {
    const hidden = !this.mainHidden;
    const hiding = this.with({ zoomed: false, mainHidden: hidden, dockMinimised: hidden ? false : this.dockMinimised });
    return hidden && isMainView(this.focus) ? hiding.focusOn(Focus.Dock) : hiding;
  }

  /** `M` or the dock's button: the dock folds to its strip, showing a hidden main view and moving focus off the dock to `mainFocus`; folded already, it unfolds. */
  toggleDockMinimised(mainFocus: Focus): Layout {
    if (this.dockMinimised) {
      return this.restoreDock();
    }
    const folded = this.with({ zoomed: false, mainHidden: false, dockMinimised: true });
    return this.focus === Focus.Dock ? folded.focusOn(mainFocus) : folded;
  }

  restoreDock(): Layout {
    return this.with({ dockMinimised: false });
  }

  arrangement(hasDock: boolean): Arrangement {
    if (!hasDock) {
      return Arrangement.NoDock;
    }
    if (this.zoomed) {
      return Arrangement.Zoomed;
    }
    if (this.mainHidden) {
      return Arrangement.NoMain;
    }
    return this.dockMinimised ? Arrangement.Minimised : Arrangement.Full;
  }

  private with(changes: Partial<Parts>): Layout {
    return new Layout(
      changes.focus ?? this.focus,
      changes.zoomed ?? this.zoomed,
      changes.mainHidden ?? this.mainHidden,
      changes.documentShown ?? this.documentShown,
      changes.dockMinimised ?? this.dockMinimised,
    );
  }
}
