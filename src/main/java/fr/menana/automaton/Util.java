/**
 * Automaton
 * Copyright (c) 2015, Julien Menana, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package fr.menana.automaton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * This class provides some tools and algorithm to help writing more complex algorithm
 * Created by Julien Menana on 02/05/2015.
 */
public class Util {

    /**
     * Construct the powerset of a given set
     * @param list the list of elements
     * @param <T> the type of the elements
     * @return the list of all the subsets that can be constructed from the given set
     */
    @SuppressWarnings("unused")
    public static <T> List<List<T>> powerset(Collection<T> list) {
        List<List<T>> ps = new ArrayList<>();
        ps.add(new ArrayList<>());

        for (T item : list) {
            List<List<T>> newPs = new ArrayList<>();

            for (List<T> subset : ps) {
                newPs.add(subset);
                List<T> newSubset = new ArrayList<>(subset);
                newSubset.add(item);
                newPs.add(newSubset);
            }
            ps = newPs;
        }
        return ps;
    }

    /**
     * A comparator to sort list in descending size order
     */
    @SuppressWarnings("unused")
    public static ListSizeComparator listSizeComparator =  new ListSizeComparator();

    /**
     * A comparator to sort list in descending size order
     */
    public static class ListSizeComparator implements Comparator<List> {

        public int compare(List o1, List o2) {
            return o2.size() - o1.size();
        }
    }

    /**
     * Checks if a given {@link java.lang.String} is an integer
     * @param string the   {@link java.lang.String} to check
     * @return <code>true</code>  if and only if the given {@link java.lang.String} is an integer
     */
    public static boolean isInteger(String string) {
        return isInteger(string,10);
    }

    private static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
}
