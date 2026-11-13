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
  zoomed: boolean;
  /** The chord that zooms the dock from inside it, which `config.yml` chooses; none while no configuration is read. */
  zoomKey: string | undefined;
  onMinimise: () => void;
  onZoom: () => void;
}

const tabsOf = (ids: readonly string[], sessions: SessionSessionsView | undefined, starting: string): Tab[] =>
  ids.map((id) => {
    const hosted = sessions?.hosted.find((candidate) => candidate.id === id);
    return {
      id,
      label: hosted?.live?.name ?? hosted?.ticket ?? starting,
      lamp: hosted?.live == null ? LampColour.Off : colourOf(hosted.live),
    };
  });

/** The dock, titled Sessions: a tab per hosted session beside its title, over the selected one's terminal, `claude`'s own screen filling the pane. */
export const DockScreen = ({ hosting, sessions, focused, zoomed, zoomKey, onFocus, onMinimise, onZoom }: Props) => {
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
    <Pane
      title={t('dock.title')}
      focused={focused}
      layout={PaneLayout.Fill}
      actions={[
        { kind: PaneActionKind.Zoom, label: zoomed ? t('dock.unzoom') : t('dock.zoom'), shortcut: zoomKey, onClick: onZoom },
        { kind: PaneActionKind.Minimise, label: t('dock.minimise'), shortcut: t('dock.minimiseKey'), onClick: onMinimise },
      ]}
      beside={
        <DockTabs
          tabs={tabs}
          selected={selected}
          onSelect={(id) => {
            hosting.select(id);
            onFocus();
          }}
        />
      }
    >
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
      strip
      tabs={tabsOf(dock.tabs, sessions, t('dock.starting'))}
      selected={dock.selectedTab()}
      onSelect={onSelect}
      action={onRestore === undefined ? undefined : { kind: PaneActionKind.Restore, label: t('dock.restore'), shortcut: t('dock.minimiseKey'), onClick: onRestore }}
    />
  );
};
