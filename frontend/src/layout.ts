import type { Arrangement } from './design-system';

export enum Focus {
  Board = 'board',
  Sessions = 'sessions',
  Dock = 'dock',
}

/** Which pane has focus, whether the dock is zoomed over the window and whether the main view is hidden, as the TUI keeps them. */
export class Layout {
  constructor(
    readonly focus: Focus = Focus.Board,
    readonly zoomed: boolean = false,
    readonly mainHidden: boolean = false,
  ) {}

  /** The pane takes focus; a main view that is hidden gives it to the dock, and a change of focus ends the zoom. */
  focusOn(requested: Focus): Layout {
    const target = requested === Focus.Sessions && this.mainHidden ? Focus.Dock : requested;
    return new Layout(target, target === this.focus && this.zoomed, this.mainHidden);
  }

  /** Focus goes up to `above`, showing the main view again if that is where it goes. */
  goUp(above: Focus): Layout {
    return new Layout(this.focus, this.zoomed, above === Focus.Sessions ? false : this.mainHidden).focusOn(above);
  }

  toggleZoom(): Layout {
    if (this.zoomed) {
      return this.leaveZoom();
    }
    return new Layout(this.focus, this.focus === Focus.Dock, this.mainHidden);
  }

  leaveZoom(): Layout {
    return new Layout(this.focus, false, this.mainHidden);
  }

  toggleMainView(): Layout {
    const hidden = !this.mainHidden;
    const hiding = new Layout(this.focus, false, hidden);
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
