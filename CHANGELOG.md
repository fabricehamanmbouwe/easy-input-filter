# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-06-29

### Added

**Core filtering engine**
- `@NoPhone` — detects phone numbers (international E.164 + local formats) in free text
- `@NoEmail` — detects email addresses (RFC 5322 pragmatic subset)
- `@NoUrl` — detects HTTP(S) links and messaging deep-links (wa.me, t.me, m.me, signal.me, ...)
- `@NoKeywords({"word1", "word2"})` — Aho-Corasick multi-keyword matching, O(n) regardless of keyword count
- `@NoHtml` — detects and strips HTML via OWASP Java HTML Sanitizer
- `@MaxRepeatChar(n)` — detects/collapses spam patterns ("aaaaaaa", "!!!!!")
- `@AllowedChars("0-9+\\-\\s")` — whitelist-only character validation
- `@Sanitize` — always cleans silently (strip HTML + normalize whitespace), never rejects
- `@Honeypot` — bot-detection trap field: any non-empty value triggers immediate rejection
- Per-annotation `strategy`: `REJECT` (400), `SANITIZE` (clean + continue), `WARN` (log + continue)

**Spring Boot integration**
- `@RequestBody` DTO scanning via Spring AOP aspect
- `@RequestParam String` parameter scanning with filter annotations applied directly
- Fields inherited from superclasses are scanned
- RFC 7807 Problem Details exception handler (400 response)
- Spring Boot auto-configuration with `easy-input-filter.enabled` master switch
- `@ConditionalOnMissingBean` on all auto-configured beans (user beans take precedence)

**Global keyword detection (Free tier: FR + EN)**
- Built-in marketplace-bypass keyword lists for French and English
- Configurable via `easy-input-filter.keywords.locales: [fr, en]`
- Additional locales (ES, PT-BR, DE, ...) available in the Pro tier

**Webhook notifications**
- Fire-and-forget HTTP POST webhook on REJECT detection
- Zero-dependency: uses `java.net.http.HttpClient`
- JSON payload: `field`, `detector`, `message`, `timestamp`, `applicationName`
- Enabled via `easy-input-filter.webhook.enabled=true`

**Pro feature stubs (code present, inactive without licence)**
- `allowIfVerified` attribute on `@NoPhone`, `@NoEmail`, `@NoUrl`
- `fuzzyTolerance` attribute on `@NoKeywords`
- `ProFeatureGate` class — central gate, always returns `false` in Free tier
- `VerifiedUserContext` interface — extension point for host application

**Developer experience**
- SLF4J logging (no implementation bundled in core)
- Startup banner: version, active detectors, Pro features hint
- Spring configuration metadata generated (IDE autocompletion)
- JaCoCo coverage reporting (min 60% instruction coverage enforced)
- Maven release profile: sources JAR, Javadoc JAR, GPG signing, Sonatype Central Portal publishing
- GitHub Actions: CI (push/PR to main) + release pipeline (tag `v*`)

[Unreleased]: https://github.com/fabricehamanmbouwe/easy-input-filter/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/fabricehamanmbouwe/easy-input-filter/releases/tag/v0.1.0
