#! /bin/bash

# ./pdfmatch.sh NO 2018 2019 | more

export COUNT_PDFS=pdf

__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
bash "${__dir}/match.sh" "$@"
