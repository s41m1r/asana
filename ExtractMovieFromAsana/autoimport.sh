#!/bin/bash
# from directory
# usage autoimport -u USER -p PWD -d DATABASE
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -u|--user)
    USER="$2"
    shift # past argument
    shift # past value
    ;;
    -p|--password)
    PASSWORD="$2"
    shift # past argument
    shift # past value
    ;;
    -d|--database)
    DATABASE="$2"
    shift # past argument
    shift # past value
    ;;
    --default)
    DEFAULT=""
    shift # past argument
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters

# printf  "%s\n"   "$USER"      "$PASSWORD"  "$DATABASE"
start=`date +%s`
for filename in *.csv; do
    #for ((i=0; i<=3; i++)); do
        #echo  $filename   extracted20190807/$(basename  $filename  .txt)_Log$i.txt 
    #done
    readarray -t eCollection < <(cut -d, -f12 $filename ) # name of file
    projName=$(echo ${eCollection[1]} | cut -d' ' -f2)
    projId=$(echo $filename| cut -d'/'  -f2 | cut -d'.'  -f1)
    tablename=${projId}"_"$projName
    use=$(echo "USE $DATABASE;")
    
    echo "Loading data into table: $tablename"

    create=$(echo "DROP TABLE IF EXISTS $tablename; CREATE TABLE $tablename (
   \`timestamp\`  varchar(23) CHARACTER SET utf8 DEFAULT NULL,
   \`taskId\`  bigint(16) DEFAULT NULL,
   \`parentTaskId\`  varchar(16) CHARACTER SET utf8 DEFAULT NULL,
   \`taskName\`  varchar(563) CHARACTER SET utf8 DEFAULT NULL,
   \`rawDataText\`  varchar(1145) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
   \`messageType\`  varchar(7) CHARACTER SET utf8 DEFAULT NULL,
   \`typeOfChange\`  int(2) DEFAULT NULL,
   \`typeOfChangeDescription\`  varchar(33) CHARACTER SET utf8 DEFAULT NULL,
   \`isRole\`  varchar(5) CHARACTER SET utf8 DEFAULT NULL,
   \`taskCreatedAt\`  varchar(23) CHARACTER SET utf8 DEFAULT NULL,
   \`createdByName\`  varchar(22) CHARACTER SET utf8 DEFAULT NULL,
   \`projectName\`  varchar(17) CHARACTER SET utf8 DEFAULT NULL,
   \`isCicle\`  varchar(5) CHARACTER SET utf8 DEFAULT NULL,
   \`createdById\`  varchar(16) CHARACTER SET utf8 DEFAULT NULL,
   \`assigneeId\`  varchar(15) CHARACTER SET utf8 DEFAULT NULL,
   \`assigneeName\`  varchar(13) CHARACTER SET utf8 DEFAULT NULL,
   \`eventId\`  varchar(16) CHARACTER SET utf8 DEFAULT NULL,
   \`projectId\`  bigint(13) DEFAULT NULL,
   \`workspaceId\`  bigint(12) DEFAULT NULL,
   \`workspaceName\`  varchar(9) CHARACTER SET utf8 DEFAULT NULL,
   \`isSubtask\`  varchar(5) CHARACTER SET utf8 DEFAULT NULL,
   \`parentTaskName\`  varchar(36) CHARACTER SET utf8 DEFAULT NULL,
   \`date\`  varchar(10) CHARACTER SET utf8 DEFAULT NULL,
   \`time\`  varchar(12) CHARACTER SET utf8 DEFAULT NULL,
   \`taskCompletedAt\`  varchar(24) CHARACTER SET utf8 DEFAULT NULL,
   \`taskModifiedAt\`  varchar(24) CHARACTER SET utf8 DEFAULT NULL,
   \`taskNotes\`  varchar(540) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;")

    load=$(echo "LOAD DATA LOCAL INFILE '$filename' 
        INTO TABLE $tablename CHARACTER SET utf8mb4 
        FIELDS TERMINATED BY ','  
        ENCLOSED BY '\"' LINES TERMINATED BY '\n' 
        IGNORE 1 ROWS;")

    #printf  "%s\n"   "$use" "$create" "$load"
    #echo "$use" "$create" "$load"
    mysql -u"$USER" -p"$PASSWORD" -e"$use""$create""$load"

done
end=`date +%s`

runtime=$((end-start))

echo "All done in $runtime seconds."




