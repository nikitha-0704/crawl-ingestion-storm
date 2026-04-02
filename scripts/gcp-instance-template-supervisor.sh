#!/bin/bash
# =============================================================================
# GCP Instance template: Storm Supervisor + Logviewer (fk-3p-storm 2.2.x)
# Paste into: Instance templates → Management → Automation → Startup script
# Suggested instance template name (per lead): ci-storm-supervisor-stage
# Optional: attach a data disk and set DATA_DISK_DEVICE (e.g. /dev/sdb) in
# custom metadata or replace below for LVM on /var/lib/fk-3p-storm
#
# GCS buckets (same as Nimbus VM — per lead):
#   ci-storm-nimbus-stage -> STORM_YAML + CLUSTER_XML
#   ci-storm-supervisor-stage -> WORKER_XML
# =============================================================================

set -euo pipefail

REPOSERVICE_HOST="${REPOSERVICE_HOST:-10.24.0.41}"
REPOSERVICE_PORT="${REPOSERVICE_PORT:-8080}"
REPOSERVICE_ENV_NAME="${REPOSERVICE_ENV_NAME:-crawl-storm-ci-stage}"
REPOSERVICE_APPKEY="${REPOSERVICE_APPKEY:-crawl-storm-ci-stage}"

FK_3P_STORM_STORM_YAML_BUCKET="${FK_3P_STORM_STORM_YAML_BUCKET:-ci-storm-nimbus-stage}"
FK_3P_STORM_CLUSTER_XML_BUCKET="${FK_3P_STORM_CLUSTER_XML_BUCKET:-ci-storm-nimbus-stage}"
FK_3P_STORM_WORKER_XML_BUCKET="${FK_3P_STORM_WORKER_XML_BUCKET:-ci-storm-supervisor-stage}"

NSCA_TEAM_NAME="${NSCA_TEAM_NAME:-YOUR_OWNING_TEAM_NAME}"

DATA_DISK_DEVICE="${DATA_DISK_DEVICE:-}"
DATA_LV_SIZE_GB="${DATA_LV_SIZE_GB:-24}"

export DEBIAN_FRONTEND=noninteractive

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

if [[ ! -e /usr/bin/python ]]; then
  ln -sf /usr/bin/python3 /usr/bin/python
fi

umask 077
{
  echo "export FK_3P_STORM_STORM_YAML_BUCKET=${FK_3P_STORM_STORM_YAML_BUCKET}"
  echo "export FK_3P_STORM_CLUSTER_XML_BUCKET=${FK_3P_STORM_CLUSTER_XML_BUCKET}"
  echo "export FK_3P_STORM_WORKER_XML_BUCKET=${FK_3P_STORM_WORKER_XML_BUCKET}"
} > /etc/default/fk-3p-storm.env

apt-get install --yes --allow-unauthenticated fk-3p-storm-2.2.0

if [[ -n "${DATA_DISK_DEVICE}" && -b "${DATA_DISK_DEVICE}" ]]; then
  apt-get install --yes --allow-unauthenticated lvm2
  if ! pvs "${DATA_DISK_DEVICE}" &>/dev/null; then
    pvcreate -ff -y "${DATA_DISK_DEVICE}"
  fi
  if ! vgs vgroot &>/dev/null; then
    vgcreate vgroot "${DATA_DISK_DEVICE}"
  fi
  if ! lvs /dev/vgroot/lv_part1 &>/dev/null; then
    lvcreate -L "${DATA_LV_SIZE_GB}G" -n lv_part1 vgroot -y
  fi
  mkfs.ext4 -F /dev/mapper/vgroot-lv_part1
  install -d -m 0755 -o root -g root /var/lib/fk-3p-storm
  UUID="$(blkid -s UUID -o value /dev/mapper/vgroot-lv_part1)"
  if ! grep -q "${UUID}" /etc/fstab; then
    echo "UUID=${UUID} /var/lib/fk-3p-storm ext4 defaults,nofail 0 2" >> /etc/fstab
  fi
  mount -a
  chown fk-3p-storm:fk-3p-storm /var/lib/fk-3p-storm
fi

/etc/init.d/cosmos-jmx start
/etc/init.d/cosmos-collectd start

/etc/init.d/fk-3p-storm start supervisor
/etc/init.d/fk-3p-storm start logviewer

exit 0
