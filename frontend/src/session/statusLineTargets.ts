import type { SessionSessionsView } from '../protocol';
import { colourOf, type LampColour } from './markers';

/** The dock tab hosting live session `liveId`, found by the live session its launch now runs, so a session `/clear` replaced is still reached; none where this window does not host it. */
export const tabOfLive = (view: SessionSessionsView, liveId: string): string | undefined => view.hosted.find((hosted) => hosted.live?.id === liveId)?.id;

/** The hosted tab of `colour` after `selected` in dock tab order, wrapping, so one of that colour is always the next; the first one when `selected` is not of that colour; none when no hosted session has it. */
export const tabOfNextColour = (view: SessionSessionsView, tabs: readonly string[], selected: string | undefined, colour: LampColour): string | undefined => {
  const ofColour = tabs.filter((tab) => {
    const live = view.hosted.find((hosted) => hosted.id === tab)?.live;
    return live != null && colourOf(live) === colour;
  });
  const after = ofColour.find((tab) => tabs.indexOf(tab) > tabs.indexOf(selected ?? ''));
  return after ?? ofColour[0];
};
