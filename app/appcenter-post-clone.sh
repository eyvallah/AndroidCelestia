#!/usr/bin/env bash

cd $APPCENTER_SOURCE_DIRECTORY
ln -s app/src/main/cpp/dependency/android app/src/main/cpp/libs

cd ..

echo "Building for branch releases/1.1.1"

# Clone the Celestia repo (modified)
git clone https://github.com/${GITHUB_USERNAME}/Celestia --branch releases/1.1.1 --single-branch
cd Celestia
git submodule update --init
cd ..

# Install gettext, needed for translation
brew install gettext

# Install appcenter cli to upload symbols
set -ex
NODE_VERSION="14.9.0"
curl "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}.pkg" > "$HOME/Downloads/node-installer.pkg"
sudo installer -store -pkg "$HOME/Downloads/node-installer.pkg" -target "/"

npm install -g appcenter-cli
