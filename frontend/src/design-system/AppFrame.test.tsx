import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Arrangement, AppFrame } from './AppFrame';
import { ruleOf, stylesheet } from './stylesheet';

const frame = stylesheet('AppFrame.module.css');

describe('the window frame', () => {
  it('gives the main view more of the right column than the dock', () => {
    expect(ruleOf(frame, '.full')).toContain('grid-template-rows: minmax(0, 62fr) minmax(0, 38fr)');
  });
});

describe('a tab with a preview', () => {
  it('puts the preview in a full-height column right of the tab, the dock strip under both', () => {
    expect(ruleOf(frame, '.tabPreview')).toContain("grid-template-areas: 'board main' 'dock dock'");
  });

  it('draws the tab and its preview side by side, and the left column holds the tab alone', () => {
    render(<AppFrame statusLine={null} keyBar={null} board={<p>columns</p>} docs={null} main={<p>preview</p>} dock={null} arrangement={Arrangement.TabPreview} />);
    expect(screen.getByText('columns')).toBeInTheDocument();
    expect(screen.getByText('preview')).toBeInTheDocument();
  });
});
