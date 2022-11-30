#!/bin/bash
#SBATCH --job-name=europat-bicleaner-ai
#SBATCH --time=24:00:00
#SBATCH --partition=gpu-cascade
#SBATCH --qos=gpu
#SBATCH --gres=gpu:1
#SBATCH --account=ec166-guest
set -euo pipefail

export PREFIX=$HOME/src/europat-scripts
source $PREFIX/init.sh

export TF_CPP_MIN_LOG_LEVEL=1

export THREADS=${SLURM_CPUS_ON_NODE:-4}
export TMPSUF=${SLURM_JOB_ID:-$$}

NUM=$(printf '%03d' $SLURM_ARRAY_TASK_ID)

export lang=$1
shift

bicleaner-ai-scores() {
	bicleaner-ai-classify \
		--score_only \
		--scol 4 --tcol 3 \
		--processes $THREADS \
		--disable_minimal_length \
		--disable_porn_removal \
		- - $PREFIX/bicleaner-ai-models/en-${lang,,}/metadata.yaml
}

export -f bicleaner-ai-scores

classified=${lang^^}.classified.$NUM
fixed=${lang^^}.fixed.$NUM

echo "creating $classified" >&2

{
	pigz -cd $fixed \
	| $PREFIX/paste-pipe.py bash -c bicleaner-ai-scores \
	| pigz -c > $classified.$TMPSUF
} && mv $classified{.$TMPSUF,}
