#!/usr/bin/python3

import argparse
import datetime
import os

from pathlib import Path

EARLIEST_YEAR = 1800
DEFAULT_START_YEAR = 1980

def normalize_ocr(args):
    for year in range(args.start, args.end+1):
        if args.verbose:
            print('Processing {}'.format(year))
        normalize_ocr_by_year(args, year)
        if args.verbose:
            print('{} done'.format(year))

def normalize_ocr_by_year(args, year):
    # combine the OCR'd text files
    output_dir = get_output_dir(args, year)
    output_filename = get_output_filename(args, year)
    output_filename = '{}/{}'.format(output_dir, output_filename)
    input_filenames = list(get_files_by_year(args, year))
    if input_filenames:
        with open(output_filename, 'w') as f:
            for docid, filename in input_filenames:
                content = read_file(filename)
                # we don't know the date
                f.write('{}\t\t{}\n'.format(docid, content))
                if args.verbose:
                    print('  Processed {}'.format(filename))
        if args.verbose:
            print('  Saved {}'.format(output_filename))

def read_file(filename):
    if os.path.isfile(filename):
        with open(filename) as f:
            return '<br>'.join(process_file(f))

def process_file(f):
    for line in f:
        line = line.strip()
        if line:
            yield line.replace('\t', '@TAB@')

def get_files_by_year(args, year):
    input_dir = get_input_dir(args, year)
    if os.path.isdir(input_dir):
        file_pattern = get_file_pattern(args, year)
        for p in sorted(Path(input_dir).glob(file_pattern)):
            docid, _ = p.stem.rsplit('-', 1)
            yield docid, str(p)

def get_input_dir(args, year):
    input_dir = '{}-{}'.format(args.country, year)
    return '{}/{}'.format(args.datadir, input_dir)

def get_file_pattern(args, year):
    return '{}-*.txt'.format(args.country)

def get_output_dir(args, year):
    return args.outdir or get_input_dir(args, year)

def get_output_filename(args, year):
    return '{}-{}-ocr.tab'.format(args.country, year)

def check_country(value):
    if len(value) == 2 and value.isalpha():
        return value.upper()
    raise argparse.ArgumentTypeError('must be a 2-letter country code')

def check_year(value):
    ivalue = int(value)
    if ivalue < EARLIEST_YEAR or ivalue > datetime.datetime.now().year:
        raise argparse.ArgumentTypeError('%s is out of range' % value)
    return ivalue

def check_dir_path(value):
    message = 'not a directory'
    if os.path.isdir(value):
        return value
    else:
        raise argparse.ArgumentTypeError(message)

def main():
    startyear = DEFAULT_START_YEAR
    endyear = datetime.datetime.now().year
    datadir = os.environ.get('PATENT_DATA_DIR', '/data/patents/pdfpatents/')
    message = 'Create a file in normalized form from separate OCR output files. Combine files for all patents with the given country code and year. A single year, or both a start and end year (inclusive) can be given. If no years are specified, the range {}-{} will be used.'.format(startyear, endyear)

    parser = argparse.ArgumentParser(description=message)
    parser.add_argument('country', metavar='country-code', help='Country code', type=check_country)
    parser.add_argument('start', metavar='start-year', nargs='?', help='First year of patents to process', type=check_year)
    parser.add_argument('end', metavar='end-year', nargs='?', help='Last year of patents to process', type=check_year)
    parser.add_argument('--outdir', metavar='output-dir', help='Output directory', type=check_dir_path)
    parser.add_argument('--datadir', help='Directory with data files to process', default=datadir)
    parser.add_argument('-v', '--verbose', help='Verbose output', action='store_true')
    args = parser.parse_args()

    # dynamic defaults for missing positional args
    args.end = args.end or args.start or endyear
    args.start = args.start or startyear
    normalize_ocr(args)


if __name__ == '__main__':
    # execute only if run as a script
    main()
