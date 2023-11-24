set -e
set -v

BASEDIR=$(dirname "$0")
cd $BASEDIR

BRANCH=$(git rev-parse --abbrev-ref HEAD)

if [ $BRANCH == 'dev' ]; then
  SERVER=https://mlp.caila-ci-dev.lo.test-ai.net
fi
if [ $BRANCH == 'stable' ]; then
  SERVER=https://mlp.caila-stable.test-ai.net
fi
if [ $BRANCH == 'release' ]; then
  SERVER=https://mlp.caila-stable.test-ai.net
fi

if [ -z $SERVER ]; then
  echo 221 $SERVER
  # check parallel env exists
  SRV=https://mlp.$BRANCH.caila-ci-feature.lo.test-ai.net
  CHECK=$(curl -si $SRV/api/mlpgate/version | head -n 1 | grep 200 || true)
  if [ "$CHECK" ]; then
    SERVER=$SRV
  fi
fi

echo 11 $SERVER

echo $SERVER

if [ ! -z $SERVER ]; then

  echo go
  curl --silent $SERVER/static/mlpgate/api-docs.yaml -o mlp-rest-api.yml
  sed "s/- url:.*/- url: https:\/\/app.caila.io/g" -i mlp-rest-api.yml

  curl --silent $SERVER/static/mlpgate/mlp-grpc.proto -o mlp-grpc.proto

fi
