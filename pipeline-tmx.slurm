#!/bin/bash
#SBATCH --job-name=europat-tmx
#SBATCH --time=24:00:00
#SBATCH --partition=standard
#SBATCH --nodes 1
#SBATCH --cpus-per-task 8
#SBATCH --qos=standard
#SBATCH --account=ec166-guest
set -euo pipefail

./pipeline-tmx.sh $*
