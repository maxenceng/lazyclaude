import { readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, relative, resolve, sep } from 'node:path';
import { describe, expect, it } from 'vitest';

const SRC = resolve(__dirname);
const CONTEXTS = ['board', 'session'];
const SHARED = ['protocol', 'design-system', 'shared', 'test', 'keys', 'connection'];

function sourceFiles(directory: string): string[] {
  return readdirSync(directory).flatMap((name) => {
    const path = join(directory, name);
    if (statSync(path).isDirectory()) {
      return sourceFiles(path);
    }
    return /\.(ts|tsx)$/.test(name) ? [path] : [];
  });
}

interface Import {
  file: string;
  from: string;
  target: string;
}

function importsOf(file: string): Import[] {
  const text = readFileSync(file, 'utf8');
  return [...text.matchAll(/(?:from|import)\s+['"](\.[^'"]*)['"]/g)].map((match) => {
    const from = relative(SRC, file);
    const target = relative(SRC, resolve(dirname(file), match[1] ?? ''));
    return { file: from, from: from.split(sep)[0] ?? '', target };
  });
}

const imports = sourceFiles(SRC)
  .filter((file) => !file.endsWith('architecture.test.ts'))
  .flatMap(importsOf);
const folderOf = (path: string) => path.split(sep)[0] ?? '';

describe('the frontend architecture', () => {
  it('lets only src/protocol import the generated files', () => {
    const offenders = imports.filter((entry) => entry.target.split(sep).includes('generated') && entry.from !== 'protocol');
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('reaches src/protocol only through its index', () => {
    const offenders = imports.filter((entry) => folderOf(entry.target) === 'protocol' && entry.target !== 'protocol' && entry.from !== 'protocol');
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('keeps one context from importing another', () => {
    const offenders = imports.filter((entry) => CONTEXTS.includes(entry.from) && CONTEXTS.includes(folderOf(entry.target)) && entry.from !== folderOf(entry.target));
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('keeps the design system free of contexts, the protocol and everything else', () => {
    const offenders = imports.filter((entry) => entry.from === 'design-system' && folderOf(entry.target) !== 'design-system');
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('keeps the shared folders free of contexts', () => {
    const offenders = imports.filter((entry) => SHARED.includes(entry.from) && CONTEXTS.includes(folderOf(entry.target)));
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });
});
