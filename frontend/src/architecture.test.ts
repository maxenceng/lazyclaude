import { readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, relative, resolve, sep } from 'node:path';
import { describe, expect, it } from 'vitest';
import { Action } from './keys/bindings';

const SRC = resolve(__dirname);
const CONTEXTS = ['board', 'docs', 'review', 'session'];
const UNHANDLED_ACTIONS: readonly string[] = ['Top', 'Bottom', 'RemoveWorktree'];
const SHARED = ['protocol', 'design-system', 'i18n', 'shared', 'test'];
const COMPOSITION = ['keys', 'connection', 'window'];
const TEST_FILE = /\.test\.tsx?$/;
const MARKDOWN_FRAMES = [join('design-system', 'Detail.tsx'), join('design-system', 'Document.tsx')];

const sourceFiles = (directory: string): string[] => {
  return readdirSync(directory).flatMap((name) => {
    const path = join(directory, name);
    if (statSync(path).isDirectory()) {
      return sourceFiles(path);
    }
    return /\.(ts|tsx)$/.test(name) ? [path] : [];
  });
};

interface Import {
  file: string;
  from: string;
  target: string;
}

const importsOf = (file: string): Import[] => {
  const text = readFileSync(file, 'utf8');
  return [...text.matchAll(/(?:from|import)\s+['"](\.[^'"]*)['"]/g)].map((match) => {
    const from = relative(SRC, file);
    const target = relative(SRC, resolve(dirname(file), match[1] ?? ''));
    return { file: from, from: from.split(sep)[0] ?? '', target };
  });
};

const imports = sourceFiles(SRC)
  .filter((file) => !file.endsWith('architecture.test.ts'))
  .flatMap(importsOf);
const folderOf = (path: string) => path.split(sep)[0] ?? '';
const atRoot = (target: string) => !target.includes(sep) && statSync(join(SRC, target), { throwIfNoEntry: false })?.isDirectory() !== true;

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

  it('keeps the design system free of contexts, the protocol and everything else but its copy', () => {
    const offenders = imports.filter((entry) => entry.from === 'design-system' && !['design-system', 'i18n'].includes(folderOf(entry.target)));
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('lets only a test read a stylesheet from disk, so node:fs stays out of the bundle', () => {
    const offenders = imports.filter((entry) => entry.target === join('design-system', 'stylesheet') && !TEST_FILE.test(entry.file));
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('reaches Markdown only through Detail and Document, whose frame its type and measure are scaled against', () => {
    const offenders = imports.filter(
      (entry) => entry.target === join('design-system', 'Markdown') && !TEST_FILE.test(entry.file) && !MARKDOWN_FRAMES.includes(entry.file),
    );
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('keeps the shared folders free of contexts', () => {
    const offenders = imports.filter((entry) => SHARED.includes(entry.from) && CONTEXTS.includes(folderOf(entry.target)));
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('keeps contexts and shared folders from importing a file at the root, all of which compose', () => {
    const offenders = imports.filter((entry) => [...CONTEXTS, ...SHARED].includes(entry.from) && atRoot(entry.target));
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('keeps contexts and shared folders from importing the composition side', () => {
    const offenders = imports.filter((entry) => [...CONTEXTS, ...SHARED].includes(entry.from) && COMPOSITION.includes(folderOf(entry.target)));
    expect(offenders.map((entry) => entry.file)).toEqual([]);
  });

  it('acts on every key the window binds, but the ones still waiting for a handler', () => {
    const consumers = sourceFiles(SRC)
      .filter((file) => !/\.test\.tsx?$/.test(file) && !file.endsWith(join('keys', 'bindings.ts')))
      .map((file) => readFileSync(file, 'utf8'))
      .join('\n');
    const unhandled = Object.keys(Action).filter((name) => !new RegExp(`\\bAction\\.${name}\\b`).test(consumers));
    expect(unhandled, 'a key the window binds and prevents, with nothing acting on it, does nothing when pressed').toEqual(UNHANDLED_ACTIONS);
  });
});
