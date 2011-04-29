package org.jenkins.tools.test.model.comparators;

import java.text.ParseException;
import java.util.Comparator;

public class VersionComparator implements Comparator<String> {
    public int compare(String o1, String o2) {

        String[] splittedO1Version = o1.split("\\.|-");
        String[] splittedO2Version = o2.split("\\.|-");

        for(int i=0; i<splittedO1Version.length; i++){
            if(i >= splittedO2Version.length){
                return 1;
            }

            Comparable chunk1 = null;
            try {
                chunk1 = Integer.valueOf(splittedO1Version[i]);
            }catch(NumberFormatException e){
                chunk1 = splittedO1Version[i];
            }

            Comparable chunk2 = null;
            try {
                chunk2 = Integer.valueOf(splittedO2Version[i]);
            }catch(NumberFormatException e){
                chunk2 = splittedO2Version[i];
            }

            if(!splittedO1Version[i].equals(splittedO2Version[i])){
                return chunk1.compareTo(chunk2);
            }
        }

        if(splittedO1Version.length == splittedO2Version.length){
            return 0;
        } else {
            return -1;
        }
    }
}
