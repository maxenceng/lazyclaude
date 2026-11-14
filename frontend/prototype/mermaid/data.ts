// PROTOTYPE: canned documents for the mermaid variants. Not shipped.
export const GRAPH = `graph TD
    shell[shell<br/><i>TamboUI</i>]
    board[board]
    session[session]
    review[review]
    docs[docs]
    shell --> board
    shell -- "board's ticket ids" --> session
    shell --> review
    shell --> docs
    session -- "PR head branch, merged head" --> review
    session -- "Verb, Status, StatusVerbs" --> board`;

export const SEQUENCE = `sequenceDiagram
    participant W as Window
    participant R as Relay
    participant S as Sidecar
    participant G as gh
    W->>R: board.read
    R->>S: {"method":"board.read"}
    S->>G: gh issue list --json number,title,labels
    G-->>S: issues
    S-->>R: BoardView
    R-->>W: BoardView
    Note over S,G: every 10 s, ETag-conditional`;

export const BROKEN = `graph TD
    shell[shell --> board
    shell -- "board's ticket ids" --> session`;

export const DIAGRAMS = { graph: GRAPH, sequence: SEQUENCE, broken: BROKEN } as const;
export type DiagramKind = keyof typeof DIAGRAMS;

export const BEFORE = `## Relationships

How the contexts lean on one another. An arrow points at the context whose published package is read.
`;

export const AFTER = `- \`board\` knows nothing of sessions or pull requests. Who works a ticket is \`session\`'s question; how a ticket looks with its sessions is \`shell\`'s join.
- \`docs\` stands alone. The shell hands it the project root that \`board\` located (ADR 0015).
`;
