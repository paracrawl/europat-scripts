#!/bin/bash

if [[ "$(hostname)" =~ "cirrus" ]]; then
	module load \
		cmake \
		nvidia/cuda-10.2 \
		nvidia/mathlibs-10.2 \
		java/jdk-14.0.1
fi

export PREFIX=$(dirname $(realpath $0))
BINDIR=$PREFIX/bin
export PATH=$BINDIR:$PATH

