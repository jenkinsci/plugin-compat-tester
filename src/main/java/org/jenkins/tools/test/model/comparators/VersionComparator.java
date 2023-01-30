/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkins.tools.test.model.comparators;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Class used to compare 2 plugin versions
 *
 * @author Frederic Camblor
 */
public class VersionComparator implements Comparator<String>, Serializable {
    @Override
    public int compare(String o1, String o2) {

        String[] splitO1Version = o1.split("\\.|-");
        String[] splitO2Version = o2.split("\\.|-");

        for(int i=0; i<splitO1Version.length; i++){
            if(i >= splitO2Version.length){
                return 1;
            }

            Comparable chunk1;
            try {
                chunk1 = Integer.valueOf(splitO1Version[i]);
            }catch(NumberFormatException e){
                chunk1 = splitO1Version[i];
            }

            Comparable chunk2;
            try {
                chunk2 = Integer.valueOf(splitO2Version[i]);
            }catch(NumberFormatException e){
                chunk2 = splitO2Version[i];
            }

            if (chunk1.getClass() != chunk2.getClass()) {
                throw new IllegalArgumentException("Comparing different types in chunk " + i +
                        ". Version 1 = " + o1 + ", version 2 = " + o2);
            }

            if(!splitO1Version[i].equals(splitO2Version[i])){
                return chunk1.compareTo(chunk2);
            }
        }

        if(splitO1Version.length == splitO2Version.length){
            return 0;
        } else {
            return -1;
        }
    }
}
