#!/usr/bin/python3

import argparse
import datetime
import os

from collections import Counter, OrderedDict
from pathlib import Path

ENTRIES = 'entries'
TITLES = 'titles'
ABSTRACTS = 'abstracts'
CLAIMS = 'claims'
DESCRIPTIONS = 'descriptions'
PDFS = 'PDFs'
PAGES = 'pages'
PDFS_WANTED = 'wanted PDFs'
PAGES_WANTED = 'wanted pages'
FILE_TYPES = OrderedDict()
FILE_TYPES.update({TITLES : 'title'})
FILE_TYPES.update({ABSTRACTS : 'abstract'})
FILE_TYPES.update({CLAIMS : 'claim'})
FILE_TYPES.update({DESCRIPTIONS : 'desc'})

def compute_and_print_counts(args):
    # aggregate entries across all years
    totals = [Counter(), Counter(), Counter(), Counter()]
    for year in range(args.start, args.end+1):
        result = calculate_counts(args, year)
        if result is None:
            print('{}: no results'.format(year))
            continue
        print_counts(args, result, year)
        totals = [x+y for (x, y) in zip(totals, result)]
    if args.start != args.end:
        print_counts(args, totals)

def print_counts(args, result, year=None):
    counted, incomplete, matched, downloaded = result
    if year is None:
        year = 'total'
    text = ', {} incomplete'.format(incomplete[ENTRIES]) if incomplete[ENTRIES] else ''
    print('{} {}: {} entries{}'.format(args.country, year, counted[ENTRIES], text))
    limit = ' ({} page limit)'.format(args.limit) if args.limit else ''
    for t in [PDFS, PAGES]:
        matched_val = '?' if incomplete[PDFS] else matched[t]
        counted_val = '?' if incomplete[PDFS] else counted[t]
        print('{:>6} / {:<5} {} are wanted{}'.format(matched_val, counted_val, t, limit))
        limit = ''
    for t in FILE_TYPES:
        matched_val = '?' if incomplete[t] or incomplete[PDFS] else matched[t]
        counted_val = '?' if incomplete[t] else counted[t]
        print('{:>6} / {:<5} {} have wanted PDFs'.format(matched_val, counted_val, t))
    for t in FILE_TYPES:
        print('{:>6} {} downloaded'.format(downloaded[t], t))
    if incomplete[PDFS]:
        for t in [PDFS, PAGES]:
            print('{:>6} {} downloaded'.format(downloaded[t], t))
    else:
        for (w, t) in [(PDFS_WANTED, PDFS), (PAGES_WANTED, PAGES)]:
            print('{:>6} / {:<5} downloaded {} are wanted'.format(downloaded[w], downloaded[t], t))
        for t in [PDFS_WANTED, PAGES_WANTED]:
            print('{:>6} {} still to download'.format(matched[t] - downloaded[t], t))

def calculate_counts(args, year):
    session = '{}-{}'.format(args.country, year)
    yeardir = '{}/{}'.format(args.infodir, session)
    infofile = '{}/{}-info.txt'.format(yeardir, session)
    if not os.path.isdir(yeardir) or not os.path.isfile(infofile):
        return None
    # find the downloaded PDF files for each patent
    pdfs = Counter()
    for filename in map(lambda x: x.name, Path(yeardir).glob('*.pdf')):
        docid, _ = filename.replace('-', '.', 2).split('-', 1)
        pdfs[docid] += 1
    # count all entries and filtered entries in the main language
    counted = Counter()
    matched = Counter()
    incomplete = Counter()
    downloaded = Counter()
    with open(infofile) as file:
        for line in file:
            counted[ENTRIES] += 1
            docid, _, _, t, a, c, d, p, n = line.split('\t')
            pages = 0 if 'null' in n else int(n)
            title = 1 * args.country in t
            abstract = 1 * args.country in a
            claims = 1 * args.country in c
            description = 1 * args.country in d
            pdf = 1 * args.country in p
            counted[TITLES] += title
            counted[ABSTRACTS] += abstract
            counted[CLAIMS] += claims
            counted[DESCRIPTIONS] += description
            counted[PDFS] += pdf
            counted[PAGES] += pages
            incomplete[TITLES] += 1 * 'null' in t
            incomplete[ABSTRACTS] += 1 * 'null' in a
            incomplete[CLAIMS] += 1 * 'null' in c
            incomplete[DESCRIPTIONS] += 1 * 'null' in d
            incomplete[PDFS] += 1 * 'null' in n
            if 'null' in ' '.join([t, a, c, d, n]):
                incomplete[ENTRIES] += 1
            elif len(t) * len(a) * len(c) * len(d) == 0 and pages > 0:
                downloaded_pages = pdfs[docid]
                if downloaded_pages == pages:
                    downloaded[PDFS] += 1
                downloaded[PAGES] += downloaded_pages
                if args.limit is None or pages <= args.limit:
                    matched[TITLES] += title
                    matched[ABSTRACTS] += abstract
                    matched[CLAIMS] += claims
                    matched[DESCRIPTIONS] += description
                    matched[PDFS] += 1
                    matched[PAGES] += pages
                    if downloaded_pages == pages:
                        downloaded[PDFS_WANTED] += 1
                    downloaded[PAGES_WANTED] += downloaded_pages
    matched[PDFS_WANTED] = matched[PDFS]
    matched[PAGES_WANTED] = matched[PAGES]
    # count downloaded text entries in the main language
    for t, f in FILE_TYPES.items():
        downloaded[t] = count_lines('{}/{}-{}-{}.tab'.format(yeardir, args.country, session, f))
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
    compute_and_print_counts(args)


if __name__ == "__main__":
    # execute only if run as a script
    main()
