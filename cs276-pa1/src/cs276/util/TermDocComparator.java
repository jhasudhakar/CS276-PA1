package cs276.util;

import java.util.Comparator;

/**
 * Sorted by termID first, followed by docID.
 *
 * @author Wei Wei
 */
public class TermDocComparator implements Comparator<Pair<Integer, Integer>> {
    // why not simply use i - j?
    // see http://www.cpylua.org/root-of-all-evil/
    private int compareInt(int i, int j) {
        return i == j ? 0 : (i < j ? -1 : 1);
    }

    public int compare(Pair<Integer, Integer> x, Pair<Integer, Integer> y) {
        int termID1 = x.getFirst();
        int docID1 = x.getSecond();
        int termID2 = y.getFirst();
        int docID2 = y.getSecond();

        int res = compareInt(termID1, termID2);
        return res != 0 ? res : compareInt(docID1, docID2);
    }
}
