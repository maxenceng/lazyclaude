import { describe, expect, it } from 'vitest';
import { RpcError } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { CaptureController } from './captureController';
import { CaptureStateKind } from './captureState';

const setup = (root: () => string | undefined = () => '/p') => {
  const client = fakeClient();
  const captured: string[] = [];
  const controller = new CaptureController({ client, root, onCaptured: (id) => captured.push(id) });
  return { client, controller, captured };
};

describe('capturing a draft ticket', () => {
  it('is closed until it is opened', () => {
    const { controller } = setup();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
  });

  it('does not open while there is no project to capture into', () => {
    const { controller } = setup(() => undefined);
    controller.open();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
  });

  it('opens empty and keeps what is typed', () => {
    const { controller } = setup();
    controller.open();
    controller.edit('Fix the bug');
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Composing, title: 'Fix the bug' });
  });

  it('cancels back to closed', () => {
    const { controller } = setup();
    controller.open();
    controller.cancel();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
  });

  it('sends the title with the project root and shows it creating', () => {
    const { client, controller } = setup();
    controller.open();
    controller.edit('Fix the bug');
    controller.create();
    expect(client.calls).toEqual([{ method: 'board/capture', params: { root: '/p', title: 'Fix the bug' } }]);
    expect(controller.state.get().kind).toBe(CaptureStateKind.Creating);
  });

  it('sends nothing for a second enter while creating, and ignores esc and typing', () => {
    const { client, controller } = setup();
    controller.open();
    controller.edit('Fix');
    controller.create();
    controller.create();
    controller.cancel();
    controller.edit('other');
    expect(client.calls).toHaveLength(1);
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Creating, title: 'Fix' });
  });

  it('closes on a captured answer and tells which ticket was made', async () => {
    const { client, controller, captured } = setup();
    controller.open();
    controller.edit('Fix');
    controller.create();
    client.answerNext({ kind: 'capturedOnBoard', id: 'PROJ-9', board: { kind: 'noBoardAfterCapture' } });
    await Promise.resolve();
    await Promise.resolve();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
    expect(captured).toEqual(['PROJ-9']);
  });

  it('keeps the title and the reason on a refusal, and sends again only on another create', async () => {
    const { client, controller, captured } = setup();
    controller.open();
    controller.edit('Fix');
    controller.create();
    client.answerNext({ kind: 'refusedTicketCapture', reason: 'gh is not signed in' });
    await Promise.resolve();
    await Promise.resolve();
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Refused, title: 'Fix', reason: 'gh is not signed in' });
    expect(client.calls).toHaveLength(1);
    expect(captured).toEqual([]);
    controller.create();
    expect(client.calls).toHaveLength(2);
  });

  it('keeps the title and says why when the call itself fails', async () => {
    const { client, controller } = setup();
    controller.open();
    controller.edit('Fix');
    controller.create();
    client.failNext(new RpcError(-32000, 'the sidecar is not running'));
    await Promise.resolve();
    await Promise.resolve();
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Refused, title: 'Fix', reason: 'the sidecar is not running' });
  });

  it('drops a refusal with esc, and editing after one goes back to composing', async () => {
    const { client, controller } = setup();
    controller.open();
    controller.create();
    client.answerNext({ kind: 'refusedTicketCapture', reason: 'no title' });
    await Promise.resolve();
    await Promise.resolve();
    controller.edit('x');
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Composing, title: 'x' });
    controller.create();
    client.answerNext({ kind: 'refusedTicketCapture', reason: 'again' });
    await Promise.resolve();
    await Promise.resolve();
    controller.cancel();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
  });
});
