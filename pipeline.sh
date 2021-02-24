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
	bleualign_cpp \
	foldfilter \
	docenc \
	pv \
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
	| sed -r 's/<br\/>|<\/p><p>/\n/g' \
	| sed -r 's/<\/?p>//g' \
	| docenc -0
}

lowercase() {
	sed -e 's/./\L\0/g'
}

translate () {
	b64filter "$@"
}

buffer () {
	local buffer=$1
	local lines=$2
	local name=$3

	pv -l -C -B $buffer -s $lines -c -N $name
}

buffer () {
	pv -lqCB $1
}

progress() {
	#pv -l -s $1 -c -N $2
	cat
}

align () {
	local file=$1
	gzip -cd $(basename $file .tab)-bleualign-input.tab.gz \
	| parallel \
		--halt 2 \
		--pipe \
		--roundrobin \
		--linebuffer \
		-j $THREADS \
		-N 1 \
		bleualign_cpp --bleu-threshold 0.2 \
	| gzip \
	> $(basename $file .tab)-aligned.gz.$TMPSUF
	mv $(basename $file .tab)-aligned.gz{.$TMPSUF,}
}

align_worker () {
	while read file; do
		if [ -z "$file" ]; then
			break
		fi

		echo aligning $file
		align $file
	done
	return 0
}

PIPE=$(mktemp -u)
mkfifo $PIPE
exec 3<>$PIPE
rm $PIPE

(align_worker <&3) &

for file in $*; do
	n=$(cat $file | wc -l)
	echo "model=${MODEL[@]}"
	echo "$(basename $file): $n documents"

	# Moved the translation column from paste out of the file substitution
	# into stdin of paste so any errors here (because here is the most
	# likely place for errors to happen) will cause the program to fail.
	if [ ! -f $(basename $file .tab)-bleualign-input.tab.gz ]; then
		cat $file \
		| col 2 \
		| lowercase \
		| document_to_base64 \
		| progress $n prep \
		| buffer 512M \
		| translate "${MODEL[@]}" \
		| progress $n translate \
		| paste \
			<(cat $file | col 1) \
			<(cat $file | col 3) \
			<(cat $file | col 2 | document_to_base64) \
			<(cat $file | col 4 | document_to_base64) \
			- \
			<(cat $file | col 4 | lowercase | document_to_base64 | buffer 512M) \
		| progress $n write \
		| gzip -9c \
		> $(basename $file .tab)-bleualign-input.tab.gz.$TMPSUF \
		&& mv $(basename $file .tab)-bleualign-input.tab.gz{.$TMPSUF,}
	fi \
	&& if [ ! -f $(basename $file .tab)-aligned.gz ]; then
		#align $file &
		echo $file >&3
	fi \
	|| true # In case of failure, do continue with the next file
done

echo "" >&3
exec 3>&-

echo "Waiting on alignment to finish"
wait
echo "Done."
