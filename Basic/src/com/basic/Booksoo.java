package com.basic;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class Booksoo {

    enum StopMode{
        BY_TIME,
        BY_REPS,
    }

    enum Filename{
        a_example,
        b_read_on,
        c_incunabula,
        d_tough_choices,
        e_so_many_books,
        f_libraries_of_the_world
    }
    static final boolean DEBUG = false;
    static final long TIME_CAP = 10 * 1000000000L;
    static final int REPS = 1000;
    static final Books.StopMode stopMode= Books.StopMode.BY_TIME;
    static final Books.Filename filename = Books.Filename.c_incunabula;

    //long l = (((long)x) << 32) | (y & 0xffffffffL);
    //int x = (int)(l >> 32);
    //int y = (int)l;



    public static void main(String[] args) {
        long startTime = System.nanoTime();
        int randomTries = 0;
        List<Integer> list = new ArrayList<Integer>();
        //File file = new File("c:\\tmp\\d_quite_big.in");
        File file = new File("C:\\tmp\\" + filename + ".txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Pattern p = Pattern.compile("[\\s]+");
            String[] ln = reader.readLine().split(p.pattern());
            int _numBooks = Integer.parseInt(ln[0]);
            int numLibraries = Integer.parseInt(ln[1]);
            int[] signTime = new int[numLibraries];
            int[] parallelBooks = new int[numLibraries];
            int[] librarySize = new int[numLibraries];
            int[][] books = new int[numLibraries][];
            //long[][] valuedBooks = new long[numLibraries][];
            int numDays = Integer.parseInt(ln[2]);
            ln = reader.readLine().split(p.pattern());
            int[] bookValues = Arrays.stream(ln).mapToInt(Integer::parseInt).toArray();
            int numBooks = bookValues.length;
            if (_numBooks != numBooks) System.out.println("!!! Bad file format (first line first parameter ignored)\n");
            for (int i = 0; i < numLibraries; i++) {
                ln = reader.readLine().split(p.pattern());
                librarySize[i] = Integer.parseInt(ln[0]);
                signTime[i] = Integer.parseInt(ln[1]);
                parallelBooks[i] = Integer.parseInt(ln[2]);
                ln = reader.readLine().split(p.pattern());
                books[i] = Arrays.stream(ln).mapToInt(Integer::parseInt).toArray();
                //valuedBooks[i] = new long[books[i].length];
                //for (int j = 0; j < books[i].length; j++) {
                //    valuedBooks[i][j] = (((long) bookValues[books[i][j]]) << 32) | (books[i][j] & 0xffffffffL);
                //}
            }

            orderByValue(books,bookValues);

            Result winner = randomLibValueFit(numDays, numLibraries, numBooks, books, librarySize, signTime, parallelBooks, bookValues);
            System.out.println("["+ randomTries + "] Current score: " + winner.getScore());
            switch (stopMode)
            {
                case BY_TIME:
                    do {
                        Result candidate = randomLibValueFit(numDays, numLibraries, numBooks, books, librarySize, signTime, parallelBooks, bookValues);
                        if (winner.getScore() < candidate.getScore()){
                            System.out.println("["+ randomTries + "] Current score: " + winner.getScore());
                            winner = candidate;
                        }
                        randomTries++;
                    } while (System.nanoTime() - startTime < TIME_CAP);
                    break;
                case BY_REPS:
                    for (int i = 0; i < REPS; i++) {
                        Result candidate = randomLibValueFit(numDays, numLibraries, numBooks, books, librarySize, signTime, parallelBooks, bookValues);
                        if (winner.getScore() < candidate.getScore()) {
                            System.out.println("["+ randomTries + "] Current score: " + winner.getScore());
                            winner = candidate;
                        }
                        randomTries++;
                    }
                    break;
            }

            int[][] resultBooks = winner.getResultBooks();
            int[] librariesOrder = winner.getLibrariesOrder();
            int[] resultSize = winner.getResultSize();
            int lastlibrary = winner.getLastlibrary();

            StringBuilder outSb = new StringBuilder(DEBUG?"----SOLUTION----\n":"");
            outSb.append(lastlibrary).append('\n');
            for (int i = 0; i < lastlibrary; i++) {
                if (resultSize[librariesOrder[i]] > 0) {
                    outSb.append(librariesOrder[i]).append(" ").append(resultSize[librariesOrder[i]]).append('\n');
                    outSb.append(arrayToNString(resultBooks[librariesOrder[i]], resultSize[librariesOrder[i]])).append('\n');
                }
            }

            if (DEBUG) System.out.print(outSb.toString());
            else {
                BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\tmp\\result_" + filename + ".txt"));
                writer.write(outSb.toString());
                writer.close();
            }
            System.out.println("Ended in: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Result fullRandomFit(int numDays, int numLibraries, int numBooks, int[][] books, int[] librarySize, int[] signTime, int[] parallelBooks, int[] bookValues)
    {
        Random rand = new Random();
        int score = 0;
        int dayOfLogin = 0;
        int loging = -1;
        int lastLibrary = 0;
        boolean[] working = new boolean[numLibraries];
        boolean[] finished = new boolean[numLibraries];
        int[][] resultBooks = new int[numLibraries][];
        int[] resultSize = new int[numLibraries];
        int[] librariesOrder = new int[numLibraries];
        boolean[] usedBooks = new boolean[numBooks];

        for (int i = 0; i < numLibraries; i++) {
            resultBooks[i] = new int[books[i].length];
            resultSize[i] = 0;
        }


        for(int day = 0; day < numDays; day++)
        {
            /*if (0 == day % (numDays / 40)) {
                System.out.println(day + "/" + numDays);
                System.out.println(loging + " -> " + dayOfLogin);
                System.out.println(Arrays.toString(working));
                System.out.println(Arrays.toString(finished));
                for (int pr = 0; pr < numLibraries; pr ++)
                {
                    System.out.print(resultSize[pr] - librarySize[pr] + " ");
                }
                DEBUG = true;
            }
            else {
                DEBUG = false;
            }*/

            if (DEBUG)
                System.out.println("Starts day " + day + " with: " + score);
            //SELECT LIBRARY
            if (day == dayOfLogin) {
                //FINISH LOGGING:
                if (loging >= 0) {
                    working[loging] = true;
                    if (DEBUG)
                        System.out.println("library " + loging + " starts.");
                }
                int nextLibrary = rand.nextInt(numLibraries);
                for (int i = 0; i < numLibraries; i++) {
                    if (!finished[nextLibrary] && !working[nextLibrary]) {
                        librariesOrder[lastLibrary++] = nextLibrary;
                        loging = nextLibrary;
                        dayOfLogin += signTime[nextLibrary];
                        if (DEBUG)
                            System.out.println("library " + nextLibrary + " selected to be started in: " + dayOfLogin);
                        break;
                    } else {
                        nextLibrary = (nextLibrary + 1) % numLibraries;
                    }
                }
            }

            for (int i = 0; i < numLibraries; i++) {
                if (working[i]) {
                    int maxNeedReps = Math.min(librarySize[i]-resultSize[i], parallelBooks[i]);
                    parallel: for (int r = 0; r < maxNeedReps; r++) {
                        if (resultSize[i] == librarySize[i]) {
                            working[i] = false;
                            finished[i] = true;
                            break parallel;
                        }
                        int nextBook = rand.nextInt(librarySize[i]);
                        books: for (int j = 0; j < librarySize[i]; j++) {
                            if (!usedBooks[books[i][nextBook]]) {
                                resultBooks[i][resultSize[i]] = books[i][nextBook];
                                resultSize[i]++;
                                usedBooks[books[i][nextBook]] = true;
                                score += bookValues[books[i][nextBook]];
                                if (DEBUG)
                                    System.out.println("library " + i + " sends book: " + books[i][nextBook] + "(" + bookValues[books[i][nextBook]] + ")");
                                break books;
                            } else {
                                nextBook = (nextBook + 1) % librarySize[i];
                            }
                        }
                    }
                }
            }
            if (DEBUG)
                System.out.println("Ends day " + day + " with: " + score + "\n--------");
        }
        return new Result(lastLibrary-1,score,resultSize,librariesOrder,resultBooks);
    }

    private static Result randomLibValueFit(int numDays, int numLibraries, int numBooks, int[][] books, int[] librarySize, int[] signTime, int[] parallelBooks, int[] bookValues)
    {
        Random rand = new Random();
        int score = 0;
        int dayOfLogin = 0;
        int loging = -1;
        int lastLibrary = 0;
        boolean[] working = new boolean[numLibraries];
        boolean[] finished = new boolean[numLibraries];
        int[][] resultBooks = new int[numLibraries][];
        int[] resultSize = new int[numLibraries];
        int[] librariesOrder = new int[numLibraries];
        int[] lastBook = new int[numLibraries];
        boolean[] usedBooks = new boolean[numBooks];

        for (int i = 0; i < numLibraries; i++) {
            resultBooks[i] = new int[books[i].length];
            resultSize[i] = 0;
        }

        for(int day = 0; day < numDays; day++)
        {
            if (DEBUG)
                System.out.println("Starts day " + day + " with: " + score);
            //SELECT LIBRARY
            if (day == dayOfLogin) {
                //FINISH LOGGING:
                if (loging >= 0) {
                    working[loging] = true;
                    if (DEBUG)
                        System.out.println("library " + loging + " starts.");
                }
                int nextLibrary = rand.nextInt(numLibraries);
                for (int i = 0; i < numLibraries; i++) {
                    if (!finished[nextLibrary] && !working[nextLibrary]) {
                        librariesOrder[lastLibrary++] = nextLibrary;
                        loging = nextLibrary;
                        dayOfLogin += signTime[nextLibrary];
                        if (DEBUG)
                            System.out.println("library " + nextLibrary + " selected to be started in: " + dayOfLogin);
                        break;
                    } else {
                        nextLibrary = (nextLibrary + 1) % numLibraries;
                    }
                }
            }

            for (int i = 0; i < numLibraries; i++) {
                if (working[i]) {
                    int maxNeedReps = Math.min(librarySize[i]-resultSize[i], parallelBooks[i]);
                    parallel: for (int r = 0; r < maxNeedReps; r++) {
                        if (resultSize[i] == librarySize[i]) {
                            working[i] = false;
                            finished[i] = true;
                            break parallel;
                        }
                        int nextBook = lastBook[i];
                        books: for (int j = 1; j < librarySize[i] - resultSize[i] && nextBook < librarySize[i]; j++) {
                            if (!usedBooks[books[i][nextBook]]) {
                                resultBooks[i][resultSize[i]] = books[i][nextBook];
                                resultSize[i]++;
                                lastBook[i] = nextBook+1;
                                usedBooks[books[i][nextBook]] = true;
                                score += bookValues[books[i][nextBook]];
                                if (DEBUG)
                                    System.out.println("library " + i + " sends book: " + books[i][nextBook] + "(" + bookValues[books[i][nextBook]] + ")");
                                break books;
                            } else {
                                nextBook = (nextBook + 1) % librarySize[i];
                            }
                        }
                    }
                }
            }
            if (DEBUG)
                System.out.println("Ends day " + day + " with: " + score + "\n--------");
        }
        return new Result(lastLibrary-1,score,resultSize,librariesOrder,resultBooks);
    }

    private static void orderByValue(int[][] elems, int[] values) {
        for (int[] elem : elems) orderByValue(elem, values);
    }

    private static void orderByValue(int[] elems, int[] values)
    {
        long[] valuedelems = new long[elems.length];
        for (int i = 0; i < elems.length; i++) {
            valuedelems[i] = (((long) values[elems[i]]) << 32) | (elems[i] & 0xffffffffL);
        }
        Arrays.sort(valuedelems);
        for (int i = 0; i < elems.length; i++) {
            elems[i] = (int) valuedelems[i];
        }
    }

    private static String arrayToNString(int[] value, int n)
    {
        if(0==n) return "";
        StringBuilder sb = new StringBuilder("");
        for(int i = 0; i < n;i++)
        {
            if(value[i] >= 0) sb.append(value[i]).append(' ');
        }
        String res = sb.toString();
        return res.substring(0, res.length() - 1);
    }

    private static String arrayToString(int[] value)
    {
        StringBuilder sb = new StringBuilder("");
        for(int i = 0; i < value.length;i++)
        {
            if(value[i] >= 0) sb.append(value[i]).append(' ');
        }
        String res = sb.toString();
        return res.substring(0, res.length() - 1);
    }

    private static class Result {
        int lastlibrary;
        int score;
        int[] resultSize;
        int[] librariesOrder;
        int[][] resultBooks;

        public Result(int lastlibrary, int score, int[] resultSize, int[] librariesOrder, int[][] resultBooks) {
            this.lastlibrary = lastlibrary;
            this.score = score;
            this.resultSize = resultSize;
            this.librariesOrder = librariesOrder;
            this.resultBooks = resultBooks;
        }

        public int getScore() {
            return score;
        }

        public int[] getLibrariesOrder() {
            return librariesOrder;
        }

        public int[][] getResultBooks() {
            return resultBooks;
        }

        public int[] getResultSize() {
            return resultSize;
        }

        public int getLastlibrary() {
            return lastlibrary;
        }
    }
}
