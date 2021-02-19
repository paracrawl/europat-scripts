#!/bin/bash
set -euo pipefail

marian-decoder \
	-c $(dirname $(realpath $0))/config.yml \
	-d ${CUDA_VISIBLE_DEVICES//,/ }
	"$@"
