import { describe, expect, it } from 'vitest';
import { CaptureController } from './captureController';
import { CaptureStateKind } from './captureState';

const flush = async () => {
  await Promise.resolve();
  await Promise.resolve();
};

class FakeStart {
  readonly calls: { root: string; description: string }[] = [];
  readonly answers: ((reason: string | undefined) => void)[] = [];

  start(root: string, description: string): Promise<string | undefined> {
    this.calls.push({ root, description });
    return new Promise((resolve) => this.answers.push(resolve));
  }
}

const setup = (root: () => string | undefined = () => '/p') => {
  const fake = new FakeStart();
  const controller = new CaptureController({ root, start: (startRoot, description) => fake.start(startRoot, description) });
  return { calls: fake.calls, answers: fake.answers, controller };
};

describe('starting a new ticket', () => {
  it('is closed until it is opened', () => {
    const { controller } = setup();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
  });

  it('does not open while there is no project to start in', () => {
    const { controller } = setup(() => undefined);
    controller.open();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
  });

  it('opens empty and keeps what is typed', () => {
    const { controller } = setup();
    controller.open();
    controller.edit('Fix the bug');
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Composing, description: 'Fix the bug' });
  });

  it('cancels back to closed', () => {
    const { controller } = setup();
    controller.open();
    controller.cancel();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
  });

  it('sends the description as typed with the project root and shows it starting', () => {
    const { calls, controller } = setup();
    controller.open();
    controller.edit('  Fix "the" bug  ');
    controller.create();
    expect(calls).toEqual([{ root: '/p', description: '  Fix "the" bug  ' }]);
    expect(controller.state.get().kind).toBe(CaptureStateKind.Starting);
  });

  it.each(['', '   ', '\n\t'])('calls nothing for the blank description %j and stays open', (blank) => {
    const { calls, controller } = setup();
    controller.open();
    controller.edit(blank);
    controller.create();
    expect(calls).toEqual([]);
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Composing, description: blank });
  });

  it('sends nothing for a second enter while starting, and ignores esc and typing', () => {
    const { calls, controller } = setup();
    controller.open();
    controller.edit('Fix');
    controller.create();
    controller.create();
    controller.cancel();
    controller.edit('other');
    expect(calls).toHaveLength(1);
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Starting, description: 'Fix' });
  });

  it('closes once the session has started', async () => {
    const { answers, controller } = setup();
    controller.open();
    controller.edit('Fix');
    controller.create();
    answers[0]?.(undefined);
    await flush();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
  });

  it('keeps the description and the reason when the session did not start, and sends again only on another create', async () => {
    const { calls, answers, controller } = setup();
    controller.open();
    controller.edit('Fix');
    controller.create();
    answers[0]?.('claude is not installed');
    await flush();
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Refused, description: 'Fix', reason: 'claude is not installed' });
    expect(calls).toHaveLength(1);
    controller.create();
    expect(calls).toHaveLength(2);
  });

  it('drops a refusal with esc, and editing after one goes back to composing', async () => {
    const { answers, controller } = setup();
    controller.open();
    controller.edit('Fix');
    controller.create();
    answers[0]?.('no');
    await flush();
    controller.edit('Fix it');
    expect(controller.state.get()).toMatchObject({ kind: CaptureStateKind.Composing, description: 'Fix it' });
    controller.create();
    answers[1]?.('again');
    await flush();
    controller.cancel();
    expect(controller.state.get().kind).toBe(CaptureStateKind.Closed);
  });
});
