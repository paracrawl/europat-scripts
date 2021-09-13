#!/bin/bash
#SBATCH --account t2-cs119-gpu
#SBATCH --nodes 1
#SBATCH --cpus-per-task 8
#SBATCH --gres gpu:4
#SBATCH --time 24:00:00
#SBATCH --partition pascal
set -eou pipefail

export PREFIX=$HOME/src/europat-scripts
source $PREFIX/init.sh

# How many bleualign?
export THREADS=${SLURM_CPUS_ON_NODE:-4}
export TMPSUF=${SLURM_JOB_ID:-$$}

# Check for software in path
which fasttext

col () {
	cut -d$'\t' -f$1
}

for file in $*; do
	n=$(cat $file | wc -l)
	echo "$(basename $file): $n documents" >&2

	cat $file \
	| col 3 \
	| sed -r 's/<br\/?>|<\/p><p>/ /g' \
	| sed -r 's/<\/?p>//g' \
	| sed -r 's/@TAB@/ /g' \
	| fasttext predict lid.176.bin - 3 \
	| paste \
		<(cat $file | col 1) \
		- \
	| pigz \
	> $(basename $file .tab).labels.gz.$TMPSUF \
	&& mv $(basename $file .tab).labels.gz{.$TMPSUF,}
done

