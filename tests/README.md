# TeamCity plugin live tests

Each directory named `1-test-*` is one live test. It has a `manifest.yaml`, one config file per host, and `test.bash`.

Put no passwords, tokens, registry addresses, or Buzyform paths in these files. The private runner injects those values.

Run `./tests/check-public-definition.bash` before you commit a definition change.