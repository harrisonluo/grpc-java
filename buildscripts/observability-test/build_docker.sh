#!/bin/bash
# Copyright 2022 gRPC authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -ex
cd "$(dirname "$0")"/../..

IMAGENAME=grpc-observability/integration-testing/linux/java
TAG=1.50.0-dev
PROJECTID=`gcloud config get-value project`

echo Building ${IMAGENAME}:${TAG}

docker build --no-cache -t o11y/${IMAGENAME}:${TAG} -f ./buildscripts/observability-test/Dockerfile .

docker tag o11y/${IMAGENAME}:${TAG} gcr.io/${PROJECTID}/${IMAGENAME}:${TAG}

docker push gcr.io/${PROJECTID}/${IMAGENAME}:${TAG}
