#!/bin/bash
set -euo pipefail

PREFIX=$(dirname $(realpath $0))
BINDIR=$PREFIX/bin

mkdir -p $BINDIR

needs_building () {
	for file in $@; do
		if [ ! -f $BINDIR/$file ]; then
			return 0
		fi
	done
	return 1
}

if needs_building classbase.jar; then
	echo "Building classbase"
	pushd src/classbase
	mvn package
	install target/ClassBaseEncoding-0.0.1-SNAPSHOT-jar-with-dependencies.jar $BINDIR/classbase.jar
	popd
fi

if needs_building foldfilter b64filter docenc; then
	echo "Building b64filter, foldfilter and docenc"
	mkdir -p build/doctools
	pushd build/doctools
	cmake \
		-DCMAKE_BUILD_TYPE=Release \
		-DPREPROCESS_PATH=$PREFIX/src/preprocess \
		$PREFIX/src/bitextor/document-aligner
	make foldfilter b64filter docenc
	install bin/* $BINDIR
	popd
fi

if needs_building bleualign_cpp; then
	echo "Building bleualign"
	mkdir -p build/bleualign
	pushd build/bleualign
	cmake \
		-DCMAKE_BUILD_TYPE=Release \
		-DPREPROCESS_PATH=$PREFIX/src/preprocess \
		$PREFIX/src/bleualign
	make bleualign_cpp
	install bleualign_cpp $BINDIR
	popd
fi