#!/bin/bash


# required arguments
if [ "$1" = "--help" ]; then
    echo Calculate the total size of unprocessed files for a language
    echo Required arguments:
    echo partially-processed-file-list \(full path to files inside\)
    echo list of \*aligment-errors-xxparts.errors \(full path to files inside\)
    echo number of parts the files have been split to \(the xx part of the error files above\)
    echo language pair \(in CAPITALS\)
    echo Usage:
    echo ./get_files_size.sh rawFilesList errorFilesList 10 FR-EN
    echo -e "N.B.: Should preferrable be run in subshell"$'\n'
    echo You can get the errroFileList with: 
    echo "ls *10parts.txt | sed -r \"s/[^ ]* */\/rds\/project\/rds\-48gU72OtDNY\/europat\/fr\-en\/alignment\-error\-logs\/&/g\" > error.files"
    echo And a list of raw paired files:
    echo -e "ls *10parts.txt | sed -r \"s/-alignment-errors-10parts\.txt/\.tab/g\" | sed -r \"s/[^ ]*/\/rds\/project\/rds\-48gU72OtDNY\/europat\/paired\/fr\-en\/&/g\" >> rawFiles.lst"$'\n'
    exit 1
fi

rawList=${1:-files.lst}
errorsList=${2:-errors.lst}
n_parts=${3:-10}

N_ITEMS=$(cat $rawList | wc -l)

declare -a raw_files=()
declare -a error_files=()
declare -a results=()
while read -r item; do 
    raw_files+=( $item )
done < $rawList
while read -r item; do 
    error_files+=( $item )
done < $errorsList
echo Number of raw files: ${#raw_files[@]}
echo Number of error files: ${#error_files[@]}
total=0
for item in $(seq 0 $(( $N_ITEMS -1 ))); do lines=$(cat ${error_files[$item]} | wc -l) && size=$(ls -l ${raw_files[$item]} | awk '{printf("%d\n",$5)}') && \
                ratio=$(echo 1 | awk -v missing="$lines" -v total="$n_parts" '{printf("%2.4f\n", missing/total)}') && \
                unprocessed_size=$(echo 1 | awk -v rat="$ratio" -v siz="$size" '{printf("%f\n", rat*siz)}') && \
                total=$(echo $total | awk -v new_size="$unprocessed_size" '{printf("%12.2f\n", $1+new_size)}') &&
                echo ${raw_files[$item]} : $size, $ratio, $unprocessed_size &&
                result+=( $unprocessed_size );

done 
#echo ${result[@]}
#total_raw=$(du -c ${${raw_files[1]}//"$lang_pair"*/} | cut -f1)
echo Total size of unprocessed raw files for language pair $lang_pair: $total
#echo Total size raw for language pair $lang_pair: $total_raw
#echo % not processed: $(echo $total_raw | awk -v tot_unprocessed="$total" '{printf("%3.3f\n", tot_unprocessed/$1)}')
