# TeamCity plugin live tests

Each directory named `<number>-test-*` is one live test. It has a `manifest.yaml` and `test.bash`. Put each host and its settings in the manifest. Shared calls live in `tests/helpers.bash`. A test sets `TEST_ID` and then calls those functions.

The template SSH password is `admin` in the manifest. Put no tokens, registry addresses, licenses, or Buzyform paths in these files. The private runner injects those values.

| Directory | Pass condition |
| --- | --- |
| `1-test-agent-connects` | The agent is connected and authorized. |
| `2-test-build-stops-vm` | One build succeeds, the profile fields match, and TeamCity removes the VM. |
| `3-test-max-instances` | Two builds are queued and only one VM starts. |
| `4-test-bad-ssh` | A bad password and a bad port remove the VM. A blocked agent path leaves the VM up until the script stops it. |
| `5-test-missing-template` | The template list and the tag list do not contain the missing names. |

Run `./tests/check-public-definition.bash` before you commit a definition change.
