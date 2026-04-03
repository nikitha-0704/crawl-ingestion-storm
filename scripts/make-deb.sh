#!/bin/bash -ex
# Build .deb for crawl-ingestion-storm (CrawlEnrichmentTopology + confd).
# Run from repo root after: mvn -q clean package
#
# Usage: ./scripts/make-deb.sh <stage|prod|...>   # replaces _HOST_ENV_ in postinst

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PACKAGE=crawl-ingestion-storm
TARGET="${1:-stage}"

function die() {
    echo "ERROR: $1" >&2
    exit 1
}

[ -f "$ROOT/target/${PACKAGE}-1.0-SNAPSHOT.jar" ] || die "Run mvn package first (expected target/${PACKAGE}-1.0-SNAPSHOT.jar)"

replacePlaceHolders() {
    file="$1"
    sed -i '' -e "s/_PACKAGE_/$PACKAGE/g" "$file"
    sed -i '' -e "s/_HOST_ENV_/$TARGET/g" "$file"
}

DEB_DIR="$ROOT/deb"
rm -rf "$DEB_DIR"
mkdir -p "$DEB_DIR"
cp -R "$ROOT/debian/"* "$DEB_DIR/"

LIB_DIR="$DEB_DIR/usr/share/$PACKAGE/lib/"
mkdir -p "$LIB_DIR"
cp "$ROOT/target/${PACKAGE}-1.0-SNAPSHOT.jar" "$LIB_DIR"

mkdir -p "$DEB_DIR/etc/default"

replacePlaceHolders "${DEB_DIR}/DEBIAN/postinst"
replacePlaceHolders "${DEB_DIR}/DEBIAN/control"
replacePlaceHolders "${DEB_DIR}/DEBIAN/prerm"
replacePlaceHolders "${DEB_DIR}/DEBIAN/postrm"
replacePlaceHolders "${DEB_DIR}/usr/share/$PACKAGE/bin/$PACKAGE"
replacePlaceHolders "$DEB_DIR/etc/default/$PACKAGE"

curr_version=$(date +%si)
sed -i '' -e "s/_VERSION_/${curr_version}/g" "${DEB_DIR}/DEBIAN/control"

dpkg-deb -b "$DEB_DIR" "$ROOT/${PACKAGE}.deb"
echo "Wrote $ROOT/${PACKAGE}.deb"
