const CHUNK = 0x8000;

/** Bytes as the base64 text the sidecar's `pty/` messages carry. */
export const toBase64 = (bytes: Uint8Array): string => {
  const pieces: string[] = [];
  for (let at = 0; at < bytes.length; at += CHUNK) {
    pieces.push(String.fromCharCode(...bytes.subarray(at, at + CHUNK)));
  }
  return btoa(pieces.join(''));
};

export const fromBase64 = (text: string): Uint8Array => Uint8Array.from(atob(text), (character) => character.charCodeAt(0));

/** The UTF-8 bytes of what xterm.js's `onData` reports. */
export const bytesOfText = (text: string): Uint8Array => new TextEncoder().encode(text);

/** The bytes of what xterm.js's `onBinary` reports: one byte per character. */
export const binaryBytes = (text: string): Uint8Array => Uint8Array.from(text, (character) => character.charCodeAt(0));
