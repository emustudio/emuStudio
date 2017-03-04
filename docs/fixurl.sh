#!/bin/bash

echo Deleting generated sites
find _docuser/ -type f -regex ".*\.\(html\)" -exec rm {} \;
find _docdevel/ -type f -regex ".*\.\(html\)" -exec rm {} \;
rm -rf ./images/

echo Building site...
jekyll clean
JEKYLL_ENV=production jekyll build --verbose

echo Copying results...
cp -rf _site/docuser/* _docuser/
cp -rf _site/docdevel/* _docdevel/
cp -r _site/images images/

echo Fixing URLs...
find _docuser/ -type f -print0 | xargs -0 sed -i 's_http://github\.com/pages/vbmacher/emuStudio_https://vbmacher.github.io/emuStudio_g'
find _docdevel/ -type f -print0 | xargs -0 sed -i 's_http://github\.com/pages/vbmacher/emuStudio_https://vbmacher.github.io/emuStudio_g'

echo Fixing image URLs...
find _docuser/ -type f -print0 | xargs -0 sed -i 's_<img src="/docuser/_<img src="https://vbmacher.github.io/emuStudio/docuser/_g'
find _docuser/ -type f -print0 | xargs -0 sed -i 's_<img src="/images/_<img src="https://vbmacher.github.io/emuStudio/images/_g'
find _docdevel/ -type f -print0 | xargs -0 sed -i 's_<img src="/docdevel/_<img src="https://vbmacher.github.io/emuStudio/docdevel/_g'
find _docdevel/ -type f -print0 | xargs -0 sed -i 's_<img src="/images/_<img src="https://vbmacher.github.io/emuStudio/images/_g'

echo Checking images...
find _docuser/ -type f -regex ".*\.\(html\)" -print0 | xargs -0 grep "{imagepath}"
find _docdevel/ -type f -regex ".*\.\(html\)"  -print0 | xargs -0 grep "{imagepath}"

echo Copying lost javadoc...
unzip -o ./emuLib-9.0.0-javadoc.jar -d _docdevel/emulib_javadoc/



