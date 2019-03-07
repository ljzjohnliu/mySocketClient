#!/bin/sh
#/***************************************************************************
# * 
# * Copyright (c) 2017 qiyi.com, Inc. All Rights Reserved
# * Author liuniu@qiyi.com
# * 
# **************************************************************************/

# gradle 编译
gradle_compile_release()
{
    echo "gradle build Release begin"
    ./gradlew clean
    ./gradlew assembleRelease
    if [ $? -ne 0 ] ; then
    echo "------Build on Host FAIL------";
    return -1;
    fi
    getoutput_release
    echo "gradle build Release end"
}

# gradle 编译
gradle_compile_debug()
{
    echo "gradle build Debug begin"
    ./gradlew clean
    ./gradlew assembleDebug
    if [ $? -ne 0 ] ; then
    echo "------Build on Host FAIL------";
    return -1;
    fi
    getoutput_debug
    echo "gradle build Debug end"
}

# release产物拷贝到output目录
getoutput_release()
{
    echo "release delete userless folders"
        rm -rf output
    echo "release delete output end"

    mkdir -p output/apk

    echo "release copy build outputs begin"
    ls ./app/build/outputs/apk
    cp ./app/build/outputs/apk/release/*.apk ./output/apk/
    echo "release copy build outputs end"
}

# debug
getoutput_debug()
{
    echo "debug delete userless folders"
        rm -rf output
    echo "debug delete output end"

    mkdir -p output/apk

    echo "debug copy build outputs begin"
    ls ./app/build/outputs/apk
    cp ./app/build/outputs/apk/debug/*.apk ./output/apk/
    echo "debug copy build outputs end"
}

Main() 
{
    if [ "$1" == "0" ]; then
       gradle_compile_debug
    elif [ "$1" == "1" ]; then
       gradle_compile_release
    fi

    return $?
}

Main "$@" 