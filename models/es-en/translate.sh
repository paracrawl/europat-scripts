#!/bin/bash
set -euo pipefail

foldfilter -s -w 500 marian-decoder \
	-c $(dirname $(realpath $0))/config.yml \
	-d ${CUDA_VISIBLE_DEVICES//,/ } \
	--quiet-translation \
	"$@"
