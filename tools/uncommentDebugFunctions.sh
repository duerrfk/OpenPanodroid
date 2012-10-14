#!/bin/sh

find ../src -name "*\.java" | xargs grep -l 'Log\.' | xargs sed -i 's/\/\/Log\./Log\./g'

find ../src -name "*\.java" | xargs grep -l 'Assert\.' | xargs sed -i 's/\/\/Assert\./Assert\./g'