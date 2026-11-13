import { Arrangement } from './design-system';

export enum Focus {
  Board = 'board',
  Docs = 'docs',
  Document = 'document',
  Sessions = 'sessions',
  Files = 'files',
  Comments = 'comments',
  Diff = 'diff',
  Dock = 'dock',
}

/** The two faces of the Base tab's left column, one shown at a time: its panel tabs, named by the focus that shows each. */
export type Panel = Focus.Board | Focus.Docs;

const isPanel = (focus: Focus): focus is Panel => focus === Focus.Board || focus === Focus.Docs;

/** What the window shows: Base is the window without tabs, Board the status columns, Pull requests every open pull request. */
export enum WindowTab {
  Base = 'base',
  Board = 'board',
  PullRequests = 'pullRequests',
}

/** The panes that fill the main view: a ticket's sessions, and a review's diff. */
const isMainView = (focus: Focus): boolean => focus === Focus.Sessions || focus === Focus.Document || focus === Focus.Diff;

interface Parts {
  focus: Focus;
  zoomed: boolean;
  mainHidden: boolean;
  documentShown: boolean;
  dockMinimised: boolean;
  panel: Panel;
}

/**
 * Which pane has focus, whether the dock is zoomed over the window, whether the main view is hidden and whether the dock is
 * minimised to its strip, as the TUI keeps them, and whether the main view shows a document: it does from the Docs panel taking
 * focus until the Board or the sessions do, or until it is closed.
 * The main view hidden and the dock minimised exclude each other: whichever was asked last stands.
 * The left column shows the Board or the Docs panel, the one focus moved to last: focus on the Docs panel shows its tab, focus on the Board shows the Board's, focus elsewhere leaves it.
 */
export class Layout {
  constructor(
    readonly focus: Focus = Focus.Board,
    readonly zoomed: boolean = false,
    readonly mainHidden: boolean = false,
    private readonly documentShown: boolean = false,
    readonly dockMinimised: boolean = false,
    readonly panel: Panel = Focus.Board,
  ) {}

  showsDocument(): boolean {
    return this.documentShown;
  }

  /** A document is on screen, which `esc` on the Docs panel closes first: shown, and the main view not hidden. */
  showsDocumentInView(): boolean {
    return this.documentShown && !this.mainHidden;
  }

  /** The pane of the main view focus goes to from the dock: the document it keeps shown, or else the ticket. */
  mainViewFocus(): Focus {
    return this.documentShown ? Focus.Document : Focus.Sessions;
  }

  /** A document is being read: focus is on it, or on the Docs panel with it in view; `esc` closes it rather than going up. */
  reading(): boolean {
    return this.focus === Focus.Document || (this.focus === Focus.Docs && this.showsDocumentInView());
  }

  /** The main view shows the selected ticket, for its `new session` picker to grow from its bar: no document, not hidden, no zoom over it. Focus on the document goes to the ticket; anywhere else it stays. */
  showTicket(): Layout {
    const shown = this.with({ zoomed: false, mainHidden: false, documentShown: false });
    return this.focus === Focus.Document ? shown.with({ focus: Focus.Sessions }) : shown;
  }

  /** The dock's zoom button: the dock takes focus and fills the window; zoomed already, the zoom ends. */
  zoomDock(): Layout {
    return this.zoomed ? this.leaveZoom() : this.focusOn(Focus.Dock).toggleZoom();
  }

  /** The main view goes back to the selected ticket and focus goes to it, from the Docs panel or the dock alike. */
  closeDocument(): Layout {
    return this.with({ documentShown: false }).focusOn(Focus.Sessions);
  }

  /** The main view shows the selected document again: the Docs cursor moved, or Enter. */
  showDocument(): Layout {
    return this.with({ documentShown: true });
  }

  /**
   * The pane takes focus; a main view that is hidden gives it to the dock, a document asked for while none is shown gives it to the
   * ticket, a minimised dock is restored to take it, and a change of focus ends the zoom.
   */
  focusOn(requested: Focus): Layout {
    const shown = requested === Focus.Document && !this.documentShown ? Focus.Sessions : requested;
    const target = isMainView(shown) && this.mainHidden ? Focus.Dock : shown;
    const document = target === Focus.Dock || target === Focus.Document ? this.documentShown : target === Focus.Docs;
    return this.with({
      focus: target,
      zoomed: target === this.focus && this.zoomed,
      documentShown: document,
      dockMinimised: target === Focus.Dock ? false : this.dockMinimised,
      panel: isPanel(target) ? target : this.panel,
    });
  }

  /** `o`: the other panel tab is shown and takes focus. */
  togglePanel(): Layout {
    return this.showPanel(this.panel === Focus.Board ? Focus.Docs : Focus.Board);
  }

  /** A click on a panel tab: that tab is shown and takes focus. */
  showPanel(panel: Panel): Layout {
    return this.focusOn(panel);
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

  /** The ticket's hide button: the main view hides as `m` hides it; hidden already, it stays hidden. */
  hideMainView(): Layout {
    return this.mainHidden ? this : this.toggleMainView();
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
      changes.panel ?? this.panel,
    );
  }
}
