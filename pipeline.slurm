#!/bin/bash
#SBATCH --job-name=europat-tmx
#SBATCH --time=24:00:00
#SBATCH --partition=standard
#SBATCH --nodes 1
#SBATCH --cpus-per-task 16
#SBATCH --qos=standard
#SBATCH --account=ec166-guest
set -euo pipefail

export PREFIX=$HOME/src/europat-scripts
source $PREFIX/init.sh

export TF_CPP_MIN_LOG_LEVEL=1

export THREADS=${SLURM_CPUS_ON_NODE:-4}
export TMPSUF=${SLURM_JOB_ID:-$$}

THRESHOLD=0.5 # Was 0.05

export lang=$1
shift

filter-by-score() {
	awk -F"\t" "\$7 >= $1"
}

filter-by-hash() {
	LC_ALL=C sort \
		--parallel=$THREADS \
		--compress-program=pigz \
		--temporary-directory=$HOME/.tmp \
		-S4G -t$'\t' -k5,5 -k6,6nr \
	| awk -F$'\t' '$5 == last { next } { last=$5; print; }'
}

ipc-metadata-files() {
	find /beegfs/europat-metadata -name "EN-????-Metadata.tab" -o -name "${lang^^}-????-Metadata.tab"
}

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
				pigz -cd \
					/beegfs/europat-3/bicleaner-ai-only-release-2/${lang^^}.classified.gz \
					/beegfs/europat-3/bicleaner-ai-only-release-3-text/${lang^^}-new-text.classified.gz \
				| filter-by-score $THRESHOLD \
				| filter-by-hash \
				| tee >(pigz -c > $filtered.$TMPSUF) \
				&& mv $filtered{.$TMPSUF,}
			fi \
			| ./paste-pipe.py ./elrc-info.py 3 4 `# adds 8th to 13th column: ratio + 2 token counts + 2 info col + info general` \
			| pypy src/tmxutil/tmxutil.py --verbose \
				--input-format tab \
				--input-languages ${lang,,} en \
				--input-columns source-document-1 source-document-2 text-1 text-2 bifixer-hash bifixer-rank score-bicleaner ratio tokenCount-1 tokenCount-2 info-1 info-2 info \
				--ipc $(ipc-metadata-files) \
				--ipc-group ipc-groups.tab \
			| tee >(pigz -c > $tmx.$TMPSUF) \
			&& mv $tmx{.$TMPSUF,}
		fi \
		| pypy src/tmxutil/tmxutil.py \
			--input-format tmx \
			--output-format tab \
			--output-languages ${lang,,} en \
		| cut -d$'\t' -f3-4 \
		| tee >(pigz -c > $txt.$TMPSUF) \
		&& mv $txt{.$TMPSUF,}
	fi \
	| cut -d$'\t' -f1 \
	| wc -wl \
	| tee $stats.$TMPSUF \
	&& mv $stats{.$TMPSUF,}
fi
