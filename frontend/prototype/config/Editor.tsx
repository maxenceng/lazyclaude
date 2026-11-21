// PROTOTYPE: the configuration editor, laid out three ways (C1 stacked, C2 split, C3 sectioned). Not shipped.
import type { ReactNode } from 'react';
import { Alert, Button, ButtonTone, PaneAction, PaneActionKind, Text, Tone } from '../../src/design-system';
import { CHORDS, PATH, Reach, VERBS, type ChordRow, type ConfigState, type VerbsRow } from './data';

const noop = () => undefined;

export type Layout = 'stack' | 'split' | 'tabs';

const ReachTag = ({ reach }: { reach: Reach }) => (reach === Reach.Both ? null : <span className="cf-reach">{reach} only</span>);

const Origin = ({ set, changed }: { set: boolean; changed: boolean }) =>
  changed ? <span className="cf-origin cf-changed">changed</span> : set ? <span className="cf-origin cf-set">set</span> : <span className="cf-origin">default</span>;

const ChordLine = ({ row, state }: { row: ChordRow; state: ConfigState }) => {
  const recording = state === 'recording' && row.key === 'nextWaiting';
  const changed = (state === 'edited' || state === 'refused') && row.key === 'nextWaiting';
  const refused = state === 'refused' && row.key === 'nextWaiting';
  const chord = changed ? 'alt+h' : row.chord;
  return (
    <>
      <div className={recording ? 'cf-row cf-cursor' : 'cf-row'}>
        <span className="cf-name">{row.action}</span>
        <ReachTag reach={row.reach} />
        <span className={recording ? 'cf-chord cf-recording' : refused ? 'cf-chord cf-bad' : 'cf-chord'}>{recording ? 'press a chord…' : chord}</span>
        <Origin set={row.set} changed={changed} />
      </div>
      {refused ? <div className="cf-why">alt+h is already Focus left</div> : null}
    </>
  );
};

const Switch = ({ on }: { on: boolean }) => (
  <span className={on ? 'cf-switch cf-on' : 'cf-switch'} role="switch" aria-checked={on}>
    <span className="cf-knob" />
    {on ? 'on' : 'off'}
  </span>
);

const OptionLines = ({ state }: { state: ConfigState }) => (
  <>
    <div className="cf-row">
      <span className="cf-name">Desktop notifications</span>
      <Switch on />
      <Origin set={false} changed={false} />
    </div>
    <div className="cf-row">
      <span className="cf-name">Mouse</span>
      <ReachTag reach={Reach.Tui} />
      <Switch on={state !== 'stale'} />
      <Origin set={state === 'stale'} changed={false} />
    </div>
    <div className="cf-row">
      <span className="cf-name">Ticket plugin</span>
      <span className="cf-text">{state === 'edited' ? 'my-workflow' : 'ddd-workflow'}</span>
      <Origin set={false} changed={state === 'edited'} />
    </div>
  </>
);

const VerbChips = ({ row, state }: { row: VerbsRow; state: ConfigState }) => {
  const cursor = state === 'edited' && row.status === 'in-progress';
  const on = cursor ? ['design', 'review'] : row.on;
  return (
    <div className={cursor ? 'cf-verbs cf-cursor' : 'cf-verbs'}>
      <span className="cf-status" style={{ color: row.colour }}>
        {row.status}
      </span>
      <span className="cf-chips">
        {on.map((verb, index) => (
          <span key={verb} className={cursor && index === 0 ? 'cf-chip cf-chip-on cf-chip-cursor' : 'cf-chip cf-chip-on'}>
            {index === 0 ? <span className="cf-first" title="preselected" /> : null}
            {verb}
          </span>
        ))}
        {row.off.map((verb) => (
          <span key={verb} className="cf-chip">
            {verb}
          </span>
        ))}
        {on.length === 0 ? <span className="cf-none">no verb</span> : null}
      </span>
      <Origin set={row.set} changed={cursor} />
    </div>
  );
};

const Section = ({ title, children }: { title: string; children: ReactNode }) => (
  <section className="cf-section">
    <h3 className="cf-head">{title}</h3>
    {children}
  </section>
);

const Keys = ({ state }: { state: ConfigState }) => (
  <Section title="Reserved chords">
    {CHORDS.map((row) => (
      <ChordLine key={row.key} row={row} state={state} />
    ))}
  </Section>
);
const Options = ({ state }: { state: ConfigState }) => (
  <Section title="Options">
    <OptionLines state={state} />
  </Section>
);
const Verbs = ({ state }: { state: ConfigState }) => (
  <Section title="Verbs per status">
    {VERBS.map((row) => (
      <VerbChips key={row.status} row={row} state={state} />
    ))}
    <p className="cf-note">Lit verbs are offered in that order; the first is preselected.</p>
  </Section>
);

const Notices = ({ state }: { state: ConfigState }) => (
  <>
    {state === 'refused' ? (
      <Alert title="Not saved">
        <Text tight>keybinding.nextWaiting: alt+h is already Focus left.</Text>
      </Alert>
    ) : null}
    {state === 'stale' ? (
      <Alert title="config.yml changed on disk">
        <Text tight>These are its new values; your edits were not saved.</Text>
      </Alert>
    ) : null}
    {state === 'invalid-file' ? (
      <Alert title="config.yml is refused">
        <Text tight mono>{PATH}:7: unknown verb 'ship'</Text>
        <Text tight>These are the values in use. Saving replaces the file.</Text>
      </Alert>
    ) : null}
  </>
);

const Body = ({ layout, state }: { layout: Layout; state: ConfigState }) => {
  if (layout === 'split') {
    return (
      <div className="cf-columns">
        <div>
          <Keys state={state} />
          <Options state={state} />
        </div>
        <div>
          <Verbs state={state} />
        </div>
      </div>
    );
  }
  if (layout === 'tabs') {
    return (
      <>
        <div className="cf-tabs" role="tablist">
          <span className="cf-tab cf-tab-on">Chords</span>
          <span className="cf-tab">Options</span>
          <span className="cf-tab">Verbs</span>
          <span className="cf-tabkeys">tab / shift+tab</span>
        </div>
        <Keys state={state} />
      </>
    );
  }
  return (
    <>
      <Keys state={state} />
      <Options state={state} />
      <Verbs state={state} />
    </>
  );
};

export const Editor = ({ layout, state }: { layout: Layout; state: ConfigState }) => {
  const dirty = state === 'edited' || state === 'refused';
  return (
    <div className="cf-scrim">
      <section className={`cf-frame cf-layout-${layout}`} role="dialog" aria-modal="true" aria-label="Configuration">
        <header className="cf-top">
          <h2 className="cf-title">Configuration</h2>
          <span className="cf-path">{PATH}</span>
          <PaneAction kind={PaneActionKind.Close} label="Cancel" shortcut="esc" onClick={noop} />
        </header>
        <div className="cf-body">
          <Notices state={state} />
          <Body layout={layout} state={state} />
        </div>
        <footer className="cf-foot">
          <Text tone={dirty ? Tone.Warning : Tone.Dim} tight>
            Saving rewrites the file without its comments. Applies at once.
          </Text>
          <span className="cf-answers">
            <Button tone={ButtonTone.Quiet} chip="esc" onClick={noop}>
              Cancel
            </Button>
            <Button tone={ButtonTone.Affirm} chip="ctrl+s" onClick={noop}>
              Save
            </Button>
          </span>
        </footer>
      </section>
    </div>
  );
};
