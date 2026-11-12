// PROTOTYPE: fake pull requests and a fake review, since none is open; realistic for ticket 145's fix. Not shipped.

export type Ci = 'failed' | 'passed' | 'pending' | 'none';

export interface PullRequest {
  number: number;
  ticket: number;
  title: string;
  head: string;
  ci: Ci;
  comments: number;
  age: string;
}

export const PULL_REQUESTS: PullRequest[] = [
  { number: 217, ticket: 145, title: 'pty4j leaves a `/tmp/pty4j-linux-x86-64-*` directory behind on every run', head: '145-pty4j-tmpdir', ci: 'failed', comments: 3, age: '2h' },
  { number: 219, ticket: 146, title: 'A pty spawned with `setInitialColumns(100)` reports `stty size` as `0 0`', head: '146-pty-initial-size', ci: 'pending', comments: 1, age: '20m' },
  { number: 218, ticket: 188, title: 'A click on a status line lamp or count goes to its session', head: '188-lamp-click', ci: 'passed', comments: 0, age: '1d' },
  { number: 220, ticket: 213, title: 'External links do not work. They do not open the browser.', head: '213-external-links', ci: 'none', comments: 0, age: '5m' },
];

export const CI_WORD: Record<Ci, string> = { failed: 'failing', passed: 'passing', pending: 'running', none: 'no checks' };
export const CI_GLYPH: Record<Ci, string> = { failed: '✕', passed: '✓', pending: '●', none: '–' };

export type Change = 'A' | 'M' | 'D' | 'R';

export interface ChangedFile {
  path: string;
  change: Change;
  added: number;
  removed: number;
  threads: number;
  pending: number;
}

const JAVA = 'src/main/java/dev/maxenceng/lazyclaude/session/infrastructure/secondary/';

export const FILES: ChangedFile[] = [
  { path: `${JAVA}PtyNativeFolder.java`, change: 'A', added: 64, removed: 0, threads: 1, pending: 1 },
  { path: `${JAVA}Pty4jHost.java`, change: 'M', added: 9, removed: 4, threads: 0, pending: 0 },
  { path: 'src/main/java/dev/maxenceng/lazyclaude/Contexts.java', change: 'M', added: 3, removed: 1, threads: 0, pending: 0 },
  { path: 'src/test/java/dev/maxenceng/lazyclaude/session/infrastructure/secondary/PtyNativeFolderTest.java', change: 'A', added: 88, removed: 0, threads: 0, pending: 0 },
  { path: 'docs/adr/0101-pty4j-natives-live-in-the-cache-not-in-tmp.md', change: 'A', added: 31, removed: 0, threads: 0, pending: 0 },
  { path: 'docs/adr/README.md', change: 'M', added: 1, removed: 0, threads: 0, pending: 0 },
];

export interface Comment {
  author: string;
  body: string;
  age: string;
}

export interface Thread {
  id: string;
  file: number;
  line: number;
  resolved: boolean;
  pending: boolean;
  comments: Comment[];
}

export const THREADS: Thread[] = [
  {
    id: 't1',
    file: 0,
    line: 21,
    resolved: false,
    pending: false,
    comments: [
      { author: 'maxenceng', body: 'Does the lock survive a crash between `tryLock` and the extraction? A half-written folder would be reused by the next run.', age: '2h' },
      { author: 'claude', body: 'It writes into `natives.partial` and renames it once complete, so a crash leaves only the partial folder, which the next run deletes first.', age: '1h' },
      { author: 'maxenceng', body: 'Good — say that in the ADR too.', age: '40m' },
    ],
  },
  {
    id: 't2',
    file: 1,
    line: 86,
    resolved: true,
    pending: false,
    comments: [{ author: 'maxenceng', body: 'This property must be set before the first `PtyProcessBuilder` is built, not just before `start()`.', age: '3h' }],
  },
  {
    id: 'p1',
    file: 0,
    line: 28,
    resolved: false,
    pending: true,
    comments: [{ author: 'you', body: 'The warning should name the directory it tried, not only the reason: "pty4j natives stay in /tmp: ~/.cache/lazyclaude/pty4j is read-only".', age: 'now' }],
  },
];

export const PENDING_REVIEW = { summary: 'Lock looks right; one ask on the fallback warning.', count: 1 };

export type DiffRow =
  | { kind: 'file'; file: number }
  | { kind: 'hunk'; header: string }
  | { kind: 'line'; sign: '+' | '-' | ' '; old?: number; new?: number; text: string }
  | { kind: 'thread'; thread: string };

const added = (from: number, lines: string[]): DiffRow[] => lines.map((text, i) => ({ kind: 'line', sign: '+', new: from + i, text }));

export const DIFF: DiffRow[] = [
  { kind: 'file', file: 0 },
  { kind: 'hunk', header: '@@ -0,0 +1,64 @@' },
  ...added(14, [
    '/** Where pty4j extracts its natives: under the cache, once per version, locked while one process writes them. */',
    'public final class PtyNativeFolder {',
    '',
    '    private static final String PARTIAL = "natives.partial";',
    '',
    '    public NativeFolder resolve(final CacheRoot cache, final Pty4jVersion version) {',
    '        final Path folder = cache.path().resolve("pty4j").resolve(version.value());',
    '        try (FileChannel channel = FileChannel.open(folder.resolveSibling(version.value() + ".lock"), CREATE, WRITE); FileLock lock = channel.lock()) {',
  ]),
  { kind: 'thread', thread: 't1' },
  ...added(22, [
    '            if (Files.isDirectory(folder)) {',
    '                return new ExtractedFolder(folder);',
    '            }',
    '            deleteIfPresent(folder.resolveSibling(PARTIAL));',
    '            return new ExtractedFolder(extractInto(folder.resolveSibling(PARTIAL)).moveTo(folder));',
    '        } catch (final IOException failure) {',
    '            return new TmpFolder(new Reason("pty4j natives stay in /tmp: " + failure.getMessage()));',
  ]),
  { kind: 'thread', thread: 'p1' },
  ...added(35, ['        }', '    }']),
  { kind: 'file', file: 1 },
  { kind: 'hunk', header: '@@ -80,9 +80,14 @@ final class Pty4jHost implements HostPort {' },
  { kind: 'line', sign: ' ', old: 80, new: 80, text: '    @Override' },
  { kind: 'line', sign: ' ', old: 81, new: 81, text: '    public HostedSession spawn(final SpawnRequest request) {' },
  { kind: 'line', sign: '-', old: 82, text: '        final PtyProcessBuilder builder = new PtyProcessBuilder(request.command()).setDirectory(request.directory().toString());' },
  { kind: 'line', sign: '-', old: 83, text: '        builder.setInitialColumns(request.size().columns()).setInitialRows(request.size().rows());' },
  { kind: 'line', sign: '+', new: 82, text: '        final NativeFolder natives = nativeFolders.resolve(cache, Pty4jVersion.current());' },
  { kind: 'line', sign: '+', new: 83, text: '        natives.apply(System::setProperty, warnings::raise);' },
  { kind: 'line', sign: '+', new: 84, text: '        final PtyProcessBuilder builder = new PtyProcessBuilder(request.command())' },
  { kind: 'line', sign: '+', new: 85, text: '            .setDirectory(request.directory().toString())' },
  { kind: 'line', sign: '+', new: 86, text: '            .setInitialColumns(request.size().columns()).setInitialRows(request.size().rows());' },
  { kind: 'thread', thread: 't2' },
  { kind: 'line', sign: ' ', old: 84, new: 87, text: '        return new PtyHostedSession(builder.start(), request.id());' },
  { kind: 'line', sign: ' ', old: 85, new: 88, text: '    }' },
  { kind: 'hunk', header: '@@ -112,6 +117,7 @@ final class Pty4jHost implements HostPort {' },
  { kind: 'line', sign: ' ', old: 112, new: 117, text: '    private static String[] environment(final SpawnRequest request) {' },
  { kind: 'line', sign: '-', old: 113, text: '        return request.environment().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);' },
  { kind: 'line', sign: '+', new: 118, text: '        return request.environment().entrySet().stream()' },
  { kind: 'line', sign: '+', new: 119, text: '            .map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);' },
  { kind: 'line', sign: ' ', old: 114, new: 120, text: '    }' },
];

/** The diff line the cursor is on: the long `try (FileChannel …)` line, under which the open thread hangs. */
export const CURSOR_LINE = 21;

export const threadById = (id: string): Thread => THREADS.find((t) => t.id === id)!;
export const fileName = (path: string): string => path.slice(path.lastIndexOf('/') + 1);
export const fileDir = (path: string): string => path.slice(0, path.lastIndexOf('/') + 1);
