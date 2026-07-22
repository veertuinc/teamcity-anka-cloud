# Releasing

1. Bump the plugin version in every hardcoded location:
   ```bash
   ./scripts/bump-version.sh <new-version>
   # Example: ./scripts/bump-version.sh 1.12.0
   ```
   This updates `pom.xml`, `server/pom.xml`, `agent/pom.xml`, `common/pom.xml`, `build/pom.xml`, `teamcity-plugin.xml`, and `README.md`. Changing only the root `pom.xml` is not enough — the build will still produce the old version.
2. Run `mvn package` and confirm `{repo root}/target/anka-build-cloud-teamcity-plugin-<version>.zip` was produced.
3. Test the zip on a TeamCity server, then tag/publish per your usual release process.
