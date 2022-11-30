#!/bin/bash

if [[ "$(hostname -A)" =~ "epcc.ed.ac.uk" ]]; then
	module load \
		cmake \
		intel-tools-19 \
		nvidia/cuda-11.2 \
		nvidia/mathlibs-11.2 \
		nvidia/cudnn/8.2.1-cuda-11.2 \
		java/jdk-14.0.1
fi

# Note: This doesn't actually run on compute nodes on CSD3 so its a bi
# useless. But only a bit, it still runs properly when you're compiling
# stuff on the login node and that's helpful. So as a work-around we'll
# just symlink parallel into our environment.
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

export PREFIX=${PREFIX:-$(dirname $(realpath $0))}
export PATH=$PREFIX/bin:$PATH
PS1="(europat) [\u@\h \W]\$ "
