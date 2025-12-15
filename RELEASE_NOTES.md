# Release Notes — Proto Wrapper Maven Plugin v1.0.1

**Release Date:** December 15, 2025

## Overview

This patch release adds GitHub Actions CI/CD pipeline for automated building and testing across multiple Java versions.

## What's New

### GitHub Actions CI/CD

Automated continuous integration workflow that runs on every push and pull request:

```yaml
# .github/workflows/build.yml
- Builds and tests on Java 11, 17, and 21
- Automatically installs protoc compiler
- Caches Maven dependencies for faster builds
- Uploads build artifacts (JAR files)
```

#### Workflow Features

| Feature | Description |
|---------|-------------|
| Multi-version testing | Java 11, 17, 21 matrix build |
| Automatic protoc | Installs Protocol Buffers compiler |
| Maven caching | Faster subsequent builds |
| Artifact upload | JAR files available for download |
| Branch triggers | `master` and `develop` branches |

#### Build Status

The workflow runs automatically on:
- Push to `master` or `develop`
- Pull requests to `master` or `develop`

## Upgrade Guide

Simply update the version in your `pom.xml`:

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.0.1</version>
    <!-- ... -->
</plugin>
```

No breaking changes — full backward compatibility with v1.0.0.

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation (EN)](README.md)
- [Documentation (RU)](README.ru.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0
