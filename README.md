# Anka Build Cloud Teamcity Plugin

Connects Teamcity and [Anka Build Cloud](https://veertu.com/anka-build/).

Documentation can be found [HERE](https://docs.veertu.com/anka/plugins-and-integrations/controller-+-registry/teamcity/).


## Development

1. Install Maven (`brew install maven`), ensure openJDK is linked (`sudo ln -sfn /opt/homebrew/opt/openjdk/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk.jdk`) (maven installs it).
2. Run `mvn package` and ensure there aren't any failures.
    - If you see `Invalid CEN header` errors, manually replace the `aspectjweaver-1.8.9.jar` with the latest jar for that project, but keep the name the same.
3. This produces {repo root}/target/anka-build-cloud-teamcity-plugin-1.9.0.zip which you can then test