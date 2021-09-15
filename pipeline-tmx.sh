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
	| parallel -j $THREADS --pipe -k -l 30000 \
		bicleaner-classify-lite --score_only -q - - bicleaner-models/en-${lang,,}/en-${lang,,}.yaml
}

filter-by-score() {
	awk -F"\t" "\$7 >= $1"
}

filter-by-hash() {
	LC_ALL=C sort -S8G -t$'\t' -k5,5 -k6,6nr \
	| awk '$5 == last { skip } { last=$5; print; }'
}

ipc-metadata-files() {
	find /beegfs/europat-metadata -name "EN-????-Metadata.tab" -o -name "${lang^^}-????-Metadata.tab"
}

classified=${lang^^}.classified.gz
fixed=${lang^^}.fixed.gz
filtered=${lang^^}.filtered.gz
tmx=${lang^^}.tmx.gz
txt=${lang^^}.txt.gz
stats=${lang^^}.stats

if [ -f $stats ]; then
	cat $stats
else
	if [ -f $txt ]; then
		pigz -cd $txt
	else
		echo "Creating txt" >&2
		if [ -f $tmx ]; then
			pigz -cd $tmx
		else
			echo "Creating tmx" >&2
			if [ -f $filtered ]; then
				pigz -cd $filtered
			else
				echo "Creating filtered" >&2
				if [ -f $classified ]; then
					pigz -cd $classified
				else
					echo "Creating classified" >&2
					if test -f $fixed; then
						fixed_path=$fixed
					else
						bleualign-output $* \
						| cut -d$'\t' -f 1-4 \
						| parallel -j $THREADS --pipe -k -l 30000 `# TODO is bifixer safe to run with parallel? It might be able to delete rows` \
							bifixer --ignore_long --aggressive_dedup - - ${lang,,} en \
						| ./filter-unicode.py \
						| pigz -c > $fixed.$TMPSUF
						fixed_path=$fixed.$TMPSUF
					fi
					# no pipe between the two to make sure $fixed is not trailing behind a tee
					paste \
					  <(pigz -cd $fixed_path) `# 6 columns: 2 ids + 2 texts + hash + rank` \
					  <(pigz -cd $fixed_path | bicleaner-scores) `# 1 column: score` \
					| tee >(pigz -c > $classified.$TMPSUF)
				fi \
				| filter-by-score 0.05 \
				| filter-by-hash \
				| tee >(pigz -c > $filtered.$TMPSUF)
			fi \
			| src/tmxutil/tmxutil.py --verbose \
				--input-format tab \
				--input-languages ${lang,,} en \
				--input-columns source-document-1 source-document-2 text-1 text-2 bifixer-hash bifixer-rank bicleaner-score \
				--ipc $(ipc-metadata-files) \
				--ipc-group ipc-groups.tab \
			| tee >(pigz -c > $tmx.$TMPSUF) 
		fi \
		| src/tmxutil/tmxutil.py \
			--input-format tmx \
			--output-format tab \
			--output-languages ${lang,,} en \
		| cut -d$'\t' -f3-4 \
		| tee >(pigz -c > $txt.$TMPSUF)
	fi \
	| cut -d$'\t' -f1 \
	| wc -wl \
	| tee $stats.$TMPSUF
fi

for var in classified filtered fixed tmx txt stats; do
	if [ -f ${!var}.$TMPSUF ]; then
		mv ${!var}{.$TMPSUF,}
	fi
done
