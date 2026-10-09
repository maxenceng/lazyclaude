# Ubiquitous language

The words below are the ones that appear in code. If a discussion introduces a new word
for an existing concept, either rename the concept everywhere or add the new word here —
never let two names for one thing coexist.

Keep this file short. A glossary nobody trusts is worse than none.

Add a section per bounded context, and add the term in the same change that introduces
it in code. A glossary updated in a separate pass documents what survived, not what the
words mean.

## <context>

| Term | Meaning | Where it lives |
|---|---|---|
| | | |

## Words we deliberately avoid

| Avoid | Use instead | Why |
|---|---|---|
| Helper, Util, Processor, Manager | A name describing what it does | These names hide missing concepts. |
| Issue, card, task | Ticket | The backlog's own word; `/ticket` and the files already use it. |
| Chat, conversation | Session | What Claude Code calls it, and what `claude --resume` resumes. |
