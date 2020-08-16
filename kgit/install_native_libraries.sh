#!/bin/bash

set -euxo pipefail

[[ ! -d dist/git24j ]] && git clone https://github.com/git24j/git24j.git dist/git24j
git -C dist/git24j/ submodule sync --recursive
git -C dist/git24j/ submodule update --init --recursive
make -C dist/git24j/
