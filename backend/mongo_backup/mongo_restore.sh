#/bin/bash
set -e

if [ -z ${WT_FTP_USER+x} ]; then
  echo "WT_FTP_USER variable isn't set"
  exit 1
fi
if [ -z ${WT_FTP_PASS+x} ]; then
  echo "WT_FTP_PASS variable isn't set"
  exit 2
fi
if [ -z ${1+x} ]; then
  echo "argument with ftp folder name is required"
  exit 3
fi

uri="mongodb://192.168.0.78:27017/?directConnection=true&replicaSet=rs0"

echo $uri
echo $1

ncftpget -TRv -u $WT_FTP_USER -p $WT_FTP_PASS 192.168.0.1 ./ "usb1_1/mongo_backups/$1" 

mongorestore --uri="$uri" "./$1"
# add --drop to remove previous data before restoring

# short for localhost
# mongorestore --uri="mongodb://localhost:27017/?directConnection=true&replicaSet=rs0" "/Users/Shared/WordTeacher/backups/mongo/wiktionaryAll" 
