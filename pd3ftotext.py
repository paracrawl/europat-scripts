

import json
from pd3f.export import Export
from os import listdir
from os.path import isfile, join
import os
import argparse
import flair, torch

# Set flair device to use CPU!
flair.device = torch.device("cuda:1" if torch.cuda.is_available() else "cpu")


def extractText(inputfilepath, outputfilepath):

    outputdr = outputfilepath
    isExist = os.path.isdir(outputdr)
    print("isExist: ", isExist)

    if not isExist:
        os.mkdir(outputdr)
    onlyfiles=[]
    if os.path.isfile(inputfilepath):
        f = os.path.basename(inputfilepath)
        inputfilepath = os.path.dirname(inputfilepath)
        onlyfiles.append(f)
    elif os.path.isdir(inputfilepath):
        onlyfiles = [f for f in listdir(inputfilepath) if isfile(join(inputfilepath, f))]
    else:
        raise Exception("Input file path does not exist")

#    print(onlyfiles)
    for f in onlyfiles:
        if f.endswith('.json'):
            name = os.path.splitext(f)[0]
            o = outputdr + "/"  + name + '.txt'
            if os.path.isfile(o):
                print("File Already Exist:",  o)
            else:
                try:
                    file_path = inputfilepath + "/" + f
                    print(file_path)
                    fileJson = open(file_path)
                    data = json.load(fileJson)

                    e = Export(data, seperate_header_footer=True, footnotes_last=True,
                               remove_page_number=True, lang='es', fast=False)

                    f = os.path.splitext(f)[0]
                    o = outputdr + "/"  + f + '.txt'
                    print(o)
                    with open(o, 'w', encoding='utf-8') as f:
                        f.write(e.text())

                except:
                    print("Anexception  occurred")

parser = argparse.ArgumentParser(description='Command line tool for sentence joining decisions.')
parser.add_argument('--input', help='Input file or directory of json')
parser.add_argument('--output', help='Output directory')
args = parser.parse_args()

if not args.input:
    raise Exception("ERROR: Please specify input.")

if not args.output:
    raise Exception("ERROR: Please specify output.")

# inputpath = '/home/ramoslee/work/EPOOPS/vahalla/PDFPatents/ES-1993/output-json/ES-2032299-T3-13.json'
# outputpath = '/home/ramoslee/work/EPOOPS/vahalla/PDFPatents/ES-1993/output-text-pd3f/'
extractText(inputfilepath=args.input, outputfilepath=args.output)
