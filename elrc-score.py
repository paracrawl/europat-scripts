#!/usr/bin/env python3
import sys
import re

TOKEN_PATTERN = re.compile(r'(([\{-\~\[-\` -\&\(-\+\:-\@\/])|(?:(?<![0-9])([\.,]))|(?:([\.,])(?![0-9]))|(?:(?<=[0-9])(-)))')


def tokenize(sentence):
	pos = 0
	for match in re.finditer(TOKEN_PATTERN, sentence):
		if match.start() > pos:
			yield sentence[pos:match.start()]

		if match.group() != ' ':
			yield match.group()

		pos = match.end()

	if pos < len(sentence):
		yield sentence[pos:]


def token_count(sentence):
	return sum(1 for _ in tokenize(sentence))


def metric_elrc(sent_sl, sent_tl):
	length_ratio = len(sent_sl) * 1.0 / len(sent_tl) if len(sent_tl) > 0 else 0.0
	tokens_sl = token_count(sent_sl)
	tokens_tl = token_count(sent_tl)
	print(f"{length_ratio:.3f}\t{tokens_sl:d}\t{tokens_tl:d}")


if len(sys.argv) == 1:
	cols = (0, 1)	
elif len(sys.argv) == 3:
	cols = (int(sys.argv[1]) - 1, int(sys.argv[2]) - 1)
else:
	print(f"usage: {sys.argv[0]} [ COL1 COL2 ]", file=sys.stderr)
	sys.exit(1)

for line in sys.stdin:
	fields = line.rstrip("\n").split("\t")
	metric_elrc(fields[cols[0]], fields[cols[1]])
