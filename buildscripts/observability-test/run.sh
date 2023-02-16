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

if [ "$1" = "server" ] ; then
  /grpc-java/interop-testing/build/install/grpc-interop-testing/bin/test-server --port=$2 --use_tls=false

elif [ "$1" = "client" ] ; then
  /grpc-java/interop-testing/build/install/grpc-interop-testing/bin/test-client \
    --server_host=$2 --server_port=$3 --use_tls=false --test_case=$5

else
  echo "Invalid action $1"
  exit 1
fi
