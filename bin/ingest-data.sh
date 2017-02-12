#!/usr/bin/env bash

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin"; pwd`
script=`basename $0`


function usage() {
  echo "Tool for sending data to the MovieRecommender application"
  echo "Usage: $script [--host <hostname>]"
  echo ""
  echo "  Options"
  echo "    --host      Specifies the host that CORTEX is running on. (Default: localhost)"
  echo "    --help      This help message"
  echo ""
}

gateway="localhost"
stream="ratingsStream"
while [ $# -gt 0 ]
do
  case "$1" in
    --host) shift; gateway="$1"; shift;;
         *)  usage; exit 1
   esac
done


#OLD_IFS=IFS
#IFS=$'\n'
lines=`cat "$bin"/../resources/ratings.dat`
for line in $lines
do
  status=`curl -qSfsw "%{http_code}\\n" -X POST -d "$line" http://$gateway:11015/v3/namespaces/default/streams/$stream`
  if [ $status -ne 200 ]; then
    echo "Failed to send data."
    echo "Exiting..."
    exit 1;
  fi
done

status=`curl -qSfsw "%{http_code}\\n"  -X POST --data-binary @"$bin"/../resources/movies.dat http://$gateway:11015/v3/namespaces/default/apps/MovieRecommender/services/MovieStoreService/methods/storemovies`

if [ $status -ne 200 ]; then
  echo "Failed to send data."
  echo "Exiting..."
  exit 1;
fi

#IFS=$OLD_IFS
