FROM ubuntu:24.04
RUN apt update && apt install --yes python3-pip python3.12-venv libffi-dev curl rsync sshpass vim tar jq git zip openjdk-21-jre

RUN chown -R 1000:1000 /opt

USER ubuntu
# since 24.04 error: externally-managed-environment, so we need to create a virtualenv
ENV VIRTUAL_ENV=/opt/venv3.12
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"
COPY requirements.txt /tmp/requirements.txt
RUN pip3 install -r /tmp/requirements.txt
RUN ansible-galaxy collection install community.general && ansible-galaxy collection install ansible.posix
RUN chown -R 1000:1000 /opt/venv3.12