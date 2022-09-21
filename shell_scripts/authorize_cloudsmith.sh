#!/bin/bash

echo $1 | dart pub token add https://dart.cloudsmith.io/alkami/fluttercore/
echo $1 | dart pub token add https://dart.cloudsmith.io/alkami/flutterpackages/
