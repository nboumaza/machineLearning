#!/usr/bin/env bash

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin"; pwd`
script=`basename $0`

auth_token=
auth_file="$HOME/.cdap.accesstoken"

function get_auth_token() {
  if [ -f $auth_file ]; then
    auth_token=`cat $auth_file`
  fi
}

function usage() {
  echo "Tool for sending data to the MovieRecommender application"
  echo "Usage: $script [--host <hostname>]"
  echo ""
  echo "  Options"
  echo "    --host      Specifies the host that CDAP is running on. (Default: localhost)"
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

#  get the access token
get_auth_token

OLD_IFS=IFS
IFS=$'\n'
lines=`cat "$bin"/../resources/ratings.dat`
for line in $lines
do
  status=`curl -qSfsw "%{http_code}\\n" -H "Authorization: Bearer $auth_token" -X POST -d "$line" http://$gateway:11015/v3/namespaces/cognitivescale/streams/$stream`
  if [ $status -ne 200 ]; then
    echo "Failed to send data."
    if [ $status == 401 ]; then
      if [ "x$auth_token" == "x" ]; then
        echo "No access token provided"
      else
        echo "Invalid access token"
      fi
    fi
    echo "Exiting program..."
    exit 1;
  fi
done

status=`curl -qSfsw "%{http_code}\\n" -H "Authorization: Bearer $auth_token" -X POST --data-binary @"$bin"/../resources/movies.dat http://$gateway:11015/v3/namespaces/cognitivescale/apps/MovieRecommender/services/MovieDictionaryService/methods/storemovies`

if [ $status -ne 200 ]; then
  echo "Failed to send data."
  if [ $status == 401 ]; then
    if [ "x$auth_token" == "x" ]; then
      echo "No access token provided"
    else
      echo "Invalid access token"
    fi
  fi
  echo "Exiting program..."
  exit 1;
fi

IFS=$OLD_IFS
