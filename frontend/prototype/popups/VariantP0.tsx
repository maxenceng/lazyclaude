// PROTOTYPE: today's popups, drawn with the shipped primitives. Not shipped.
import { Button, Dialog, Popup, Text, TextArea, TextInput, Tone } from '../../src/design-system';
import type { PopupProps } from './main';

const noop = () => undefined;

export const VariantP0 = ({ popup }: PopupProps) => {
  switch (popup) {
    case 'capture':
    case 'capture-creating':
    case 'capture-refused':
      return (
        <Popup title="New ticket">
          <TextInput label="Ticket title" value="Rework popups look" readOnly={popup === 'capture-creating'} onChange={noop} onSubmit={noop} onCancel={noop} />
          {popup === 'capture-creating' ? <div role="status"><Text tone={Tone.Dim}>creating…</Text></div> : null}
          {popup === 'capture-refused' ? <div role="alert"><Text tone={Tone.Warning}>⚠ gh: could not reach api.github.com</Text></div> : null}
        </Popup>
      );
    case 'quit':
      return (
        <Dialog title="Quit lazyclaude?" hint="y quit · esc stay">
          <Text>2 hosted sessions are open, and quitting ends them.</Text>
          <Button onClick={noop}>Quit</Button>
          <Button onClick={noop}>Stay</Button>
        </Dialog>
      );
    case 'comment':
    case 'comment-refused':
      return (
        <Dialog title="Pending comment" hint="ctrl+s save · esc cancel">
          <TextArea label="Comment text" value={'This reads the file twice; the watcher already has it.\nCould it take the snapshot instead?'} readOnly={false} onChange={noop} onSave={noop} onCancel={noop} />
          {popup === 'comment-refused' ? <Text tone={Tone.Warning} tight>not saved (gh: HTTP 422 pull request review thread is outdated)</Text> : null}
        </Dialog>
      );
    case 'reply':
      return (
        <Dialog title="Why does the watcher debounce at 200ms?" hint="ctrl+s save · esc cancel">
          <Text tight mono>maxenceng Why does the watcher debounce at 200ms?</Text>
          <Text tight mono>claude It matches the editor's save burst; ADR 0071 records the measurement.</Text>
          <TextArea label="Comment text" value="Fine, keep it." readOnly={false} onChange={noop} onSave={noop} onCancel={noop} />
        </Dialog>
      );
    case 'submit':
      return (
        <Dialog title="Submit review as Comment?" hint="y submit · esc keeps it pending">
          <Button onClick={noop}>Submit</Button>
          <Button onClick={noop}>Keep pending</Button>
        </Dialog>
      );
  }
};
