# Anka Build Cloud Teamcity Plugin

Connects Teamcity and [Anka Build Cloud](https://veertu.com/anka-build/).

Documentation can be found [HERE](https://docs.veertu.com/anka/plugins-and-integrations/controller-+-registry/teamcity/).

## Development

1. Install Maven 4.0.0 rc 5
2. Run `mvn package` and ensure there aren't any failures.
    - If you see `Invalid CEN header` errors, manually replace the `aspectjweaver-1.8.9.jar` with the latest jar for that project, but keep the name the same.
3. This produces {repo root}/target/anka-build-cloud-teamcity-plugin-1.11.0.zip which you can then test

- You can watch `logs/teamcity-clouds.log` to see all logs related to the plugin.

### Tests and lint

```bash
mvn test
mvn -pl server,agent,common -am checkstyle:check
mvn -pl server,agent,common -am install -DskipTests spotbugs:check
```

Unit tests run offline (no live TeamCity or Anka required). GitHub Actions runs the same checks on pull requests and pushes to `master` (see `.github/workflows/ci.yml`).

### Releasing

See [RELEASING.md](RELEASING.md). Prefer the **Release** GitHub Action: `workflow_dispatch` from `release/vX.X.X` (Stable Marketplace) or from `edge` (beta Marketplace).

<!-- ### Building with Docker

```
cd ${REPO_ROOT}
docker run -it --rm --name maven-builder -v "/tmp:/root/.m2" -v "$(pwd)":/usr/src/teamcity-repo -w /usr/src/teamcity-repo maven:3.8.6-openjdk-8 mvn clean package
``` -->
