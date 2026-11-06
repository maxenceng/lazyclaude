import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { DockTabs, Pane, type Tab, TerminalSurface } from '../design-system';
import type { SessionSessionsView } from '../protocol';
import { useStore } from '../shared/useStore';
import type { Hosting } from './hosting';
import { colourOf, LampColour } from './markers';

interface Props {
  hosting: Hosting;
  sessions: SessionSessionsView | undefined;
  focused: boolean;
  onFocus: () => void;
}

/** The dock: a tab per hosted session over the selected one's terminal, `claude`'s own screen filling the pane. */
export const DockScreen = ({ hosting, sessions, focused, onFocus }: Props) => {
  const { t } = useTranslation('session');
  const dock = useStore(hosting.dock);
  const selected = dock.selectedTab();
  const emulator = selected === undefined ? undefined : hosting.emulatorOf(selected);
  const surface = useRef<HTMLDivElement>(null);

  const tabs: Tab[] = dock.tabs.map((id) => {
    const hosted = sessions?.hosted.find((candidate) => candidate.id === id);
    return {
      id,
      label: hosted?.ticket ?? hosted?.live?.name ?? t('dock.starting'),
      lamp: hosted?.live == null ? LampColour.Off : colourOf(hosted.live),
    };
  });

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
    <Pane title={t('dock.title')} focused={focused} layout="fill">
      <DockTabs tabs={tabs} selected={selected} onSelect={(id) => {
          hosting.select(id);
          onFocus();
        }} />
      <TerminalSurface ref={surface} label={t('dock.terminal')} onFocus={onFocus} />
    </Pane>
  );
};
