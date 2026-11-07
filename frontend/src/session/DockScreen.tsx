import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { DockTabs, Pane, PaneActionKind, PaneLayout, type Tab, TerminalSurface } from '../design-system';
import type { SessionSessionsView } from '../protocol';
import { useStore } from '../shared/useStore';
import type { Hosting } from './hosting';
import { colourOf, LampColour } from './markers';

interface Props {
  hosting: Hosting;
  sessions: SessionSessionsView | undefined;
  focused: boolean;
  onFocus: () => void;
  onMinimise: () => void;
}

const tabsOf = (ids: readonly string[], sessions: SessionSessionsView | undefined, starting: string): Tab[] =>
  ids.map((id) => {
    const hosted = sessions?.hosted.find((candidate) => candidate.id === id);
    return {
      id,
      label: hosted?.ticket ?? hosted?.live?.name ?? starting,
      lamp: hosted?.live == null ? LampColour.Off : colourOf(hosted.live),
    };
  });

/** The dock: a tab per hosted session over the selected one's terminal, `claude`'s own screen filling the pane. */
export const DockScreen = ({ hosting, sessions, focused, onFocus, onMinimise }: Props) => {
  const { t } = useTranslation('session');
  const dock = useStore(hosting.dock);
  const selected = dock.selectedTab();
  const emulator = selected === undefined ? undefined : hosting.emulatorOf(selected);
  const surface = useRef<HTMLDivElement>(null);

  const tabs = tabsOf(dock.tabs, sessions, t('dock.starting'));

  useEffect(() => {
    const container = surface.current;
    if (emulator === undefined || container === null) {
      return;
    }
    emulator.attach(container);
    const observer = new ResizeObserver(() => emulator.resize());
    observer.observe(container);
    return () => observer.disconnect();
  }, [emulator]);

  useEffect(() => {
    if (focused) {
      emulator?.terminal.focus();
    } else {
      emulator?.terminal.blur();
    }
  }, [focused, emulator]);

  return (
    <Pane title={t('dock.title')} focused={focused} layout={PaneLayout.Fill}>
      <DockTabs
        tabs={tabs}
        selected={selected}
        onSelect={(id) => {
          hosting.select(id);
          onFocus();
        }}
        action={{ kind: PaneActionKind.Minimise, label: t('dock.minimise'), onClick: onMinimise }}
      />
      <TerminalSurface ref={surface} label={t('dock.terminal')} onFocus={onFocus} />
    </Pane>
  );
};

interface StripProps {
  hosting: Hosting;
  sessions: SessionSessionsView | undefined;
  onSelect: (id: string) => void;
  /** Given when the strip is a minimised dock, which its button restores. */
  onRestore?: () => void;
}

/** The dock folded to one line, while a window tab covers the panes or minimised under the main view: a tab per hosted session with its lamp, so none is hidden. */
export const DockStrip = ({ hosting, sessions, onSelect, onRestore }: StripProps) => {
  const { t } = useTranslation('session');
  const dock = useStore(hosting.dock);
  return (
    <DockTabs
      tabs={tabsOf(dock.tabs, sessions, t('dock.starting'))}
      selected={dock.selectedTab()}
      onSelect={onSelect}
      action={onRestore === undefined ? undefined : { kind: PaneActionKind.Restore, label: t('dock.restore'), onClick: onRestore }}
    />
  );
};
