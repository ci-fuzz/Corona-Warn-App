#!/bin/bash -eu
# Here you can provide a script to build your project. Here you should provide
# the steps as you would normally build your code. The CI security suite will
# then automatically perform all needed adjustments necessary to build the fuzzers
# based on the provided configurations.

mvn install -DskipTests
