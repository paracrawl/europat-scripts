#!/usr/bin/python3

import argparse
import datetime
import os

from collections import defaultdict, Counter, OrderedDict
from pathlib import Path

EARLIEST_YEAR = 1800
DEFAULT_START_YEAR = 1980

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
ANY_TEXT = 'any text'
ALL_TEXT = 'all text'
DOWNLOADED = 'downloaded'
INCOMPLETE = 'incomplete'
UNAVAILABLE = 'unavailable'
SAMPLE_PDFS = 'sample PDFs'
SAMPLE_PAGES = 'sample pages'

FILE_TYPES = OrderedDict()
FILE_TYPES.update({TITLES : 'title'})
FILE_TYPES.update({ABSTRACTS : 'abstract'})
FILE_TYPES.update({CLAIMS : 'claim'})
FILE_TYPES.update({DESCRIPTIONS : 'desc'})

def compute_and_print_counts(args):
    # aggregate entries across all years
    totals = [Counter(), Counter(), Counter(), Counter(), Counter(), Counter(), Counter(), Counter()]
    text_totals = defaultdict(Counter)
    for year in range(args.start, args.end+1):
        label = '{}'.format(year)
        result, text_result = calculate_counts(args, year)
        if result is None:
            if not args.summary:
                print('{}: no results'.format(label))
            continue
        if not args.summary:
            print_counts(args, result, text_result, label)
        totals = [x+y for (x, y) in zip(totals, result)]
        for lang in text_result:
            text_totals[lang] += text_result[lang]
    if args.start != args.end:
        label = '{}-{} total'.format(args.start, args.end)
        print_counts(args, totals, text_totals, label)

def print_counts(args, result, text_results, label):
    counted, incomplete, matched, unavailable, pdfs, pages, sample_pdfs, sample_pages = result
    search_incomplete = incomplete[PDFS] > 0
    pdf_counts = {PDFS : pdfs, PAGES : pages}
    text = ', {} incomplete'.format(incomplete[ENTRIES]) if incomplete[ENTRIES] else ''
    print('\n{} {}: {} entries{}'.format(args.country, label, counted[ENTRIES], text))
    if counted[ENTRIES] == 0:
        return
    limit = ' ({} page limit)'.format(args.limit) if args.limit else ''
    for t in pdf_counts:
        counts = pdf_counts[t]
        matched_val = '?' if search_incomplete else counts[MATCHED]
        counted_val = '?' if search_incomplete else counts[COUNTED]
        print('{:>7} / {:<6} {} are wanted{}'.format(matched_val, counted_val, t, limit))
        limit = ''
    for t in FILE_TYPES:
        matched_val = '?' if search_incomplete or incomplete[t] else matched[t]
        counted_val = '?' if incomplete[t] else counted[t]
        print('{:>7} / {:<6} {} have wanted PDFs'.format(matched_val, counted_val, t))
    print('{:>7} / {:<6} patents have any text in {}'.format(counted[ANY_TEXT], counted[ENTRIES], args.country))
    print('{:>7} / {:<6} patents have all text in {}'.format(counted[ALL_TEXT], counted[ENTRIES], args.country))
    num_langs = len(text_results)
    for lang in sorted(text_results):
        downloaded = text_results[lang]
        if num_langs > 1:
            print('  -- lang={} --'.format(lang))
        for t in FILE_TYPES:
            print('{:>7} {} downloaded'.format(downloaded[t], t))
    if num_langs > 1:
        print('  -- ------- --')
    for t in FILE_TYPES:
        unavailable_val = unavailable[t]
        if unavailable_val > 0:
            print('{:>7} {} unavailable'.format(unavailable_val, t))
    if search_incomplete:
        for t in pdf_counts:
            counts = pdf_counts[t]
            print('{:>7} {} downloaded'.format(counts[DOWNLOADED], t))
    else:
        print_pdf_counts(pdf_counts)
        if pages[MATCHED] and sample_pages[MATCHED]:
            print('  -- ------- --')
        print_pdf_counts({SAMPLE_PDFS : sample_pdfs, SAMPLE_PAGES : sample_pages})

def print_pdf_counts(pdf_counts):
    for t in pdf_counts:
        counts = pdf_counts[t]
        downloaded = counts[DOWNLOADED]
        if downloaded > 0:
            print('{:>7} / {:<6} downloaded {} are wanted'.format(counts[WANTED], downloaded, t))
    for t in pdf_counts:
        for r in [INCOMPLETE, UNAVAILABLE]:
            counts = pdf_counts[t]
            missing = counts[r]
            if missing > 0:
                print('{:>7} / {:<6} wanted {} are {}'.format(missing, counts[MATCHED], t, r))
    for t in pdf_counts:
        counts = pdf_counts[t]
        remaining = counts[MATCHED] - counts[WANTED] - counts[INCOMPLETE] - counts[UNAVAILABLE]
        if counts[MATCHED] > 0:
            print('{:>7} / {:<6} wanted {} still to download'.format(remaining, counts[MATCHED], t))

def calculate_counts(args, year):
    session = '{}-{}'.format(args.country, year)
    yeardir = '{}/{}'.format(args.infodir, session)
    sampledir = '{}/sample'.format(yeardir)
    infofile = '{}/{}-info.txt'.format(yeardir, session)
    if not os.path.isdir(yeardir) or not os.path.isfile(infofile):
        return None, None
    text = defaultdict(set) if args.verbose else None
    # find the downloaded text entries for each language
    text_results = defaultdict(Counter)
    for t, f in FILE_TYPES.items():
        for filename in map(lambda x: x.name, Path(yeardir).glob('{}-*-{}-{}.tab'.format(args.country, year, f))):
            lang = filename.split('-')[1]
            downloaded = text_results[lang]
            docids = text[t] if text is not None and lang == args.country else None
            downloaded[t] = count_lines('{}/{}'.format(yeardir, filename), docids)
    # count unavailable text entries
    unavailable = Counter()
    for t in FILE_TYPES:
        docids = text[t] if text is not None else None
        unavailable[t] = count_lines('{}/ids-{}-missing-{}.txt'.format(yeardir, session, t), docids)
    # find the downloaded PDF files for each patent
    downloaded_pages = find_downloaded_pages(yeardir)
    downloaded_sample_pages = find_downloaded_pages(sampledir)
    # find the unavailable PDF files for each patent
    unavailable_pages = find_unavailable_pages('{}/ids-{}-missing-images.txt'.format(yeardir, session))
    unavailable_sample_pages = find_unavailable_pages('{}/ids-{}-missing-images.txt'.format(sampledir, session))
    # count all entries and filtered entries in the main language
    counted = Counter()
    matched = Counter()
    incomplete = Counter()
    pdfs = Counter()
    pages = Counter()
    sample_pdfs = Counter()
    sample_pages = Counter()
    with open(infofile) as file:
        for line in file:
            counted[ENTRIES] += 1
            parts = line.split('\t')
            if len(parts) == 9:
                docid, _, _, t, a, c, d, p, n = parts
            else:
                docid, _, _, t, a, c, d, p, n, _, _ = parts
            page_count = 0 if 'null' in n else int(n)
            found = {}
            found[TITLES] = 1 * args.country in t
            found[ABSTRACTS] = 1 * args.country in a
            found[CLAIMS] = 1 * args.country in c
            found[DESCRIPTIONS] = 1 * args.country in d
            any_text_main_lang = 1 in found.values()
            all_text_main_lang = not 0 in found.values()
            for f in FILE_TYPES:
                counted[f] += found[f]
                if text is not None:
                    if found[f] and docid not in text[f]:
                        text[docid].add(f)
            if any_text_main_lang:
                counted[ANY_TEXT] += 1
            if all_text_main_lang:
                counted[ALL_TEXT] += 1
            pdf = 1 * args.country in p
            pdfs[COUNTED] += pdf
            pages[COUNTED] += page_count
            incomplete[TITLES] += 1 * 'null' in t
            incomplete[ABSTRACTS] += 1 * 'null' in a
            incomplete[CLAIMS] += 1 * 'null' in c
            incomplete[DESCRIPTIONS] += 1 * 'null' in d
            incomplete[PDFS] += 1 * 'null' in n
            if 'null' in ' '.join([t, a, c, d, n]):
                incomplete[ENTRIES] += 1
            elif page_count > 0:
                if len(t) * len(a) * len(c) * len(d) == 0:
                    counts = (page_count, downloaded_pages[docid], unavailable_pages[docid])
                    within_limit = process_pdfs(args, counts, pdfs, pages, docid)
                    if within_limit:
                        for f in FILE_TYPES:
                            matched[f] += found[f]
                elif all_text_main_lang:
                    counts = (page_count, downloaded_sample_pages[docid], unavailable_sample_pages[docid])
                    process_pdfs(args, counts, sample_pdfs, sample_pages, docid)
    if text is not None:
        for f in FILE_TYPES:
            del text[f]
        for docid in sorted(text):
            missing = text[docid]
            print('{}: missing {}'.format(docid, ' '.join(missing)))
    # return the results
    return (counted, incomplete, matched, unavailable, pdfs, pages, sample_pdfs, sample_pages), text_results

def process_pdfs(args, counts, pdfs, pages, docid):
    pages_total, pages_downloaded, pages_unavailable = counts
    pages[DOWNLOADED] += pages_downloaded
    if pages_downloaded == pages_total:
        pdfs[DOWNLOADED] += 1
    within_limit = args.limit is None or pages_total <= args.limit
    if within_limit:
        pages[MATCHED] += pages_total
        pdfs[MATCHED] += 1
        pages[WANTED] += pages_downloaded
        if pages_downloaded == pages_total:
            pdfs[WANTED] += 1
        elif args.verbose:
            missing = pages_total - pages_downloaded
            if missing > 0:
                print('{} missing for {}'.format(missing, docid))
        pages[UNAVAILABLE] += pages_unavailable
        if pages_unavailable > 0:
            pdfs[INCOMPLETE] += 1
    return within_limit

def count_lines(filename, docids=None):
    count = 0
    if os.path.isfile(filename):
        with open(filename) as file:
            for line in file:
                count += 1
                if docids is not None:
                    docid = line.strip().split()[0]
                    docids.add(docid.replace('-', '.'))
    return count

def find_downloaded_pages(dirname):
    result = Counter()
    if os.path.isdir(dirname):
        for filename in map(lambda x: x.name, Path(dirname).glob('*.pdf')):
            docid, _ = filename.replace('-', '.', 2).split('-', 1)
            result[docid] += 1
    return result

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
    raise argparse.ArgumentTypeError('must be a 2-letter country code')

def check_year(value):
    ivalue = int(value)
    if ivalue < EARLIEST_YEAR or ivalue > datetime.datetime.now().year:
        raise argparse.ArgumentTypeError('%s is out of range' % value)
    return ivalue

def check_limit(value):
    message = 'must be a positive integer, or zero for no limit'
    try:
        ivalue = int(value)
        if ivalue < 0:
            raise argparse.ArgumentTypeError(message)
        return None if ivalue == 0 else ivalue
    except ValueError:
        raise argparse.ArgumentTypeError(message)

def main():
    startyear = DEFAULT_START_YEAR
    endyear = datetime.datetime.now().year
    limit = os.environ.get('PDF_PAGE_LIMIT', 25)
    infodir = os.environ.get('INFODIR', '/fs/loki0/data/pdfpatents')
    message = 'Print patent counts for the given country code. A single year, or both a start and end year (inclusive) can be given. If no years are specified, the range {}-{} will be used. Patents with too many PDF pages will be ignored. The page limit is configurable (default {}).'.format(startyear, endyear, limit)

    parser = argparse.ArgumentParser(description=message)
    parser.add_argument('country', metavar='country-code', help='Country code', type=check_country)
    parser.add_argument('start', metavar='start-year', nargs='?', help='First year of patents to process', type=check_year)
    parser.add_argument('end', metavar='end-year', nargs='?', help='Last year of patents to process', type=check_year)
    parser.add_argument('--limit', help='Maximum number of PDF pages (default {})'.format(limit), default=limit, type=check_limit)
    parser.add_argument('--infodir', help='Directory with info files', default=infodir)
    parser.add_argument('-s', '--summary', help='Only print total counts', action='store_true')
    parser.add_argument('-v', '--verbose', help='Verbose output', action='store_true')
    args = parser.parse_args()

    # dynamic defaults for missing positional args
    args.end = args.end or args.start or endyear
    args.start = args.start or startyear
    # if there is only a single year, don't repeat it as a summary
    args.summary = args.summary and args.end != args.start
    compute_and_print_counts(args)


if __name__ == '__main__':
    # execute only if run as a script
    main()
