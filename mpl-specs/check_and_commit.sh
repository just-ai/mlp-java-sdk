set -e

BASEDIR=$(dirname "$0")
cd $BASEDIR/..

CHANGES=$(git status -s)

if [ -nz "$CHANGES" ]; then

  git commit -m "Update API spec" mpl-specs
  git push

  #exit 1 ??
fi