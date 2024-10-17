# Anka Build Cloud Teamcity Plugin

Connects Teamcity and [Anka Build Cloud](https://veertu.com/anka-build/).

Documentation can be found [HERE](https://docs.veertu.com/anka/plugins-and-integrations/controller-+-registry/teamcity/).

## Development

### Building

- Must be built with maven 3 and jdk 8

```
cd ${REPO_ROOT}
docker run -it --rm --name maven-builder -v "/tmp:/root/.m2" -v "$(pwd)":/usr/src/teamcity-repo -w /usr/src/teamcity-repo maven:3.8.6-openjdk-8 mvn clean package
```

