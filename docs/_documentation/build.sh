#!/usr/bin/env bash

current=`pwd`
for d in `find . -name '_config.yml'`; do
  cd `dirname $d`
  JEKYLL_ENV=production bundler exec jekyll build
  cd $current
done