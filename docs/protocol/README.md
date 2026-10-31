# The sidecar protocol

`protocol.schema.json` is the JSON Schema of every method's params and result, every input's params (a call that is never answered) and every notification's params that
`lazyclaude --sidecar` speaks. A frontend generates its TypeScript types from it. Each sealed View names its `kind` as a
discriminator, so a generated union narrows on it.

The protocol is code first (ADR 0050): the schema is generated from the Payloads and Views, by the sidecar's own
writer rules (ADR 0073), and is never edited by hand.

- `make protocol-schema` writes the file.
- `make test` fails when the committed file differs from a fresh generation, so a renamed View field shows up as a
  diff to commit.
