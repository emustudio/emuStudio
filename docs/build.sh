#!/bin/bash

echo Deleting generated sites
rm -rf ./docs/
rm -rf ./images/
bundle exec jekyll clean

echo Building site...
cd _documentation || exit
. build.sh
cd ..

JEKYLL_ENV=production bundle exec jekyll build --verbose

echo Checking images...
find docs/ -type f -regex ".*\.\(html\)" -print0 | xargs -0 grep "{imagepath}"
if [[ "$?" -eq 0 ]]; then
  echo "  Problem"
  exit 1
fi

echo Checking if baseurl ends with slash...
grep -r href=\" . | grep 'href=\"{{ *site\.baseurl *}}[^/{]' | grep -vE _posts\|_site
if [[ "$?" -eq 0 ]]; then
  echo "  Problem"
  exit 1
fi
