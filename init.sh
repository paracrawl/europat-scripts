#!/bin/bash

if [[ "$(hostname)" =~ "cirrus" ]]; then
	module load \
		cmake \
		nvidia/cuda-10.2 \
		nvidia/mathlibs-10.2 \
		java/jdk-14.0.1
fi

if [[ "$(hostname -A)" =~ "hpc.cam.ac.uk" ]]; then
	module purge && module load \
		rhel7/global \
		slurm \
		parallel \
		cmake \
		gcc/8 `# CUDA10.2 can't deal with gcc > 8` \
		binutils-2.31.1-gcc-5.4.0-uyyspmn `# Nick: newer binutils is better` \
		intel/mkl/2020.2 \
		cuda/10.2 `# Marian doesn't support 11 properly yet`
fi

export PREFIX=$(dirname $(realpath $0))
BINDIR=$PREFIX/bin
export PATH=$BINDIR:$PATH

