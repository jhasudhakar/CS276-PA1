package cs276.util;

import java.util.Comparator;

/**
 * Sorted by termID first, followed by docID.
 *
 * @author Wei Wei
 */
public class TermDocComparator implements Comparator<Pair<Integer, Integer>> {
    public int compare(Pair<Integer, Integer> x, Pair<Integer, Integer> y) {
        int xFirst = x.getFirst();
        int xSecond = x.getSecond();
        int yFirst = y.getFirst();
        int ySecond = y.getSecond();

        if (xFirst == yFirst) {
            return xSecond - ySecond;
        } else {
            return xFirst - yFirst;
        }
    }
}
