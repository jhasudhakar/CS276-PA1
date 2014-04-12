package cs276.assignments;

import cs276.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.*;

// custom
import cs276.util.TermDocComparator;

public class Index {

    // Term id -> (position in index file, doc frequency) dictionary
    private static Map<Integer, Pair<Long, Integer>> postingDict
        = new TreeMap<Integer, Pair<Long, Integer>>();
    // Doc name -> doc id dictionary
    private static Map<String, Integer> docDict
        = new TreeMap<String, Integer>();
    // Term -> term id dictionary
    private static Map<String, Integer> termDict
        = new TreeMap<String, Integer>();
    // Block queue
    private static LinkedList<File> blockQueue
        = new LinkedList<File>();

    // Total file counter
    private static int totalFileCount = 0;
    // Document counter
    private static int docIdCounter = 0;
    // Term counter
    private static int wordIdCounter = 0;
    // Index
    private static BaseIndex index = null;


    /*
     * Write a posting list to the file
     * You should record the file position of this posting list
     * so that you can read it back during retrieval
     *
     * */
    private static void writePosting(FileChannel fc, PostingList posting)
            throws IOException {
        // update postingDict
        postingDict.put(posting.getTermId(), new Pair<Long, Integer>(fc.position(), posting.getList().size()));
        // write posting to file channel
        index.writePosting(fc, posting);
    }

    /**
     * Pop next element if there is one, otherwise return null
     * @param iter an iterator that contains integers
     * @return next element or null
     */
    private static Integer popNextOrNull(Iterator<Integer> iter) {
        if (iter.hasNext()) {
            return iter.next();
        } else {
            return null;
        }
    }

    /**
     * Merge two posting lists
     * @param p1
     * @param p2
     */
    private static PostingList mergePostings(PostingList p1, PostingList p2) {
        Iterator<Integer> iter1 = p1.getList().iterator();
        Iterator<Integer> iter2 = p2.getList().iterator();
        List<Integer> postings = new ArrayList<Integer>();
        Integer docId1 = popNextOrNull(iter1);
        Integer docId2 = popNextOrNull(iter2);
        Integer prevDocId = new Integer(0);
        while (docId1 != null && docId2 != null) {
            if (docId1 < docId2) {
                if (prevDocId < docId1) {
                    postings.add(docId1);
                    prevDocId = docId1;
                }
                docId1 = popNextOrNull(iter1);
            } else {
                if (prevDocId < docId2) {
                    postings.add(docId2);
                    prevDocId = docId2;
                }
                docId2 = popNextOrNull(iter2);
            }
        }

        while (docId1 != null) {
            if (prevDocId < docId1) {
                postings.add(docId1);
            }
            docId1 = popNextOrNull(iter1);
        }

        while (docId2 != null) {
            if (prevDocId < docId2) {
                postings.add(docId2);
            }
            docId2 = popNextOrNull(iter2);
        }

        return new PostingList(p1.getTermId(), postings);
    }

    public static void main(String[] args) throws IOException {
        /* Parse command line */
        if (args.length != 3) {
            System.err
                    .println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
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

        /* Get root directory */
        String root = args[1];
        File rootdir = new File(root);
        if (!rootdir.exists() || !rootdir.isDirectory()) {
            System.err.println("Invalid data directory: " + root);
            return;
        }

        /* Get output directory */
        String output = args[2];
        File outdir = new File(output);
        if (outdir.exists() && !outdir.isDirectory()) {
            System.err.println("Invalid output directory: " + output);
            return;
        }

        if (!outdir.exists()) {
            if (!outdir.mkdirs()) {
                System.err.println("Create output directory failure");
                return;
            }
        }

        /* BSBI indexing algorithm */
        File[] dirlist = rootdir.listFiles();

        // use ArrayList to collect all termID-docID pairs
        List<Pair<Integer, Integer>> pairs = new ArrayList<Pair<Integer, Integer>>();

        /* For each block */
        for (File block : dirlist) {
            File blockFile = new File(output, block.getName());
            blockQueue.add(blockFile);

            File blockDir = new File(root, block.getName());
            File[] filelist = blockDir.listFiles();

            /* For each file */
            for (File file : filelist) {
                ++totalFileCount;
                String fileName = block.getName() + "/" + file.getName();
                // use pre-increment to ensure docID > 0
                int docId = ++docIdCounter;
                docDict.put(fileName, docId);

                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.trim().split("\\s+");
                    for (String token : tokens) {
                        /*
                         * lookup/create term id
                         * accumulate <termId, docId>
                         */

                        int termId;
                        // if termDict contains the token already, do nothing
                        // else insert it and get new termID
                        if (!termDict.containsKey(token)) {
                            // use pre-increment to ensure termID > 0
                            termId = ++wordIdCounter;
                            termDict.put(token, termId);
                        } else {
                            termId = termDict.get(token);
                        }

                        // add termID-docID into pairs
                        pairs.add(new Pair(termId, docId));
                    }
                }
                reader.close();
            }

            /* Sort and output */
            if (!blockFile.createNewFile()) {
                System.err.println("Create new block failure.");
                return;
            }

            RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");

            // sort pairs
            Collections.sort(pairs, new TermDocComparator());

            // write output
            int cnt = 0, prevTermId = -1, termId, prevDocId = -1, docId;
            if (pairs.size() > 0)
                // set valid prevTermID
                prevTermId = pairs.get(0).getFirst();

            List<Integer> postings = new ArrayList<Integer>();
            for (Pair<Integer, Integer> p : pairs) {
                termId = p.getFirst();
                docId = p.getSecond();

                if (termId == prevTermId) {
                    // duplicate docIDs only added once
                    if (prevDocId != docId) {
                        postings.add(docId);
                    }
                    prevDocId = docId;
                } else {
                    // a different term is encountered
                    // should write postings of previous term to disk
                    writePosting(bfc.getChannel(), new PostingList(prevTermId, postings));

                    // start new postings
                    postings.clear();
                    postings.add(docId);
                    prevTermId = termId;
                    prevDocId = -1;
                }
            }

            bfc.close();
        }

        /* Required: output total number of files. */
        System.out.println(totalFileCount);

        /* Merge blocks */
        while (true) {
            if (blockQueue.size() <= 1)
                break;

            File b1 = blockQueue.removeFirst();
            File b2 = blockQueue.removeFirst();

            File combfile = new File(output, b1.getName() + "+" + b2.getName());
            if (!combfile.createNewFile()) {
                System.err.println("Create new block failure.");
                return;
            }

            RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
            RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
            RandomAccessFile mf = new RandomAccessFile(combfile, "rw");

            /*
             * merge two PostingList
             */
            FileChannel fc1 = bf1.getChannel();
            FileChannel fc2 = bf2.getChannel();
            FileChannel mfc = mf.getChannel();

            PostingList p1 = index.readPosting(fc1);
            PostingList p2 = index.readPosting(fc2);

            while (p1 != null && p2 != null) {
                int t1 = p1.getTermId();
                int t2 = p2.getTermId();

                if (t1 == t2) {
                    // merge postings of the same term
                    PostingList p3 = mergePostings(p1, p2);

                    // write p3 to disk
                    writePosting(mfc, p3);
                    p1 = index.readPosting(fc1);
                    p2 = index.readPosting(fc2);
                } else if (t1 < t2) {
                    // write p1
                    writePosting(mfc, p1);
                    p1 = index.readPosting(fc1);
                } else {
                    // write p2
                    writePosting(mfc, p2);
                    p2 = index.readPosting(fc2);
                }
            }

            bf1.close();
            bf2.close();
            mf.close();
            b1.delete();
            b2.delete();
            blockQueue.add(combfile);
        }

        /* Dump constructed index back into file system */
        File indexFile = blockQueue.removeFirst();
        indexFile.renameTo(new File(output, "corpus.index"));

        BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
                output, "term.dict")));
        for (String term : termDict.keySet()) {
            termWriter.write(term + "\t" + termDict.get(term) + "\n");
        }
        termWriter.close();

        BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
                output, "doc.dict")));
        for (String doc : docDict.keySet()) {
            docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
        }
        docWriter.close();

        BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
                output, "posting.dict")));
        for (Integer termId : postingDict.keySet()) {
            postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
                    + "\t" + postingDict.get(termId).getSecond() + "\n");
        }
        postWriter.close();
    }

}
