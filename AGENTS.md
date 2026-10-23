# Repository instructions – OttOSpiele ArrowPuzzle

## AI-assisted development provenance

When an AI assistant creates or materially modifies code, tests, assets, build configuration or documentation, the **same commit** MUST include provenance trailers in the Git commit message:

```text
<type>(<scope>): <concise description>

<what changed and why; tests and limitations>

AI-Generated: yes
AI-Provider: OpenAI
AI-Model: GPT-6
AI-Tool: ChatGPT (GitHub connector)
AI-Role: code generation, review support, tests, documentation
AI-User-Request: <brief faithful summary of relevant human request>
AI-Human-Review: not yet verified | reviewed by project maintainer
AI-Validation: <exact executed tests/build IDs, or not executed>
AI-Provenance-Policy: docs/AI_PROVENANCE.md
```

Always use the **actual** provider, model and tool available at generation time. Do not blindly label future work GPT-6 if the model differs. Do not claim human review or tests without evidence. Do not claim legal compliance solely from these trailers.

Older commits must **not** be rewritten merely to add AI metadata; see the README for historical disclosure.

## Maintenance with every change

- Update README requirements, implementation-status table and dated change log whenever features, requirements, user choices or limitations change.
- Record deviations from user requirements and unresolved work honestly.
- Preserve offline, ad-free, privacy-friendly intent for this children's app.
- Run game-core tests and Android CI where possible; state what was actually validated.
- Commit to develop, not main, unless explicitly instructed otherwise.
- Do not include verbatim private conversation transcripts, credentials, hidden reasoning or unrelated personal data in public commit descriptions.
