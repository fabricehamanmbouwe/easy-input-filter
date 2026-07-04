# Contributing to easy-input-filter

Thank you for considering a contribution! Please read this document before opening an issue
or submitting a pull request.

## Development setup

**Requirements:** Java 21, Maven 3.9+

```bash
git clone https://github.com/fabricehamanmbouwe/easy-input-filter.git
cd easy-input-filter
mvn clean install          # build + run all tests
```

## Running the demo app

```bash
cd example-app
mvn spring-boot:run        # starts on http://localhost:8080
```

## Project structure

```
easy-input-filter-core            pure Java engine — no Spring dep, unit-tested
easy-input-filter-aop             Spring AOP aspect — integration tested
easy-input-filter-autoconfigure   Spring Boot auto-config + exception handler
easy-input-filter-spring-boot-starter  meta-module (no code, aggregates all above)
```

**Architectural constraint**: `easy-input-filter-core` must never depend on Spring,
the web layer, or any runtime container. Any new feature that requires Spring must live
in `easy-input-filter-aop` or `easy-input-filter-autoconfigure`.

## How to add a new detector

1. Create `FooDetector implements Detector` in `easy-input-filter-core/detector/`.
2. Create `@NoFoo` annotation in `easy-input-filter-core/annotation/`.
3. Add the dispatch case in `FilterEngine.applyAnnotations()`.
4. Add a `FooDetectorTest` in the core module.

## Code style

- Follow **Google Java Style Guide** (indentation 4 spaces, max line length 100).
- No wildcard imports.
- Every public type and method must have a Javadoc comment.
- All logger calls must use SLF4J placeholders (`{}`) — no string concatenation in log calls.

## Commit convention

Use the **Conventional Commits** format:

```
feat: add @NoIban annotation for fintech compliance
fix: fix false positive in PhoneDetector for 4-digit references
docs: update README with webhook configuration example
test: add edge-case tests for AllowedCharsDetector
```

## Running tests with coverage

```bash
mvn clean verify            # runs tests + JaCoCo coverage check (min 60% instruction coverage)
```

Coverage reports are generated in each module's `target/site/jacoco/index.html`.

## Submitting a pull request

1. Fork the repo and create a branch: `git checkout -b feat/my-feature`
2. Write tests before or alongside your code.
3. Run `mvn clean verify` — all tests must pass, coverage must not drop below 60%.
4. Push your branch and open a PR against `main`.
5. Describe *why* the change is needed in the PR description (not just *what* it does).

## Reporting security issues

Please **do not** open a public issue for security vulnerabilities.
Email [fabricehaman.mbouwe@gmail.com](mailto:fabricehaman.mbouwe@gmail.com) directly.
