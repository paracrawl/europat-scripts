#!/bin/bash
#SBATCH --account t2-cs119-cpu
#SBATCH --nodes 1
#SBATCH --cpus-per-task 8
#SBATCH --time 24:00:00
#SBATCH --partition pascal
set -eou pipefail

export PREFIX=$HOME/src/europat-scripts
source $PREFIX/init.sh

# How many bleualign?
export THREADS=${SLURM_CPUS_ON_NODE:-4}
export TMPSUF=${SLURM_JOB_ID:-$$}

# Check for software in path
which \
	bleualign_cpp \
	foldfilter \
	docenc \
	parallel

col () {
	cut -d$'\t' -f$1
}

document-to-base64() {
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

split-sentences() {
	src/preprocess/moses/ems/support/split-sentences.perl \
		-d \
		-b \
		-l $1 \
		-p nonbreaking_prefixes/nonbreaking_prefix.$1 \
		-n \
		-k
}

duct-tape() {
	python3 ./duct-tape.py --base64
}

lowercase() {
	sed -e 's/./\L\0/g'
}

tab-to-space() {
	sed -e 's/\t/ /g'
}

align () {
	parallel \
		--halt 2 \
		--pipe \
		--roundrobin \
		--linebuffer \
		-j $THREADS \
		-N 1 \
		bleualign_cpp --bleu-threshold 0.2
}

lang=$1
shift

for year in $*; do
	# output of join:
	# 1: foreign_id
	# 2: foreign_family
	# 3: en_id
	# 4: en_family
	# 5: english text (raw with <br> and html)
	# 6: foreign text (ocr output, raw with <br>)
	# 7: translated text
	# 8: labels (deleted then)
	
	ocr_text=/lustre/home/dc007/efarrow/europat/ocr_text/${lang^^}-${year}-ocr.tab
	ocr_labels=./${lang^^}-${year}-ocr.labels.gz
	translated_text=./${lang^^}-${year}-ocr-en.tab.gz
	combined=./${lang^^}-${year}.tab.gz
	bleualign_input=./${lang^^}-${year}-bleualign-input.tab.gz
	bleualign_output=./${lang^^}-${year}-bleualign-output.tab.gz

	cat /beegfs/europat-family-pairs/${lang^^}-EN-${year}-ID.tab \
	| ./join-documents.py ./patents-index-cirrus.gz \
		$ocr_text:3 \
		$translated_text:2 \
		$ocr_labels:2 \
	| (fgrep -iv "\t__label__en " || true) \
	| col 1-7 \
	| pigz -c > $combined

	paste \
		<(pigz -cd $combined | col 1) \
		<(pigz -cd $combined | col 3) \
		<(pigz -cd $combined | col 6 | document-to-base64 | duct-tape | split-sentences ${lang,,}) \
		<(pigz -cd $combined | col 5 | ./clean-text-patent.py | document-to-base64 | split-sentences en) \
		<(pigz -cd $combined | col 7 | cat) \
		<(pigz -cd $combined | col 5 | ./clean-text-patent.py | document-to-base64 | split-sentences en) \
	| tee >(pigz -c > $bleualign_input) \
  | align \
  | pigz -c > $bleualign_output.$TMPSUF

  mv $bleualign_output{.$TMPSUF,}

done