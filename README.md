# easy-input-filter

[![Maven Central](https://img.shields.io/badge/Maven%20Central-0.1.0--SNAPSHOT-blue)](https://central.sonatype.com/)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=fabricehamanmbouwe_easy-input-filter&metric=alert_status)](https://sonarcloud.io/dashboard?id=fabricehamanmbouwe_easy-input-filter)
<!-- The Quality Gate badge becomes active once SONAR_TOKEN is configured in
     GitHub Settings > Secrets and variables > Actions and the first CI build runs. -->
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

**Stop letting your marketplace lose revenue to phone numbers hidden in product descriptions.**

A Spring Boot starter that filters and sanitizes free-text user input — phone numbers, emails,
URLs, forbidden keywords, HTML, repeated characters — using simple field annotations.
Zero boilerplate, zero manual controller code.

```java
public class ProductRequest {

    @NoPhone
    @NoEmail
    @NoUrl
    @NoKeywords({"whatsapp", "telegram"})
    private String description;
}
```

That's it. Any `@RequestBody` carrying this DTO is now automatically checked before your
controller method even runs.

---

## Free vs Pro

| Feature | Free | Pro |
|---|:---:|:---:|
| Phone / email / URL detection | ✅ | ✅ |
| Honeypot field (`@Honeypot`) | ✅ | ✅ |
| Webhook on detection | ✅ | ✅ |
| Supported languages | FR, EN | 12+ |
| Verified-user whitelist (`allowIfVerified`) | — | ✅ |
| Fuzzy matching (`fuzzyTolerance`) | — | ✅ |
| Rules stored in database (hot-reloadable) | — | ✅ |
| REST dashboard | — | ✅ |

[Get Pro →](https://easy-input-filter.io/pro)

---

## Why this exists

Marketplaces, freelance platforms, dating apps, classified ads sites — any product with a
free-text field — lose revenue when users exchange contact details to bypass the platform's
commission. Building a reliable filter (regex, false positives, obfuscation, performance) from
scratch takes days. This starter does it in one dependency.

## Quickstart

```xml
<dependency>
    <groupId>io.github.fabricehamanmbouwe</groupId>
    <artifactId>easy-input-filter-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Annotate any String field on a class used as `@RequestBody`:

```java
@NoPhone                                   // rejects phone-like numbers
@NoEmail                                   // rejects email addresses
@NoUrl                                     // rejects http(s) links AND wa.me / t.me bypass links
@NoKeywords({"whatsapp", "telegram"})      // rejects a custom keyword list (Aho-Corasick powered)
@NoHtml                                    // rejects/strips HTML markup
@Sanitize                                  // always cleans silently, never rejects
@MaxRepeatChar(4)                          // collapses "soooo gooood!!!!!" spam patterns
@AllowedChars("0-9+\\-\\s")                // whitelist-only character validation
@Honeypot                                  // bot trap — any non-empty value is rejected
private String description;
```

When a REJECT-strategy annotation matches, the request fails with a `400` and a
[RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) Problem Details body:

```json
{
  "type": "https://github.com/fabricehamanmbouwe/easy-input-filter/errors/input-rejected",
  "title": "Input rejected by easy-input-filter",
  "status": 400,
  "detail": "Detected a phone-like number: 06****78",
  "field": "description",
  "detector": "phone",
  "timestamp": "2026-04-06T10:15:00Z"
}
```

## Per-field strategy

Every annotation accepts a `strategy`:

| Strategy   | Behaviour                                                |
|------------|-----------------------------------------------------------|
| `REJECT`   | Throws, request fails with 400 (default)                 |
| `SANITIZE` | The matched portion is cleaned, request continues         |
| `WARN`     | Logged only, request continues unchanged                 |

```java
@NoPhone(strategy = FilterStrategy.WARN)
private String comment;
```

## Configuration

```yaml
easy-input-filter:
  enabled: true                 # master switch
  pro-info-url: https://easy-input-filter.io/pro
  phone-detection:
    enabled: true
    countries: [ FR, BE, CA ]   # documentation hint, detection itself stays generic
  keywords:
    locales: [ fr, en ]         # FR and EN built-in bypass-keyword lists (Free tier)
  webhook:
    enabled: false              # set true to receive HTTP POST on every detection
    url: https://example.com/hooks/easy-input-filter
    on-strategies: [ REJECT ]
```

Disable the whole library with `easy-input-filter.enabled=false` — zero runtime overhead.

## Architecture

```
easy-input-filter-core            pure Java filtering engine, zero Spring dependency
easy-input-filter-aop             Spring AOP aspect, intercepts @RequestBody
easy-input-filter-autoconfigure   Spring Boot auto-configuration + RFC 7807 exception mapping
easy-input-filter-spring-boot-starter   the single dependency you actually add
```

The core module has no Spring dependency on purpose — the same `FilterEngine` can be reused
standalone or in a future SaaS API without any change.

## Try it locally

**Step 1 — Build the starter** (from repo root):

```bash
mvn clean install
```

**Step 2 — Start the demo app** (from `example-app/`):

```bash
cd example-app
mvn spring-boot:run
```

The app listens on **:8080**. Run these two steps separately — do not paste them as a single block in PowerShell.

**Step 3 — Send a request**

*Linux / macOS:*

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"title":"Canape","description":"Contactez-moi au 06 12 34 56 78","internalNote":"  <b>VIP</b>  client  "}'
```

*Windows PowerShell* — `curl` is an alias for `Invoke-WebRequest` in PS; use `curl.exe` (real curl, built into Windows 10/11) or `Invoke-RestMethod`. Note: PS 5.1 throws on 4xx responses, so wrap the FAIL call in try/catch to read the JSON body:

```powershell
# FAIL — phone number detected (400). Wrap in try/catch to see the RFC 7807 body.
try {
    Invoke-RestMethod -Method POST http://localhost:8080/products `
      -ContentType "application/json" `
      -Body '{"title":"Canape","description":"Contactez-moi au 06 12 34 56 78","internalNote":"  <b>VIP</b>  client  "}'
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    [System.IO.StreamReader]::new($stream).ReadToEnd() | ConvertFrom-Json
}
```

→ `400` with `"detector": "phone"` in the response body.

```powershell
# PASS — clean description (200). No try/catch needed.
Invoke-RestMethod -Method POST http://localhost:8080/products `
  -ContentType "application/json" `
  -Body '{"title":"Canape","description":"Tres bon etat","internalNote":"  <b>VIP</b>  client  "}'
```

→ `200`, with `internalNote` sanitized to `"VIP client"`.

## Security

Dependencies are automatically scanned for known CVEs on every push to `main`
using [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/).
Any dependency with a CVSS score ≥ 7 (HIGH or CRITICAL) will fail the build.
Run the scan locally with:

```bash
mvn dependency-check:check -Psecurity -DossindexAnalyzerEnabled=false
```

The HTML report is written to `target/dependency-check-report/`.

## Roadmap (Pro tier)

- Rules stored in database, hot-reloadable without redeploy, multi-tenant
- Fuzzy matching (Levenshtein) to catch obfuscations like `ph0ne`, `wh4ts4pp`
- Risk scoring (0-100) combining multiple signals
- REST dashboard to manage rules without touching code
- IBAN / credit card detection for fintech compliance
- MCP server mode (stdio) — use easy-input-filter directly from Claude Code, Cline, or Cursor as an agent tool

## License

Apache License 2.0 — see [LICENSE](LICENSE).

## Author

[Fabrice Hamanmbouwe](https://github.com/fabricehamanmbouwe) — Senior Java / Spring Boot Developer.
