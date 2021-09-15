#!/bin/bash
set -euo pipefail
L1=$1
L2=en
MODEL=$(dirname $(realpath $0))/models/$L1-$L2
shift

foldfilter -s -w 250 marian-decoder \
	--models $MODEL/$L1$L2.base.npz.best-ce-mean-words.npz \
	--vocabs $MODEL/vocab.$L1$L2.base.spm{,} \
	--beam-size 6 \
	--normalize 0.6 \
	--mini-batch 64 \
	--maxi-batch 1000 \
	--maxi-batch-sort src \
	--workspace 12000 \
	--quiet-translation \
	-d ${CUDA_VISIBLE_DEVICES//,/ } \
	"$@"
