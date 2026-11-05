import { describe, expect, it } from 'vitest';
import { binaryBytes, bytesOfText, fromBase64, toBase64 } from './bytes';

describe('the pty bytes on the wire', () => {
  it('writes bytes as base64 and reads them back', () => {
    const bytes = Uint8Array.from([0, 27, 91, 127, 128, 255]);
    expect(toBase64(bytes)).toBe('ABtbf4D/');
    expect(fromBase64('ABtbf4D/')).toEqual(bytes);
  });

  it('writes text as its UTF-8 bytes', () => {
    expect(toBase64(bytesOfText('é€'))).toBe('w6nigqw=');
  });

  it('reads the characters of a binary string, as xterm.js reports a mouse past column 223, as one byte each', () => {
    expect(binaryBytes('\x1b[M\xe9')).toEqual(Uint8Array.from([27, 91, 77, 233]));
  });

  it('carries more bytes than one argument list holds', () => {
    const bytes = new Uint8Array(300_000).fill(65);
    expect(fromBase64(toBase64(bytes))).toEqual(bytes);
  });
});
