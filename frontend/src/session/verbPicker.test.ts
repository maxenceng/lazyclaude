import { describe, expect, it } from 'vitest';
import { VerbPicker } from './verbPicker';

describe('the verb picker over a ticket', () => {
  const picker = () => new VerbPicker('PROJ-1', 'proj-1', ['REFINE', 'START', 'REVIEW']);

  it('starts on the first verb', () => {
    expect(picker().selected()).toBe('REFINE');
  });

  it('opens on the verb session preselects', () => {
    expect(VerbPicker.opening('PROJ-1', 'proj-1', ['REFINE', 'START', 'REVIEW'], 'START').selected()).toBe('START');
  });

  it('moves down and up, held at the ends', () => {
    expect(picker().down().down().down().selected()).toBe('REVIEW');
    expect(picker().down().up().up().selected()).toBe('REFINE');
  });

  it('selects the verb a press lands on', () => {
    expect(picker().at(1).selected()).toBe('START');
  });

  it('carries the ticket and its branch to start the session with', () => {
    expect(picker().ticket).toBe('PROJ-1');
    expect(picker().branch).toBe('proj-1');
  });
});
