#!/bin/sh
set -eu

: "${RCLONE_CONFIG_SOURCE_ACCESS_KEY_ID:?Missing source access key}"
: "${RCLONE_CONFIG_SOURCE_SECRET_ACCESS_KEY:?Missing source secret key}"
: "${RCLONE_CONFIG_BACKUP_ACCESS_KEY_ID:?Missing backup access key}"
: "${RCLONE_CONFIG_BACKUP_SECRET_ACCESS_KEY:?Missing backup secret key}"

PUBLIC_BUCKET="${SOURCE_PUBLIC_BUCKET:-saltos-prod-public}"
PRIVATE_BUCKET="${SOURCE_PRIVATE_BUCKET:-saltos-prod-private}"
BACKUP_BUCKET="${DESTINATION_BUCKET:-saltos-prod-backup}"

if [ -n "${CLOUD_RUN_EXECUTION:-}" ]; then
  SNAPSHOT_ID="$CLOUD_RUN_EXECUTION"
else
  SNAPSHOT_ID="manual-$(date -u +%Y%m%dT%H%M%SZ)"
fi

DEST_BASE="${BACKUP_BUCKET}/snapshots/${SNAPSHOT_ID}"

echo "Starting R2 backup: ${SNAPSHOT_ID}"

echo "Backing up public media..."
rclone copy \
  "source:${PUBLIC_BUCKET}" \
  "backup:${DEST_BASE}/public" \
  --immutable \
  --log-level INFO

echo "Backing up private media..."
rclone copy \
  "source:${PRIVATE_BUCKET}" \
  "backup:${DEST_BASE}/private" \
  --immutable \
  --log-level INFO

echo "Checking public backup..."
rclone check \
  "source:${PUBLIC_BUCKET}" \
  "backup:${DEST_BASE}/public" \
  --one-way

echo "Checking private backup..."
rclone check \
  "source:${PRIVATE_BUCKET}" \
  "backup:${DEST_BASE}/private" \
  --one-way

echo "R2 backup completed successfully: ${SNAPSHOT_ID}"
