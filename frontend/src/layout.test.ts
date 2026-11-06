import { describe, expect, it } from 'vitest';
import { Focus, Layout } from './layout';
import { Arrangement } from './design-system';

describe('the window\'s layout', () => {
  it('starts on the board with every pane shown', () => {
    const layout = new Layout();
    expect(layout.focus).toBe(Focus.Board);
    expect(layout.arrangement(true)).toBe(Arrangement.Full);
  });

  it('shows no dock while there is no hosted session', () => {
    expect(new Layout().arrangement(false)).toBe(Arrangement.NoDock);
  });

  it('zooms the dock over the window and leaves the zoom', () => {
    const zoomed = new Layout().focusOn(Focus.Dock).toggleZoom();
    expect(zoomed.arrangement(true)).toBe(Arrangement.Zoomed);
    expect(zoomed.toggleZoom().arrangement(true)).toBe(Arrangement.Full);
    expect(zoomed.leaveZoom().arrangement(true)).toBe(Arrangement.Full);
  });

  it('zooms only the dock, the one pane with a zoom to show', () => {
    expect(new Layout().toggleZoom().arrangement(true)).toBe(Arrangement.Full);
    expect(new Layout().focusOn(Focus.Dock).toggleZoom().arrangement(false)).toBe(Arrangement.NoDock);
  });

  it('leaves the zoom when focus moves', () => {
    expect(new Layout().focusOn(Focus.Dock).toggleZoom().focusOn(Focus.Board).arrangement(true)).toBe(Arrangement.Full);
  });

  it('hides the main view, and moves focus off it to the dock', () => {
    const hidden = new Layout().focusOn(Focus.Sessions).toggleMainView();
    expect(hidden.arrangement(true)).toBe(Arrangement.NoMain);
    expect(hidden.focus).toBe(Focus.Dock);
    expect(hidden.toggleMainView().arrangement(true)).toBe(Arrangement.Full);
  });

  it('shows the main view again when focus goes up to it', () => {
    expect(new Layout().focusOn(Focus.Dock).toggleMainView().goUp(Focus.Sessions).arrangement(true)).toBe(Arrangement.Full);
  });

  it('does not focus the main view while it is hidden', () => {
    expect(new Layout().focusOn(Focus.Dock).toggleMainView().focusOn(Focus.Sessions).focus).toBe(Focus.Dock);
  });

  it('leaves the zoom when the main view is toggled', () => {
    expect(new Layout().focusOn(Focus.Dock).toggleZoom().toggleMainView().arrangement(true)).toBe(Arrangement.NoMain);
  });

  it('gives the main view to the document while the Docs panel has focus, and back to the ticket when the Board or the sessions do', () => {
    expect(new Layout().showsDocument()).toBe(false);
    const docs = new Layout().focusOn(Focus.Docs);
    expect(docs.showsDocument()).toBe(true);
    expect(docs.focusOn(Focus.Board).showsDocument()).toBe(false);
    expect(docs.focusOn(Focus.Sessions).showsDocument()).toBe(false);
  });

  it('keeps the document in the main view while focus is in the dock', () => {
    expect(new Layout().focusOn(Focus.Docs).focusOn(Focus.Dock).showsDocument()).toBe(true);
    expect(new Layout().focusOn(Focus.Dock).showsDocument()).toBe(false);
  });

  it('keeps the document through a zoom and the main view being hidden and shown', () => {
    const layout = new Layout().focusOn(Focus.Docs).toggleMainView().toggleMainView().focusOn(Focus.Dock).toggleZoom().leaveZoom();
    expect(layout.showsDocument()).toBe(true);
  });

  it('does not focus the diff of a review while the main view is hidden, and shows it again when focus goes up to it', () => {
    expect(new Layout().focusOn(Focus.Dock).toggleMainView().focusOn(Focus.Diff).focus).toBe(Focus.Dock);
    expect(new Layout().focusOn(Focus.Dock).toggleMainView().goUp(Focus.Diff).arrangement(true)).toBe(Arrangement.Full);
  });

  it('moves focus off the diff of a review to the dock when the main view is hidden', () => {
    expect(new Layout().focusOn(Focus.Diff).toggleMainView().focus).toBe(Focus.Dock);
  });

  it('shows no document while a review panel has focus', () => {
    expect(new Layout().focusOn(Focus.Docs).focusOn(Focus.Files).showsDocument()).toBe(false);
    expect(new Layout().focusOn(Focus.Comments).showsDocument()).toBe(false);
  });
});
