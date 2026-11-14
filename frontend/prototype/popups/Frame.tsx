// PROTOTYPE: one frame for every popup, drawn three ways (P1–P3). Not shipped.
import type { ReactNode } from 'react';
import { Button, ButtonTone, PaneAction, PaneActionKind, Text, TextArea, TextInput, Tone } from '../../src/design-system';
import type { PopupKind } from './main';
import './frame.css';

const noop = () => undefined;

export interface Answer {
  label: string;
  chip: string;
  primary?: boolean;
}

interface FrameProps {
  look: 'p1' | 'p2' | 'p3';
  title: string;
  answers: readonly Answer[];
  children: ReactNode | null;
}

const Frame = ({ look, title, answers, children }: FrameProps) => {
  const box = (
    <section className={`pf pf-${look}`} role="dialog" aria-modal="true" aria-label={title} data-modal="">
      <header className="pf-head">
        <h2 className="pf-title">{title}</h2>
        {look === 'p3' ? null : <PaneAction kind={PaneActionKind.Close} label="Cancel" shortcut="esc" onClick={noop} />}
      </header>
      {children === null ? null : <div className="pf-body">{children}</div>}
      <footer className="pf-foot">
        {answers.map((answer) =>
          answer.primary === true ? (
            <Button key={answer.label} tone={ButtonTone.Affirm} chip={answer.chip} onClick={noop}>{answer.label}</Button>
          ) : (
            <button key={answer.label} type="button" className="pf-quiet" onClick={noop}>
              {answer.label}
              <kbd className="pf-chip">{answer.chip}</kbd>
            </button>
          ),
        )}
      </footer>
    </section>
  );
  return look === 'p2' ? <div className="pf-scrim">{box}</div> : box;
};

const CAPTURE: readonly Answer[] = [
  { label: 'Cancel', chip: 'esc' },
  { label: 'Create draft', chip: 'enter', primary: true },
];
const BOX: readonly Answer[] = [
  { label: 'Cancel', chip: 'esc' },
  { label: 'Save', chip: 'ctrl+s', primary: true },
];

export const Popups = ({ look, popup }: { look: FrameProps['look']; popup: PopupKind }) => {
  switch (popup) {
    case 'capture':
    case 'capture-creating':
    case 'capture-refused':
      return (
        <Frame look={look} title="New ticket" answers={CAPTURE}>
          <TextInput label="Ticket title" value="Rework popups look" readOnly={popup === 'capture-creating'} onChange={noop} onSubmit={noop} onCancel={noop} />
          {popup === 'capture-creating' ? <div role="status"><Text tone={Tone.Dim} tight>creating…</Text></div> : null}
          {popup === 'capture-refused' ? <div role="alert"><Text tone={Tone.Warning} tight>⚠ gh: could not reach api.github.com</Text></div> : null}
        </Frame>
      );
    case 'quit':
      return (
        <Frame look={look} title="Quit lazyclaude?" answers={[{ label: 'Stay', chip: 'esc' }, { label: 'Quit', chip: 'y', primary: true }]}>
          <Text tight>2 hosted sessions are open, and quitting ends them.</Text>
        </Frame>
      );
    case 'comment':
    case 'comment-refused':
      return (
        <Frame look={look} title="Pending comment" answers={BOX}>
          <TextArea label="Comment text" value={'This reads the file twice; the watcher already has it.\nCould it take the snapshot instead?'} readOnly={false} onChange={noop} onSave={noop} onCancel={noop} />
          {popup === 'comment-refused' ? <Text tone={Tone.Warning} tight>not saved (gh: HTTP 422 pull request review thread is outdated)</Text> : null}
        </Frame>
      );
    case 'reply':
      return (
        <Frame look={look} title="Why does the watcher debounce at 200ms?" answers={BOX}>
          <Text tight mono>maxenceng Why does the watcher debounce at 200ms?</Text>
          <Text tight mono>claude It matches the editor's save burst; ADR 0071 records the measurement.</Text>
          <TextArea label="Comment text" value="Fine, keep it." readOnly={false} onChange={noop} onSave={noop} onCancel={noop} />
        </Frame>
      );
    case 'submit':
      return (
        <Frame look={look} title="Submit review as Comment?" answers={[{ label: 'Keep pending', chip: 'esc' }, { label: 'Submit', chip: 'y', primary: true }]}>
          {null}
        </Frame>
      );
  }
};

export const VariantP1 = ({ popup }: { popup: PopupKind }) => <Popups look="p1" popup={popup} />;
export const VariantP2 = ({ popup }: { popup: PopupKind }) => <Popups look="p2" popup={popup} />;
export const VariantP3 = ({ popup }: { popup: PopupKind }) => <Popups look="p3" popup={popup} />;
