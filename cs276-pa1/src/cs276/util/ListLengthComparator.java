package cs276.util;

import cs276.assignments.PostingList;

import java.util.Comparator;

/**
 * Created by weiwei on 4/12/14.
 */
public class ListLengthComparator implements Comparator<PostingList> {
    private int compareInt(int i, int j) {
        return i == j ? 0 : (i < j ? -1 : 1);
    }
    public int compare(PostingList p1, PostingList p2) {
        int p1len = p1.getList().size();
        int p2len = p2.getList().size();
        return compareInt(p1len, p2len);
    }
}
