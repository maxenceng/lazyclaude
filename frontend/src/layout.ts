import type { Arrangement } from './design-system';

export enum Focus {
  Board = 'board',
  Docs = 'docs',
  Sessions = 'sessions',
  Dock = 'dock',
}

/**
 * Which pane has focus, whether the dock is zoomed over the window and whether the main view is hidden, as the TUI keeps them,
 * and whether the main view shows a document: it does from the Docs panel taking focus until the Board or the sessions do.
 */
export class Layout {
  constructor(
    readonly focus: Focus = Focus.Board,
    readonly zoomed: boolean = false,
    readonly mainHidden: boolean = false,
    private readonly documentShown: boolean = false,
  ) {}

  showsDocument(): boolean {
    return this.documentShown;
  }

  /** The pane takes focus; a main view that is hidden gives it to the dock, and a change of focus ends the zoom. */
  focusOn(requested: Focus): Layout {
    const target = requested === Focus.Sessions && this.mainHidden ? Focus.Dock : requested;
    const document = target === Focus.Dock ? this.documentShown : target === Focus.Docs;
    return new Layout(target, target === this.focus && this.zoomed, this.mainHidden, document);
  }

  /** Focus goes up to `above`, showing the main view again if that is where it goes. */
  goUp(above: Focus): Layout {
    return new Layout(this.focus, this.zoomed, above === Focus.Sessions ? false : this.mainHidden, this.documentShown).focusOn(above);
  }

  toggleZoom(): Layout {
    if (this.zoomed) {
      return this.leaveZoom();
    }
    return new Layout(this.focus, this.focus === Focus.Dock, this.mainHidden, this.documentShown);
  }

  leaveZoom(): Layout {
    return new Layout(this.focus, false, this.mainHidden, this.documentShown);
  }

  toggleMainView(): Layout {
    const hidden = !this.mainHidden;
    const hiding = new Layout(this.focus, false, hidden, this.documentShown);
    return hidden && this.focus === Focus.Sessions ? hiding.focusOn(Focus.Dock) : hiding;
  }

  arrangement(hasDock: boolean): Arrangement {
    if (!hasDock) {
      return 'noDock';
    }
    if (this.zoomed) {
      return 'zoomed';
    }
    return this.mainHidden ? 'noMain' : 'full';
  }
}
