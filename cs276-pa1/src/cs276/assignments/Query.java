package cs276.assignments;

import cs276.util.ListLengthComparator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.*;

public class Query {

    // Term id -> position in index file
    private static Map<Integer, Long> posDict = new TreeMap<Integer, Long>();
    // Term id -> document frequency
    private static Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();
    // Doc id -> doc name dictionary
    private static Map<Integer, String> docDict = new TreeMap<Integer, String>();
    // Term -> term id dictionary
    private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
    // Index
    private static BaseIndex index = null;


    /*
     * Write a posting list with a given termID from the file
     * You should seek to the file position of this specific
     * posting list and read it back.
     * */
    private static PostingList readPosting(FileChannel fc, int termId)
            throws IOException {
        // first seek to the file position of this specific posting list
        if (posDict.containsKey(termId)) {
            // read it back
            return index.readPosting(fc.position(posDict.get(termId)));
        }
        return null;
    }

    /**
     * Pop next element if there is one, otherwise return null
     * @param p iterator
     * @param <X> class
     * @return next element or null
     */
    static <X> X popNextOrNull(Iterator<X> p) {
        if (p.hasNext()) {
            return p.next();
        } else {
            return null;
        }
    }

    /**
     * Intersect two posting lists
     * @param p1
     * @param p2
     * @return the intersection of two posting lists
     */
    static List<Integer> intersect(Iterator<Integer> p1, Iterator<Integer> p2) {
        List<Integer> answer = new ArrayList<Integer>();
        Integer docId1 = popNextOrNull(p1);
        Integer docId2 = popNextOrNull(p2);

        while (docId1 != null && docId2 != null) {
            if (docId1 == docId2) {
                answer.add(docId1);
                docId1 = popNextOrNull(p1);
                docId2 = popNextOrNull(p2);
            }
            else if (docId1 < docId2) {
                docId1 = popNextOrNull(p1);
            } else {
                docId2 = popNextOrNull(p2);
            }
        }

        return answer;
    }

    /**
     * Output query results to stdout
     * @param res
     */
    static void outputQueryResult(List<Integer> res) {
        if (res == null || res.size() == 0) {
            System.out.println("no results found");
            return;
        }
        List<String> output = new ArrayList<String>();
        for (Integer i : res) {
            //document file name should only include subdirectory name and then the file name
            assert(docDict.containsKey(i));
            output.add(docDict.get(i));
        }
        Collections.sort(output);
        for (String s : output) {
            System.out.println(s);
        }
    }

    public static void main(String[] args) throws IOException {
        /* Parse command line */
        if (args.length != 2) {
            System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
            return;
        }

        /* Get index */
        String className = "cs276.assignments." + args[0] + "Index";
        try {
            Class<?> indexClass = Class.forName(className);
            index = (BaseIndex) indexClass.newInstance();
        } catch (Exception e) {
            System.err
                    .println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

        /* Get index directory */
        String input = args[1];
        File inputdir = new File(input);
        if (!inputdir.exists() || !inputdir.isDirectory()) {
            System.err.println("Invalid index directory: " + input);
            return;
        }

        /* Index file */
        RandomAccessFile indexFile = new RandomAccessFile(new File(input,
                "corpus.index"), "r");

        String line = null;
        /* Term dictionary */
        BufferedReader termReader = new BufferedReader(new FileReader(new File(
                input, "term.dict")));
        while ((line = termReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            termDict.put(tokens[0], Integer.parseInt(tokens[1]));
        }
        termReader.close();

        /* Doc dictionary */
        BufferedReader docReader = new BufferedReader(new FileReader(new File(
                input, "doc.dict")));
        while ((line = docReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
        }
        docReader.close();

        /* Posting dictionary */
        BufferedReader postReader = new BufferedReader(new FileReader(new File(
                input, "posting.dict")));
        while ((line = postReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
            freqDict.put(Integer.parseInt(tokens[0]),
                    Integer.parseInt(tokens[2]));
        }
        postReader.close();

        /* Processing queries */
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        /* For each query */
        boolean termNotFound = false;
        while ((line = br.readLine()) != null) {
            // tokens in the query are separated by space
            String[] tokens = line.split("\\s+");
            // get posting list for each query word
            List<List<Integer>> postingLists = new ArrayList<List<Integer>>();
            for (String token : tokens) {
                if (termDict.containsKey(token)) {
                    postingLists.add(readPosting(indexFile.getChannel(), termDict.get(token)).getList());
                } else {
                    // if the query word is not indexed, simply return "no results found"
                    termNotFound = true;
                }
            }
            if (termNotFound) {
                termNotFound = false;
                System.out.println("no results found");
                continue;
            }
            // order the terms by postings list length to optimize query performance
            Collections.sort(postingLists, new ListLengthComparator());
            // posting list intersection
            Iterator<List<Integer>> iter = postingLists.iterator();
            List<Integer> pl1 = popNextOrNull(iter), pl2;
            while (pl1 != null && (pl2 = popNextOrNull(iter)) != null) {
                // intersect two posting lists
                pl1 = intersect(pl1.iterator(), pl2.iterator());
            }
            // output result
            outputQueryResult(pl1);
        }
        br.close();
        indexFile.close();
    }
}
