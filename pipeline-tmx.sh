#!/bin/bash
set -euo pipefail

export PREFIX=$HOME/src/europat-scripts
source $PREFIX/init.sh

export THREADS=${SLURM_CPUS_ON_NODE:-4}
export TMPSUF=${SLURM_JOB_ID:-$$}

lang=$1
shift

english-first() {
	awk -F"\t" 'BEGIN { OFS="\t" }{ print $2, $1, $4, $3 }'
}

bleualign-output() {
	for year in $*; do
		pigz -cd ${lang^^}-${year}-bleualign-output.tab.gz
	done

	# Also include all the data from the previous release
	pigz -cd /beegfs/europat-release-2/${lang,,}-en.raw.gz
}

bicleaner-scores() {
	english-first \
	| bicleaner-classify-lite \
		--score_only -q \
		- - bicleaner-models/en-${lang,,}/en-${lang,,}.yaml
}

filter-by-score() {
	awk -F"\t" '$7 >= 0.05'
}

filter-by-hash() {
	LC_ALL=C sort -S8G -t$'\t' -k5,5 -k6,6nr \
	| awk '$5 == last { skip } { last=$5; print; }'
}

bifixer_output=${lang^^}.fixed.gz
classified=${lang^^}.classified.gz
fixed=${lang^^}.fixed.gz
filtered=${lang^^}.filtered.gz
stats=${lang^^}.stats.txt
tmx=${lang^^}.tmx.gz
txt=${lang^^}.txt.gz

bleualign-output $* \
| cut -d$'\t' -f 1-4 \
| parallel -j $THREADS --pipe -k -l 30000 \
	bifixer --ignore_long --aggressive_dedup - - ${lang,,} en \
| ./filter-unicode.py \
| pigz -c > $fixed.$TMPSUF

paste \
  <(pigz -cd $fixed.$TMPSUF) `# 6 columns: 2 ids + 2 texts + hash + rank` \
  <(pigz -cd $fixed.$TMPSUF | bicleaner-scores) `# 1 column: score` \
| tee >(pigz -c > $classified.$TMPSUF) \
| cat `#filter-by-score 0.05"` \
| filter-by-hash \
| tee >(pigz -c > $filtered.$TMPSUF) \
| src/tmxutil/tmxutil.py \
	--input-format tab \
	--input-languages ${lang,,} en \
	--input-columns source-document-1 source-document-2 text-1 text-2 bifixer-hash bifixer-rank bicleaner-score \
	--ipc /beegfs/europat-metadata/*-Metadata.tab \
	--ipc-group ipc-groups.tab \
| tee >(pigz -c > $tmx.$TMPSUF) \
| src/tmxutil/tmxutil.py \
	--input-format tmx \
	--output-format tab \
	--output-languages en ${lang,,} \
| cut -d$'\t' -f3-4 \
| pigz -c > $txt.$TMPSUF

for var in classified filtered fixed tmx txt; do
	mv ${!var}{.$TMPSUF,}
done
