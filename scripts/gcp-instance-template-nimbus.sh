#!/bin/bash
# =============================================================================
# GCP INSTANCE TEMPLATE — Storm Nimbus + Storm UI (fk-3p-storm 2.2.x)
#
# Crawl Layer-1 / ci-storm (NOT pricing). Replace defaults below OR pass the
# same variable names as GCP custom metadata on the instance template.
#
# Use ONLY for the Nimbus (+ UI) VM. ZK and Supervisor use other scripts.
#
# GCP: Instance templates → Management → Automation → Startup script (paste all).
# Suggested instance template name (per lead): ci-storm-nimbus-stage
#
# GCS buckets (example project upst-explore-9988): two buckets → three FK_3P_STORM_* vars
#   ci-storm-nimbus-stage     -> STORM_YAML + CLUSTER_XML (same bucket for both)
#   ci-storm-supervisor-stage -> WORKER_XML
# Override via instance metadata if your project uses different names.
#
# Before first VM: ZK running; storm.yaml / cluster XML in the YAML bucket must
# list your ZK IP — ask platform for bucket layout and reposervice registration.
# =============================================================================

set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

meta_get() {
  local key="$1"
  curl -fs -H "Metadata-Flavor: Google" \
    "http://metadata.google.internal/computeMetadata/v1/instance/attributes/${key}" 2>/dev/null || true
}

for _k in REPOSERVICE_ENV_NAME REPOSERVICE_APPKEY REPOSERVICE_HOST REPOSERVICE_PORT \
  FK_3P_STORM_STORM_YAML_BUCKET FK_3P_STORM_CLUSTER_XML_BUCKET FK_3P_STORM_WORKER_XML_BUCKET \
  NSCA_TEAM_NAME; do
  _M="$(meta_get "${_k}")"
  [[ -n "${_M}" ]] && declare -g "${_k}=${_M}"
done

# --- Reposervice: register a crawl/ci-storm env with platform; use YOUR --name / --appkey ---
REPOSERVICE_HOST="${REPOSERVICE_HOST:-10.24.0.41}"
REPOSERVICE_PORT="${REPOSERVICE_PORT:-8080}"
REPOSERVICE_ENV_NAME="${REPOSERVICE_ENV_NAME:-crawl-storm-ci-stage}"
REPOSERVICE_APPKEY="${REPOSERVICE_APPKEY:-crawl-storm-ci-stage}"

# --- GCS buckets (lead: two bucket names; fk-3p-storm still exports three vars) ---
FK_3P_STORM_STORM_YAML_BUCKET="${FK_3P_STORM_STORM_YAML_BUCKET:-ci-storm-nimbus-stage}"
FK_3P_STORM_CLUSTER_XML_BUCKET="${FK_3P_STORM_CLUSTER_XML_BUCKET:-ci-storm-nimbus-stage}"
FK_3P_STORM_WORKER_XML_BUCKET="${FK_3P_STORM_WORKER_XML_BUCKET:-ci-storm-supervisor-stage}"

NSCA_TEAM_NAME="${NSCA_TEAM_NAME:-YOUR_OWNING_TEAM_NAME}"

log() { echo "[ci-crawl-storm-nimbus $(date -Iseconds)] $*"; }

log "Starting Nimbus bootstrap (crawl / ci-storm)"

apt-get update --allow-unauthenticated

apt-get install --yes --allow-unauthenticated fk-nagios-common fk-nsca-wrapper
apt-get install --yes --allow-unauthenticated cosmos-jmx cosmos-collectd
echo "team_name=${NSCA_TEAM_NAME}" > /etc/default/nsca_wrapper

echo "deb [trusted=yes] http://packages.reposvc-prod.fkcloud.in/repos/infra-cli/9 /" > /etc/apt/sources.list.d/infra-cli.list
apt-get update --allow-unauthenticated
apt-get install --yes --allow-unauthenticated infra-cli

reposervice --host "${REPOSERVICE_HOST}" --port "${REPOSERVICE_PORT}" env \
  --name "${REPOSERVICE_ENV_NAME}" --appkey "${REPOSERVICE_APPKEY}" \
  > /etc/apt/sources.list.d/storm.list

apt-get update --allow-unauthenticated

ln -sf /usr/bin/python3 /usr/bin/python

umask 077
{
  echo "export FK_3P_STORM_STORM_YAML_BUCKET=${FK_3P_STORM_STORM_YAML_BUCKET}"
  echo "export FK_3P_STORM_CLUSTER_XML_BUCKET=${FK_3P_STORM_CLUSTER_XML_BUCKET}"
  echo "export FK_3P_STORM_WORKER_XML_BUCKET=${FK_3P_STORM_WORKER_XML_BUCKET}"
} > /etc/default/fk-3p-storm.env

apt-get install --yes --allow-unauthenticated fk-3p-storm-2.2.0

/etc/init.d/cosmos-jmx start
/etc/init.d/cosmos-collectd start

log "Starting Nimbus and UI"
/etc/init.d/fk-3p-storm start nimbus
/etc/init.d/fk-3p-storm start ui

log "Nimbus bootstrap finished OK"

exit 0
