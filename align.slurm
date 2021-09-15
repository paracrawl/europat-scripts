#!/bin/bash
#SBATCH --job-name=europat-align
#SBATCH --time=24:00:00
#SBATCH --partition=standard
#SBATCH --nodes 1
#SBATCH --cpus-per-task 4
#SBATCH --qos=standard
#SBATCH --account=ec166-guest
set -euo pipefail

./pipeline.sh $1 $SLURM_ARRAY_TASK_ID


