#!/usr/bin/python3

import argparse
import datetime
import glob
import os

from collections import Counter, OrderedDict

ENTRIES = 'entries'
TITLES = 'titles'
ABSTRACTS = 'abstracts'
CLAIMS = 'claims'
DESCRIPTIONS = 'descriptions'
PDF = 'PDFs'
FILE_TYPES = OrderedDict()
FILE_TYPES.update({TITLES : 'title'})
FILE_TYPES.update({ABSTRACTS : 'abstract'})
FILE_TYPES.update({CLAIMS : 'claim'})
FILE_TYPES.update({DESCRIPTIONS : 'desc'})

def print_counts(args):
    for year in range(args.start, args.end+1):
        result = calculate_counts(args, year)
        if result is None:
            print('{}: no results'.format(year))
            continue
        counted, incomplete, matched, downloaded = result
        text = ', {} incomplete'.format(incomplete[ENTRIES]) if incomplete[ENTRIES] else ''
        print('{} {}: {} entries{}'.format(args.country, year, counted[ENTRIES], text))
        matched_val = '?' if incomplete[PDF] else matched[PDF]
        counted_val = '?' if incomplete[PDF] else counted[PDF]
        print('{:>6} / {:<5} {} are wanted'.format(matched_val, counted_val, PDF))
        for t in FILE_TYPES:
            matched_val = '?' if incomplete[t] or incomplete[PDF] else matched[t]
            counted_val = '?' if incomplete[t] else counted[t]
            print('{:>6} / {:<5} {} have wanted PDFs'.format(matched_val, counted_val, t))
        for t in list(FILE_TYPES) + [PDF]:
            print('{:>6} {} downloaded'.format(downloaded[t], t))

def calculate_counts(args, year):
    session = '{}-{}'.format(args.country, year)
    yeardir = '{}/{}'.format(args.infodir, session)
    infofile = '{}/{}-info.txt'.format(yeardir, session)
    if not os.path.isdir(yeardir) or not os.path.isfile(infofile):
        return None
    # count all entries and filtered entries in the main language
    counted = Counter()
    matched = Counter()
    incomplete = Counter()
    with open(infofile) as file:
        for line in file:
            counted[ENTRIES] += 1
            _, _, _, t, a, c, d, p, n = line.split('\t')
            title = 1 * args.country in t
            abstract = 1 * args.country in a
            claims = 1 * args.country in c
            description = 1 * args.country in d
            pdf = 1 * args.country in p
            counted[TITLES] += title
            counted[ABSTRACTS] += abstract
            counted[CLAIMS] += claims
            counted[DESCRIPTIONS] += description
            counted[PDF] += pdf
            incomplete[TITLES] += 1 * 'null' in t
            incomplete[ABSTRACTS] += 1 * 'null' in a
            incomplete[CLAIMS] += 1 * 'null' in c
            incomplete[DESCRIPTIONS] += 1 * 'null' in d
            incomplete[PDF] += 1 * 'null' in n
            pages = 0 if 'null' in n else int(n)
            if 'null' in ' '.join([t, a, c, d, n]):
                incomplete[ENTRIES] += 1
            elif len(t) * len(a) * len(c) * len(d) == 0 and pages > 0:
                if args.limit is None or pages <= args.limit:
                    matched[PDF] += 1
                    matched[TITLES] += title
                    matched[ABSTRACTS] += abstract
                    matched[CLAIMS] += claims
                    matched[DESCRIPTIONS] += description
    # count downloaded entries in the main language
    downloaded = {l : count_lines('{}/{}-{}-{}.tab'.format(yeardir, args.country, session, k))
                     for l, k in FILE_TYPES.items()}
    downloaded[PDF] = len(glob.glob('{}/*-1.pdf'.format(yeardir)))
    # return the results
    return counted, incomplete, matched, downloaded

def count_lines(filename):
    count = 0
    if os.path.isfile(filename):
        with open(filename) as file:
            for line in file:
                count += 1
    return count

def check_country(value):
    if len(value) == 2 and value.isalpha():
        return value.upper()
    raise argparse.ArgumentTypeError("must be a 2-letter country code")

def check_year(value):
    ivalue = int(value)
    if ivalue < 1900 or ivalue >= datetime.datetime.now().year:
        raise argparse.ArgumentTypeError("%s is out of range" % value)
    return ivalue

def check_limit(value):
    message = "must be a positive integer, or zero for no limit"
    try:
        ivalue = int(value)
        if ivalue < 0:
            raise argparse.ArgumentTypeError(message)
        return None if ivalue == 0 else ivalue
    except ValueError:
        raise argparse.ArgumentTypeError(message)

def main():
    startyear = 1994
    endyear = datetime.datetime.now().year-1
    limit = os.environ.get('PDF_PAGE_LIMIT', 25)
    infodir = os.environ.get('INFODIR', '/fs/loki0/data/pdfpatents')
    message = 'Print patent counts for the given country code. A single year, or both a start and end year (inclusive) can be given. If no years are specified, the range {}-{} will be used. Patents with too many PDF pages will be ignored. The page limit is configurable (default {}).'.format(startyear, endyear, limit)

    parser = argparse.ArgumentParser(description=message)
    parser.add_argument('country', metavar='country-code', help='Country code', type=check_country)
    parser.add_argument('start', metavar='start-year', nargs='?', help='First year of patents to process', type=check_year)
    parser.add_argument('end', metavar='end-year', nargs='?', help='Last year of patents to process', type=check_year)
    parser.add_argument('--limit', help='Maximum number of PDF pages (default {})'.format(limit), default=limit, type=check_limit)
    parser.add_argument('--infodir', help='Directory with info files', default=infodir)
    args = parser.parse_args()

    # dynamic defaults for missing positional args
    args.end = args.end or args.start or endyear
    args.start = args.start or startyear
    print_counts(args)


if __name__ == "__main__":
    # execute only if run as a script
    main()
