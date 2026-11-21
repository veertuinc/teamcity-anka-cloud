# Releasing

Releases ship directly from the `edge` branch. Do not create `release/vX.X.X` version branches.

## Preferred: Jenkins (`teamcity-plugin/edge`)

**Prerequisites:** Jenkins credentials configured on the controller:
- `engineering-manager-github-app` — GitHub App credential with `contents:write` on `veertuinc/teamcity-anka-cloud`.
- `jetbrains-marketplace-token` — JetBrains Marketplace [permanent token](https://plugins.jetbrains.com/author/account) (My Tokens).

Uploads go to [Anka Build Cloud](https://plugins.jetbrains.com/plugin/33114-anka-build-cloud) (`pluginId` `33114`, XML ID `teamcity_anka-build-cloud-teamcity-plugin`) via the [Plugin Upload API](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html). Marketplace must allow that XML ID (update the listing if it still expects `teamcity_ankaCloud`).

### Release steps

1. Bump the plugin version on `edge`:
   ```bash
   ./scripts/bump-version.sh <new-version>
   # Example: ./scripts/bump-version.sh 1.12.0
   ```
   Commit and push the version bump to `edge`.
2. Confirm that `CI / Test`, `CI / Checkstyle`, and `CI / SpotBugs` pass on `edge`.
3. Update the `teamcity-anka-cloud` submodule pointer in `veertuinc/teamcity-wrapper` on `edge` and push.
4. Open the Jenkins job `teamcity-plugin/edge` (backed by the `teamcity-wrapper` root `Jenkinsfile`), select **Build with Parameters**, check **`PUBLISH_RELEASE`**, and start the build.
5. After all live tests pass, Jenkins will:
   - Read the version from `teamcity-anka-cloud/pom.xml` on `edge`
   - Verify that `teamcity-anka-cloud` `HEAD` matches `origin/edge` and that tag `vX.X.X` does not already exist
   - Create and push annotated tag `vX.X.X`
   - Publish a GitHub Release `vX.X.X` with `target/anka-build-cloud-teamcity-plugin-X.X.X.zip`
   - Upload the same zip to JetBrains Marketplace (Stable)

## Manual fallback

1. Bump the plugin version on `edge` in every hardcoded location:
   ```bash
   ./scripts/bump-version.sh <new-version>
   # Example: ./scripts/bump-version.sh 1.12.0
   ```
   This updates `pom.xml`, `server/pom.xml`, `agent/pom.xml`, `common/pom.xml`, `build/pom.xml`, `teamcity-plugin.xml`, and `README.md`. Changing only the root `pom.xml` is not enough — the build will still produce the old version.
2. Run `mvn package` and confirm `{repo root}/target/anka-build-cloud-teamcity-plugin-<version>.zip` was produced.
3. Test the zip on a TeamCity server.
4. Commit and push the version bump to `edge`, create and push annotated tag `vX.X.X` on `edge`, publish the GitHub Release with the zip, and upload the zip to JetBrains Marketplace (Stable).

## Automation

The default branch is `edge`. Releases do not use `release/vX.Y.Z` branches. Follow the Preferred section.

1. Required checks must be green on the `edge` commit: `CI / Test`, `CI / Checkstyle`, `CI / SpotBugs`. `ci.yml` runs on pull requests and on push to `edge`.
2. Next security version is the latest release tag plus one patch.
3. Run `./scripts/bump-version.sh X.Y.Z`, commit, and push to `edge`.
4. Update the `teamcity-anka-cloud` submodule in `veertuinc/teamcity-wrapper` on `edge` and push.
5. Trigger Jenkins job `teamcity-plugin/edge` with `PUBLISH_RELEASE=true`.
6. Wait until the build finishes. On failure, stop and do not trigger again in the same run.
7. Jenkins uses the configured `engineering-manager-github-app` and `jetbrains-marketplace-token` credentials. Never paste tokens into logs or commits.
