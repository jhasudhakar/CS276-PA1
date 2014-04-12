package cs276.util;

import java.util.Comparator;
import java.util.List;

/**
 * Created by weiwei on 4/12/14.
 */
public class ListLengthComparator implements Comparator<List<Integer>> {
    private int compareInt(int i, int j) {
        return i == j ? 0 : (i < j ? -1 : 1);
    }
    public int compare(List<Integer> l1, List<Integer> l2) {
        int len1 = l1.size();
        int len2 = l2.size();
        return compareInt(len1, len2);
    }
}
