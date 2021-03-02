#!/bin/bash
export PREFIX=$(dirname ${BASH_SOURCE[0]})
bash --init-file $PREFIX/init.sh "$@"
