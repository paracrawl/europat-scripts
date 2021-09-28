#!/bin/bash
#SBATCH --job-name=europat-hashes
#SBATCH --time=12:00:00
#SBATCH --partition=standard
#SBATCH --nodes 1
#SBATCH --cpus-per-task 4
#SBATCH --qos=standard
#SBATCH --account=ec166-guest
#SBATCH --array 1-5
set -euo pipefail

LANGUAGES=(DE FR ES HR PL NO)
L=${LANGUAGES[$SLURM_ARRAY_TASK_ID]}

src/tmxutil/tmxutil.py \
	--output-format tab \
	--output-columns bifixer-hash \
	-- /beegfs/europat-release-3-no-ocr/${L^^}.tmx.gz \
| pigz -c > /beegfs/europat-release-3-no-ocr/${L,,}-en.hashes.gz.$$ \
&& mv /beegfs/europat-release-3-no-ocr/${L,,}-en.hashes.gz{.$$,}
