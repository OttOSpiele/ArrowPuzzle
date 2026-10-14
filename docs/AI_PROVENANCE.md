# AI provenance and transparency

This project has used OpenAI ChatGPT (GPT-6) as a substantial implementation assistant since its initial prototype on 14 October 2026. Human product requests and technical decisions guided the work. Public commit history prior to introduction of this policy did not consistently contain AI attribution; the README retrospectively describes the known origin. Historical commits remain unchanged to retain trustworthy commit hashes.

## Commit metadata

Use the commit trailers defined in [AGENTS.md](../AGENTS.md) on each future materially AI-produced change. Include an accurate model identifier, description of AI contribution, user-request summary, human review status and tests actually executed. The AI-Act reference is context, not a certification.

## Regulatory scope

As of 14 October 2026 the European Commission's interpretation of AI Act Article 50 does not generally require individual AI-generated source-code commits to carry AI marking; source code is listed among excluded kinds of outputs for certain machine-readable labeling obligations. This project's practice is a voluntary provenance measure.

Sources:
- https://ai-act-service-desk.ec.europa.eu/de/ai-act/article-50
- https://digital-strategy.ec.europa.eu/en/faqs/transparency-obligations-under-article-50-ai-act

If the end-user product later includes generative or directly interactive AI, or AI-produced public media requiring disclosure, reassess applicable obligations before release.

## Scope of documentation

This documentation refers only to **OttOSpiele Arrow Puzzle**, not to other OttOSpiele projects. Keep both README and this policy current when implementation or relevant requirements change.
