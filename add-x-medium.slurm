#!/bin/bash
#SBATCH --job-name=europat-hashes
#SBATCH --time=12:00:00
#SBATCH --partition=standard
#SBATCH --nodes 1
#SBATCH --cpus-per-task 4
#SBATCH --qos=standard
#SBATCH --account=ec166-guest
#SBATCH --array 0-5
set -euo pipefail

LANGUAGES=(DE FR ES HR PL NO)
L=${LANGUAGES[$SLURM_ARRAY_TASK_ID]}

echo "lanugage: $L" >&2

pigz -cd /beegfs/europat-release-3-threshold-0.5/${L,,}-en.tmx.gz \
| bin/pypy ./ocr-info.py <(pigz -cd /beegfs/europat-release-3-no-ocr/${L,,}-en.hashes.gz) \
| pigz -9c > /beegfs/europat-release-3-threshold-0.5/${L,,}-en.tmx-with-medium.gz.$$ \
&& mv /beegfs/europat-release-3-threshold-0.5/${L,,}-en.tmx-with-medium.gz{.$$,}
