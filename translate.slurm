#!/bin/bash
#SBATCH --job-name=europat-translate
#SBATCH --time=24:00:00
#SBATCH --partition=gpu-cascade
#SBATCH --qos=gpu
#SBATCH --gres=gpu:1
#SBATCH --account=ec166-guest
set -euo pipefail

PREFIX=$HOME/src/europat-scripts
ENTRY=$(awk "NR == $SLURM_ARRAY_TASK_ID" < $1)
BASENAME=${ENTRY##*/}
L1=${BASENAME%%-*}
shift

$PREFIX/pipeline-monolingual.sh $PREFIX/translate.sh ${L1,,} -- $ENTRY
