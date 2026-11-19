const token = (name: string): string => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

/** The pixel size mermaid sets its labels in, and the size the reading faces are loaded at to measure them. */
export const DIAGRAM_FONT_PX = 15;

/** Mermaid's `base` theme variables, read from the design tokens at the moment a diagram is drawn: nodes lift off the sunk panel they sit on. */
export const themeOf = (): Record<string, string> => {
  const text = token('--text');
  const dim = token('--dim');
  const ground = token('--pane-sunk');
  const node = token('--pane-lift');
  const edge = token('--seam');
  return {
    background: ground,
    primaryColor: node,
    primaryTextColor: text,
    primaryBorderColor: edge,
    secondaryColor: node,
    tertiaryColor: ground,
    lineColor: dim,
    textColor: text,
    mainBkg: node,
    nodeBorder: edge,
    clusterBkg: ground,
    edgeLabelBackground: ground,
    actorBkg: node,
    actorBorder: edge,
    actorTextColor: text,
    actorLineColor: edge,
    signalColor: dim,
    signalTextColor: text,
    labelBoxBkgColor: node,
    labelTextColor: text,
    noteBkgColor: node,
    noteTextColor: text,
    noteBorderColor: edge,
    fontFamily: token('--font-read'),
    fontSize: `${DIAGRAM_FONT_PX}px`,
  };
};

/** The reading faces a label may be set in, as `document.fonts.load` names them: regular, semibold and italic. */
export const readingFaces = (): string[] => {
  const family = token('--font-read');
  return [`400 ${DIAGRAM_FONT_PX}px ${family}`, `600 ${DIAGRAM_FONT_PX}px ${family}`, `italic 400 ${DIAGRAM_FONT_PX}px ${family}`];
};
