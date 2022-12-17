
ROOT=$(dirname $0)
BRANCH=$(git rev-parse --abbrev-ref HEAD)
NAME=$(basename $(realpath -s $ROOT))

mvn clean package

BRANCH_NAME_LOWER=`echo $BRANCH | tr '[:upper:]' '[:lower:]'`
docker build . -t $NAME:$BRANCH_NAME_LOWER

#docker push $NAME:$BRANCH_NAME_LOWER
