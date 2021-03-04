#!/bin/bash
#SBATCH --account t2-cs119-gpu
#SBATCH --nodes 1
#SBATCH --cpus-per-task 8
#SBATCH --gres gpu:4
#SBATCH --time 24:00:00
#SBATCH --partition pascal
set -eou pipefail

# Args:
#	translation-script
#	--
#	
# USAGE
# >>> pipeline.sh es path/to/models/translate.sh -- paired_data/en-es/ES-EN-${YEAR}-*.tab
#
export PREFIX=$HOME/sw/europat-scripts
source $PREFIX/init.sh
export LANGUAGE="${1:-es}"
shift
# How many bleualign?
export THREADS=${SLURM_CPUS_ON_NODE:-4}
export TMPSUF=${SLURM_JOB_ID:-$$}

declare -a MODEL=("$1")
shift

while [[ $# > 0 ]] && [[ $1 != -- ]]; do
	MODEL+=("$1")
	shift
done
shift # remove --

# Check for software in path
which marian-decoder \
	b64filter \
	bleualign_cpp \
	foldfilter \
	docenc \
	process_unicode \
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
	| sed -e :a -e '/^\n*$/{$d;N;};/\n$/ba' \
	| docenc -0
}

preprocess() {
#	if [ ! -z "${LOWERCASE:-}" ]; then
#		echo "Preprocessing with lowercase" >&2
		lowercase
#	else
#		cat
#	fi
}
simplify(){
#	if [ -z "${REMOVE_PUNCTUATION:-}" ]; then
#		echo "Removing punctuation" >&2
		remove_punctuation
#	else
#		cat
#	fi
}
lowercase() {
	sed -e 's/./\L\0/g'
}
lowercase_kenneth() {
	if [ "$LANGUAGE" != "pl" ]; then
		process_unicode -l "$LANGUAGE" --lower #--flatten --normalize --lower
	else
		sed -e 's/./\L\0/g'
	fi
}
remove_punctuation(){
	# use sed to avoid removing \n and/or <br></br>
	sed 's/[].!?,|~'\''"()[&£$@%^*]//g'
	#tr -d '[:punct:]'
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
	> $(basename $file .tab)-aligned.gz.$TMPSUF \
	&& mv $(basename $file .tab)-aligned.gz{.$TMPSUF,}
}

align_worker () {
	set -euo pipefail
	echo Started align worker >&2
	while read file; do
		if [ -z "$file" ]; then
			echo "Received stop signal" >&2
			break
		fi
		echo "Aligning $file" >&2
		if ! align $file ; then
			echo "Aligning $file failed!" >&2
		fi
	done
	echo Align worker done >&2
	return 0
}

if [ "${ALIGN_IN_BACKGROUND:-1}" -gt 0 ]; then
	# Set up fifo pipe used as file queue
	pipe=$(mktemp -u)
	mkfifo $pipe
	exec 3<>$pipe
	rm $pipe
	
	# Start worker
	align_worker <&3 &
	PID=$!
	echo Started align worker $PID >&2

	# make sure we kill align worker when we get asked to stop.
	trap "kill -9 -- $PID \$(ps -o pid --no-headers --ppid $PID)" INT TERM

	# Call that's used when a file is ready to be aligned
	align_queue () {
		echo "Queueing $1" >&2
		echo $1 >&3
	}

	align_join() {
		# Send stop signal to worker
		echo "" >&3
		exec 3>&-
		# Wait for worker to exit
		echo "waiting on alignment to finish on $PID"
		wait $PID
	}
else
	# Just align immediately here and now
	align_queue () {
		echo "Aligning $1" >&2
		align $1
	}

	align_join() {
		:
	}
fi


for file in $*; do
	echo "model=${MODEL[@]}"

	# moved the translation column from paste out of the file substitution
	# into stdin of paste so any errors here (because here is the most
	# likely place for errors to happen) will cause the program to fail.
	if [ ! -f $(basename $file .tab)-translated.gz ]; then

		cat $file \
		| col 2 \
		| preprocess \
		| document_to_base64 \
		| tee >(gzip -c > $(basename $file .tab)-translate-input.gz) \
		| buffer 512m \
		| translate "${MODEL[@]}" \
		| gzip -9c \
		> $(basename $file .tab)-translated.gz.$TMPSUF \
		&& mv $(basename $file .tab)-translated.gz{.$TMPSUF,}
	else
		echo "$file already translated" >&2
	fi &&
	if [ ! -f $(basename $file .tab)-bleualign-input.tab.gz ]; then
		n=$(cat $file | wc -l)
		echo "$(basename $file): $n documents"
		zcat $(basename $file .tab)-translated.gz \
		| docenc -d \
		| preprocess \
		| simplify \
		| sed -e :a -e '/^\n*$/{$d;N;};/\n$/ba' \
		| document_to_base64 \ # base64 \ #docenc \
		| paste \
			<(cat $file | col 1) \
			<(cat $file | col 3) \
			<(cat $file | col 2 | document_to_base64) \
			<(cat $file | col 4 | document_to_base64) \
			- \
			<(cat $file | col 4 | preprocess | simplify | document_to_base64 | buffer 512m) \
		| gzip -9c \
		> $(basename $file .tab)-bleualign-input.tab.gz.$TMPSUF \
		&& mv $(basename $file .tab)-bleualign-input.tab.gz{.$TMPSUF,}
	else
		echo "$file already has bleualign input file" >&2
	fi &&
	if [ ! -f $(basename $file .tab)-aligned.gz ]; then
		if [ "${ALIGN:-1}" -gt 0 ]; then
			align_queue $file
		fi
	else
		echo "$file already aligned" >&2
	fi || true # in case of failure, do continue with the next file
done

align_join

echo "Done" >&2
