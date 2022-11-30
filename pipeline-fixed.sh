#!/bin/bash
set -euo pipefail

export PREFIX=$HOME/src/europat-scripts
source $PREFIX/init.sh

export THREADS=${SLURM_CPUS_ON_NODE:-4}
export TMPSUF=${SLURM_JOB_ID:-$$}

THRESHOLD=0.5 # Was 0.05

export lang=$1
shift

bleualign-output() {
	pigz -cd bleualign-output/${lang^^}-????-bleualign-output.tab.gz

	# Also include all the data from the previous release
	# pigz -cd /beegfs/europat-release-2/${lang,,}-en.raw.gz
}

fixed=${lang^^}.fixed.gz

bleualign-output $* \
| cut -d$'\t' -f 1-4 \
| parallel -j $THREADS --pipe -k -l 60000 `# TODO is bifixer safe to run with parallel? It might be able to delete rows` \
	bifixer --ignore_long --aggressive_dedup - - ${lang,,} en \
| pypy $PREFIX/filter-unicode.py \
| pigz -c > $fixed.$TMPSUF \
&& mv $fixed{.$TMPSUF,}
