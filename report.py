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
COUNTED = 'counted'
MATCHED = 'matched'
WANTED = 'wanted'
DOWNLOADED = 'downloaded'
INCOMPLETE = 'incomplete'
UNAVAILABLE = 'unavailable'
FILE_TYPES = OrderedDict()
FILE_TYPES.update({TITLES : 'title'})
FILE_TYPES.update({ABSTRACTS : 'abstract'})
FILE_TYPES.update({CLAIMS : 'claim'})
FILE_TYPES.update({DESCRIPTIONS : 'desc'})

def compute_and_print_counts(args):
    # aggregate entries across all years
    totals = [Counter(), Counter(), Counter(), Counter(), Counter(), Counter(), Counter()]
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
    counted, incomplete, matched, downloaded, unavailable, pdfs, pages = result
    if year is None:
        year = 'total'
    search_incomplete = incomplete[PDFS] > 0
    pdf_counts = {PDFS : pdfs, PAGES : pages}
    text = ', {} incomplete'.format(incomplete[ENTRIES]) if incomplete[ENTRIES] else ''
    print('\n{} {}: {} entries{}'.format(args.country, year, counted[ENTRIES], text))
    if counted[ENTRIES] == 0:
        return
    limit = ' ({} page limit)'.format(args.limit) if args.limit else ''
    for t in [PDFS, PAGES]:
        counts = pdf_counts[t]
        matched_val = '?' if search_incomplete else counts[MATCHED]
        counted_val = '?' if search_incomplete else counts[COUNTED]
        print('{:>6} / {:<5} {} are wanted{}'.format(matched_val, counted_val, t, limit))
        limit = ''
    for t in FILE_TYPES:
        matched_val = '?' if search_incomplete or incomplete[t] else matched[t]
        counted_val = '?' if incomplete[t] else counted[t]
        print('{:>6} / {:<5} {} have wanted PDFs'.format(matched_val, counted_val, t))
    for t in FILE_TYPES:
        print('{:>6} {} downloaded'.format(downloaded[t], t))
    for t in FILE_TYPES:
        unavailable_val = unavailable[t]
        if unavailable_val > 0:
            print('{:>6} {} unavailable'.format(unavailable_val, t))
    if search_incomplete:
        for t in [PDFS, PAGES]:
            counts = pdf_counts[t]
            print('{:>6} {} downloaded'.format(counts[DOWNLOADED], t))
    else:
        for t in [PDFS, PAGES]:
            counts = pdf_counts[t]
            print('{:>6} / {:<5} downloaded {} are wanted'.format(counts[WANTED], counts[DOWNLOADED], t))
        for t in [PDFS, PAGES]:
            for r in [INCOMPLETE, UNAVAILABLE]:
                counts = pdf_counts[t]
                missing_val = counts[r]
                if missing_val > 0:
                    print('{:>6} / {:<5} wanted {} are {}'.format(missing_val, counts[MATCHED], t, r))
        for t in [PDFS, PAGES]:
            counts = pdf_counts[t]
            remaining_val = counts[MATCHED] - counts[WANTED] - counts[INCOMPLETE] - counts[UNAVAILABLE]
            print('{:>6} wanted {} still to download'.format(remaining_val, t))

def calculate_counts(args, year):
    session = '{}-{}'.format(args.country, year)
    yeardir = '{}/{}'.format(args.infodir, session)
    infofile = '{}/{}-info.txt'.format(yeardir, session)
    if not os.path.isdir(yeardir) or not os.path.isfile(infofile):
        return None
    # find the downloaded PDF files for each patent
    downloaded_pages = Counter()
    for filename in map(lambda x: x.name, Path(yeardir).glob('*.pdf')):
        docid, _ = filename.replace('-', '.', 2).split('-', 1)
        downloaded_pages[docid] += 1
    # find the unavailable PDF files for each patent
    unavailable_pages = find_unavailable_pages('{}/ids-{}-missing-images.txt'.format(yeardir, session))
    # count all entries and filtered entries in the main language
    counted = Counter()
    matched = Counter()
    incomplete = Counter()
    downloaded = Counter()
    unavailable = Counter()
    pdfs = Counter()
    pages = Counter()
    with open(infofile) as file:
        for line in file:
            counted[ENTRIES] += 1
            docid, _, _, t, a, c, d, p, n = line.split('\t')
            page_count = 0 if 'null' in n else int(n)
            title = 1 * args.country in t
            abstract = 1 * args.country in a
            claims = 1 * args.country in c
            description = 1 * args.country in d
            pdf = 1 * args.country in p
            counted[TITLES] += title
            counted[ABSTRACTS] += abstract
            counted[CLAIMS] += claims
            counted[DESCRIPTIONS] += description
            pdfs[COUNTED] += pdf
            pages[COUNTED] += page_count
            incomplete[TITLES] += 1 * 'null' in t
            incomplete[ABSTRACTS] += 1 * 'null' in a
            incomplete[CLAIMS] += 1 * 'null' in c
            incomplete[DESCRIPTIONS] += 1 * 'null' in d
            incomplete[PDFS] += 1 * 'null' in n
            if 'null' in ' '.join([t, a, c, d, n]):
                incomplete[ENTRIES] += 1
            elif len(t) * len(a) * len(c) * len(d) == 0 and page_count > 0:
                patent_page_count = downloaded_pages[docid]
                if patent_page_count == page_count:
                    pdfs[DOWNLOADED] += 1
                pages[DOWNLOADED] += patent_page_count
                if args.limit is None or page_count <= args.limit:
                    matched[TITLES] += title
                    matched[ABSTRACTS] += abstract
                    matched[CLAIMS] += claims
                    matched[DESCRIPTIONS] += description
                    pdfs[MATCHED] += 1
                    pages[MATCHED] += page_count
                    if patent_page_count == page_count:
                        pdfs[WANTED] += 1
                    elif args.verbose:
                        missing = page_count - patent_page_count
                        if missing > 0:
                            print('{} missing for {}'.format(missing, docid))
                    pages[WANTED] += patent_page_count
                    unavailable_page_count = unavailable_pages[docid]
                    if unavailable_page_count > 0:
                        pdfs[INCOMPLETE] += 1
                    pages[UNAVAILABLE] += unavailable_page_count
    # count downloaded text entries in the main language
    for t, f in FILE_TYPES.items():
        downloaded[t] = count_lines('{}/{}-{}-{}.tab'.format(yeardir, args.country, session, f))
    # count unavailable text entries
    for t, f in FILE_TYPES.items():
        unavailable[t] = count_lines('{}/ids-{}-missing-{}.txt'.format(yeardir, session, t))
    # return the results
    return counted, incomplete, matched, downloaded, unavailable, pdfs, pages

def count_lines(filename):
    count = 0
    if os.path.isfile(filename):
        with open(filename) as file:
            for line in file:
                count += 1
    return count

def find_unavailable_pages(filename):
    result = Counter()
    if os.path.isfile(filename):
        with open(filename) as file:
            for line in file:
                docid = line.strip().split()[0]
                result[docid] += 1
    return result

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
    parser.add_argument('-v', '--verbose', help="Verbose output", action="store_true")
    args = parser.parse_args()

    # dynamic defaults for missing positional args
    args.end = args.end or args.start or endyear
    args.start = args.start or startyear
    compute_and_print_counts(args)


if __name__ == "__main__":
    # execute only if run as a script
    main()
