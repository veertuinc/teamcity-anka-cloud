# TeamCity plugin live tests

Each directory named `1-test-*` is one live test. It has a `manifest.yaml` and `test.bash`. Put each host and its settings in the manifest. Shared calls live in `tests/helpers.bash`. A test sets `TEST_ID` and then calls those functions.

The template SSH password is `admin` in the manifest. Put no tokens, registry addresses, licenses, or Buzyform paths in these files. The private runner injects those values.

Run `./tests/check-public-definition.bash` before you commit a definition change.