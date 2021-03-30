#!/bin/sh

cd `dirname $0`;

DIDCOPY=0

CELESTIA_ROOT=`pwd`/../base_assets/src/main/assets/CelestiaResources
CELESTIA_REPO_ROOT=`pwd`/../../Celestia

mkdir -p $CELESTIA_ROOT

CELESTIA_CONTENT_REPO_ROOT=$CELESTIA_REPO_ROOT/content

for directory in 'images' 'locale' 'scripts' 'shaders';do
    f=$CELESTIA_REPO_ROOT/$directory
    if [ $f -nt $CELESTIA_ROOT/$directory ];then
        echo "rsync -rv --quiet --exclude=CMakeLists.txt $f $CELESTIA_ROOT"
        rsync -rv --quiet --exclude=CMakeLists.txt $f $CELESTIA_ROOT
        DIDCOPY=1
    fi
done

for directory in 'data' 'extras' 'extras-standard' 'models' 'textures';do
    f=$CELESTIA_CONTENT_REPO_ROOT/$directory
    if [ $f -nt $CELESTIA_ROOT/$directory ];then
        echo "rsync -rv --quiet --exclude=CMakeLists.txt $f $CELESTIA_ROOT"
        rsync -rv --quiet --exclude=CMakeLists.txt $f $CELESTIA_ROOT
        DIDCOPY=1
    fi
done

for file in "celestia.cfg" "controls.txt" "demo.cel" "guide.cel" "start.cel" "COPYING" "AUTHORS" "TRANSLATORS";do
    f=$CELESTIA_REPO_ROOT/$file
    if [ $f -nt $CELESTIA_ROOT/$file ];then
        echo "cp $f $CELESTIA_ROOT/$file"
        cp $f $CELESTIA_ROOT/$file
    fi
done
