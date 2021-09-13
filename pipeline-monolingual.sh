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

declare -a MODEL=("$1")
shift

while [[ $# > 0 ]] && [[ $1 != -- ]]; do
	MODEL=("${MODEL[@]}" "$1")
	shift
done
shift # remove --

# Check for software in path
which marian-decoder \
	b64filter \
	foldfilter \
	docenc \
	parallel

col () {
	cut -d$'\t' -f$1
}

document_to_base64() {
	# Remove trailing newline from input (to not cause an empty
	# document at the end), suffix each line with a null byte,
	# which will indicate where a document starts. Then inside each
	# document replace the paragraphs and br tags with newslines.
	# docenc will then group all of those into base64 encoded chunks.
	awk 'NR > 1 { print prev } { prev=$0 } END { ORS=""; print }' \
	| sed -r 's/$/\x0/g' \
	| sed -r 's/<br\/?>|<\/p><p>/\n/g' \
	| sed -r 's/<\/?p>//g' \
	| sed -r 's/@TAB@/ /g' \
	| docenc -0
}

preprocess() {
	if [ ! -z "${LOWERCASE:-}" ]; then
		echo "Preprocessing with lowercase" >&2
		lowercase
	else
		cat
	fi
}

split-sentences() {
	src/preprocess/moses/ems/support/split-sentences.perl \
		-q \
		-d \
		-b \
		-p nonbreaking_prefixes/nonbreaking_prefixes.$1 \
		-n \
		-k
}

duct-tape() {
	python3 ./duct-tape.py --base64
}

lowercase() {
	sed -e 's/./\L\0/g'
}

translate () {
	b64filter "$@"
}

for file in $*; do
	n=$(cat $file | wc -l)
	basename=${file##*/}
	language=${basename%%-*}
	echo "$(basename $file): $n documents" >&2

	cat $file \
	| col 3 \
	| preprocess \
	| document_to_base64 \
	| duct-tape \
	| split-sentences ${language,,} \
	| translate "${MODEL[@]}" \
	| paste \
		<(cat $file | col 1) \
		- \
	| pigz \
	> $(basename $file .tab)-en.tab.gz.$TMPSUF \
	&& mv $(basename $file .tab)-en.tab.gz{.$TMPSUF,}
done

