#!/bin/sh
#*******************************************************************************
# Copyright (c) 2011, 2020 University of Southampton.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#    University of Southampton - initial API and implementation
#*******************************************************************************
BASENAME=$1
TEXFile="$BASENAME.tex"
HTML_FOLDER=$2

echo "rm -rf $HTML_FOLDER"
rm -rf $HTML_FOLDER

echo "plastex -d $HTML_FOLDER --theme=python $TEXFile"
plastex -d $HTML_FOLDER --theme=python $TEXFile

echo "sed -f sed_commands -i '' $HTML_FOLDER/eclipse-toc.xml"
sed -f sed_commands -i '' $HTML_FOLDER/eclipse-toc.xml
