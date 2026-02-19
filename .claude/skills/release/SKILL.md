---
name: Release Please
description: This skill should be used when the user asks to "create a release", "bump version", "prepare a release", "write a conventional commit", "update version", "check release config", "commit changes", or mentions Release Please, versioning, or CHANGELOG. All commits in this project MUST follow conventional commit format. Provides guidance on the project's Release Please CI setup for Android/Gradle.
version: 0.1.0
---

# Release Please for Android/Gradle

This project uses [Release Please](https://github.com/googleapis/release-please) by Google for automated version management and GitHub Releases.

## How It Works

1. Push commits to `main` using **conventional commit** format
2. Release Please GitHub Action creates/updates a **Release PR** with version bump + CHANGELOG
3. When the Release PR is merged, a **GitHub Release** is created with a git tag

## Conventional Commits

**All commits** in this project must follow the [Conventional Commits](https://www.conventionalcommits.org/) format. This applies to every commit, not just release-related ones — Release Please reads the full commit history to determine version bumps and generate the CHANGELOG:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types and Their Effect on Version

| Type | Version Bump | Example |
|------|-------------|---------|
| `fix:` | Patch (0.0.x) | `fix: resolve crash on stream extraction timeout` |
| `feat:` | Minor (0.x.0) | `feat: add sleep timer with preset durations` |
| `feat!:` or `BREAKING CHANGE:` | Major (x.0.0) | `feat!: redesign playback service API` |
| `chore:`, `docs:`, `ci:`, `refactor:`, `test:`, `style:` | No bump | `docs: update README with cast instructions` |

### Scope Examples (Optional)

```
feat(playback): add audio quality selection
fix(ui): correct sleep timer countdown display
refactor(data): simplify stream repository caching
```

## Project Configuration

### Files

| File | Purpose |
|------|---------|
| `release-please-config.json` | Release Please configuration (release type, extra files) |
| `.release-please-manifest.json` | Current version tracker |
| `.github/workflows/release.yml` | GitHub Actions workflow |
| `app/build.gradle.kts` | Contains `x-release-please-version` annotation |

### Version Syncing to Gradle

The `app/build.gradle.kts` uses an `x-release-please-version` annotation on `versionName`. Release Please updates this string automatically.

`versionCode` is derived from `versionName` at build time:
- Formula: `major * 10000 + minor * 100 + patch`
- Example: `1.2.3` → `10203`

No external sync scripts are required.

### Release Type

This project uses `release-type: "simple"` since it is not an npm package. The `simple` type:
- Reads conventional commits
- Updates `CHANGELOG.md`
- Updates version in `.release-please-manifest.json`
- Updates annotated files via `extra-files`

## Common Tasks

### Creating a Release

No manual action required. Push conventional commits to `main` and Release Please handles the rest:

1. Check for an open Release PR in the repository
2. Review the auto-generated CHANGELOG entries
3. Merge the Release PR to trigger a GitHub Release

### Manually Bumping Version

To force a specific version, edit `.release-please-manifest.json`:

```json
{
  ".": "2.0.0"
}
```

### Adding Files for Version Updates

To have Release Please update version strings in additional files, add entries to `release-please-config.json` under `extra-files`:

```json
{
  "extra-files": [
    {
      "type": "generic",
      "path": "path/to/file"
    }
  ]
}
```

Then add `// x-release-please-version` annotation to the version line in that file.

## CI Build and Publish

When a Release PR is merged and a GitHub Release is created, the `build` job automatically:

1. Checks out the tagged code
2. Sets up JDK 21 (Temurin)
3. Decodes the keystore from `KEYSTORE_BASE64` secret
4. Builds a signed release APK via `./gradlew assembleRelease`
5. Uploads `app-release.apk` to the GitHub Release

### Required GitHub Secrets

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded `.jks` keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias within the keystore |
| `KEY_PASSWORD` | Key password |

### Generating the Base64 Secret

```bash
base64 -i my-release-key.jks | pbcopy   # macOS — copies to clipboard
base64 -w 0 my-release-key.jks          # Linux — outputs to stdout
```

Then paste the output into GitHub → Settings → Secrets → `KEYSTORE_BASE64`.

### Signing Config

`app/build.gradle.kts` conditionally reads signing from environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Locally without env vars, release builds are unsigned. In CI, the workflow provides these variables.

## Troubleshooting

### Release PR Not Created

- Verify commits follow conventional commit format
- Check that the workflow has `contents: write` and `pull-requests: write` permissions
- Ensure `baseBranch` in config matches the actual default branch

### Version Not Updated in Gradle

- Verify `x-release-please-version` annotation exists on the `versionName` line
- Check that `app/build.gradle.kts` is listed in `extra-files` in `release-please-config.json`
