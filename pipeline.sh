#!/bin/bash
set -eou pipefail

# How many bleualign?
THREADS=6

ROOT=$(dirname $(realpath $0))

# Add the local bin to my path plz
export PATH=$ROOT/bin:$PATH

# Note subword nmt is not installed in bin, you'll have to
# pip3 install it yourself at the moment
export BPE_BIN="subword-nmt apply-bpe"
export MARIAN_BIN=marian-decoder

export SLANG=$1
shift

export GPU=$1
shift

case $SLANG in
	de)
		MODEL="$ROOT/DC01311513/nmt/v1/translate.sh"
		;;
	fr)
		MODEL="$ROOT/DC01311514/nmt/v1/translate.sh"
		;;
	dummy)
		SLANG=de
		MODEL="$ROOT/debug-model/translate.sh"
		;;
	*)
		echo "Supported languages: de, fr" 1>&2
		exit 1
		;;
esac

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

preprocess () {
	b64filter java -jar $ROOT/bin/classbase.jar \
		-C  $ROOT/resource/classbase.json \
		-I /dev/stdin \
		-L ${1^^} \
		-S
}

translate () {
	b64filter foldfilter -w 1000 $MODEL \
		--quiet \
		--beam-size 6 \
		--normalize 1 \
		--mini-batch 64 \
		--maxi-batch 1000 \
		--mini-batch-words 4000 \
		--maxi-batch-sort src \
		--workspace 8000
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
	pv -l -s $1 -c -N $2
}

align () {
	local file=$1
	gzip -cd $(basename $file .tab)-bleualign-input.tab.gz \
	| parallel \
		--halt 2 \
		--pipe \
		--roundrobin \
		-j $THREADS \
		-N 1 \
		bleualign_cpp --bleu-threshold 0.2 \
	| gzip \
	> $(basename $file .tab)-aligned.gz
}

for file in $*; do
	n=$(cat $file | wc -l)
	echo "$(basename $file): $n documents"
	paste \
		<(cat $file | col 1) \
		<(cat $file | col 3) \
		<(cat $file | col 2 | document_to_base64) \
		<(cat $file | col 4 | document_to_base64) \
		<(cat $file | col 2 | document_to_base64 | preprocess $SLANG | progress $n prep-$SLANG | buffer 512M | translate | progress $n translate) \
		<(cat $file | col 4 | document_to_base64 | preprocess en | progress $n prep-en | buffer 512M) \
	| progress $n write \
	| gzip -9c \
	> $(basename $file .tab)-bleualign-input.tab.gz
	
	# Run align in the background we dont want to wait for it
	align $file &
done

echo "Waiting on alignment to finish"
wait
echo "Done."
