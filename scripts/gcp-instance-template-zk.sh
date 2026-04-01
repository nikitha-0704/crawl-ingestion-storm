#!/bin/bash
# =============================================================================
# GCP Instance template: ZooKeeper node (stage). No fk-3p-storm.
# Paste into: Instance templates → Management → Automation → Startup script
# Suggested template name: e.g. crawl-storm-stage-zk
# =============================================================================

set -euo pipefail

NSCA_TEAM_NAME="${NSCA_TEAM_NAME:-PLACEHOLDER_YOUR_TEAM_NAME}"

export DEBIAN_FRONTEND=noninteractive

apt-get update --allow-unauthenticated

apt-get install --yes --allow-unauthenticated fk-nagios-common fk-nsca-wrapper
apt-get install --yes --allow-unauthenticated cosmos-jmx cosmos-collectd
echo "team_name=${NSCA_TEAM_NAME}" > /etc/default/nsca_wrapper

apt-get update --allow-unauthenticated
if apt-cache show zookeeperd &>/dev/null; then
  apt-get install --yes zookeeperd
elif apt-cache show zookeeper &>/dev/null; then
  apt-get install --yes zookeeper
else
  echo "No standard zookeeper package — install ZK per your platform runbook." >&2
  exit 1
fi

/etc/init.d/cosmos-jmx start
/etc/init.d/cosmos-collectd start

if [[ -x /etc/init.d/zookeeper ]]; then
  /etc/init.d/zookeeper start
fi

exit 0
