# Releasing

## Preferred: GitHub Action

**Prerequisite:** repo secret `JETBRAINS_MARKETPLACE_TOKEN` — a JetBrains Marketplace [permanent token](https://plugins.jetbrains.com/author/account) (My Tokens). Uploads go to [Anka Build Cloud](https://plugins.jetbrains.com/plugin/33114-anka-build-cloud) (`pluginId` `33114`, XML ID `teamcity_anka-build-cloud-teamcity-plugin`) via the [Plugin Upload API](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html). Marketplace must allow that XML ID (update the listing if it still expects `teamcity_ankaCloud`).

Channel depends on the branch you run **Release** from:

| Branch | Marketplace channel | What the workflow does |
| --- | --- | --- |
| `edge` | **beta** | Builds the current `pom.xml` version and uploads to Marketplace beta (no bump, tag, GitHub Release, or merge). |
| `release/vX.X.X` | **Stable** | Full release: bump, tag, GitHub Release, Marketplace Stable, merge into `edge`. |

Install/update from beta requires adding `https://plugins.jetbrains.com/plugins/beta/list` as a custom plugin repository in TeamCity.

### Stable release (`release/vX.X.X`)

1. Create a branch named `release/vX.X.X` (example: `release/v1.12.0`) from the commit you want to ship.
2. Open the **Release** workflow in GitHub Actions and run `workflow_dispatch` **from that branch** (no version input — the branch name is the source of truth).
3. The workflow will:
   - Parse `X.X.X` from `release/vX.X.X`
   - Bump version files via `./scripts/bump-version.sh`
   - Commit the bump on the release branch
   - Create and push tag `vX.X.X`
   - Build `{repo root}/target/anka-build-cloud-teamcity-plugin-X.X.X.zip`
   - Publish a GitHub Release with that zip
   - Upload the same zip to JetBrains Marketplace (Stable)
   - Merge the release branch into `edge`

### Beta publish (`edge`)

1. Ensure `edge` has the version and code you want in Marketplace beta.
2. Open the **Release** workflow and run `workflow_dispatch` **from `edge`**.
3. The workflow will:
   - Read the version from root `pom.xml`
   - Build `{repo root}/target/anka-build-cloud-teamcity-plugin-X.X.X.zip`
   - Upload that zip to JetBrains Marketplace (`beta` channel)

## Manual fallback

1. Bump the plugin version in every hardcoded location:
   ```bash
   ./scripts/bump-version.sh <new-version>
   # Example: ./scripts/bump-version.sh 1.12.0
   ```
   This updates `pom.xml`, `server/pom.xml`, `agent/pom.xml`, `common/pom.xml`, `build/pom.xml`, `teamcity-plugin.xml`, and `README.md`. Changing only the root `pom.xml` is not enough — the build will still produce the old version.
2. Run `mvn package` and confirm `{repo root}/target/anka-build-cloud-teamcity-plugin-<version>.zip` was produced.
3. Test the zip on a TeamCity server, then tag `vX.X.X`, publish the release, and merge the release branch into `edge`.
