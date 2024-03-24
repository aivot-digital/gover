#!/bin/bash
VERSION=$1
DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
# docker buildx create --name multi-arch-builder --use --bootstrap
docker buildx build \
  --push \
  --platform linux/arm64,linux/amd64 \
  --file Dockerfile-Backend \
  --build-arg buildDate=$DATE \
  --build-arg version=$VERSION \
  --tag ghcr.io/aivot-digital/gover:$VERSION . && \
docker buildx build \
  --push \
  --platform linux/arm64,linux/amd64 \
  --file Dockerfile-Staff \
  --build-arg buildDate=$DATE \
  --build-arg version=$VERSION \
  --tag ghcr.io/aivot-digital/gover-staff:$VERSION . && \
docker buildx build \
  --push \
  --platform linux/arm64,linux/amd64 \
  --file Dockerfile-Customer \
  --build-arg buildDate=$DATE \
  --build-arg version=$VERSION \
  --tag ghcr.io/aivot-digital/gover-customer:$VERSION .
