#/bin/bash
set -e
logs="/Users/Shared/WordTeacher/backups/mongo/logs"
exec >> $logs
exec 2>&1

if [ -z ${WT_FTP_USER+x} ]; then
  echo "WT_FTP_USER variable isn't set"
  exit 1
fi
if [ -z ${WT_FTP_PASS+x} ]; then
  echo "WT_FTP_PASS variable isn't set"
  exit 2
fi

date=$(date '+%Y-%m-%d_%H-%M-%S')
uri="mongodb://192.168.0.78:27017/?directConnection=true&replicaSet=rs0"
out="/Users/Shared/WordTeacher/backups/mongo/$date"

echo $date
echo $uri
echo $out

mongodump --uri="$uri" --out="$out" --db=wiktionary

ncftpput -R -v -u $WT_FTP_USER -p $WT_FTP_PASS 192.168.0.1 usb1_1/mongo_backups "$out"

# short for localhost
# mongodump --uri="mongodb://localhost:27017/?directConnection=true&replicaSet=rs0" --out="/Users/Shared/WordTeacher/backups/mongo/wiktionaryAll" --db=wiktionary
