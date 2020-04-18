package com.basic;

import com.sun.source.tree.Tree;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class Books {

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
    static final long TIME_CAP = 10 * 1000000000L;
    static final int REPS = 1000;
    static final Books.StopMode stopMode= Books.StopMode.BY_TIME;
    static final Books.Filename filename = Books.Filename.d_tough_choices;
    static int numLibraries;
    static final boolean DEBUG = false;

    static float BOOK_LIBRARY_REPETITION_FACTOR = 1f/3f;

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
            numLibraries = Integer.parseInt(ln[1]);
            int numDays = Integer.parseInt(ln[2]);
            ln = reader.readLine().split(p.pattern());
            int numBooks = ln.length;

            Library[] libraries = new Library[numLibraries];
            LinkedList<Library> libraryList = new LinkedList<>();
            Book[] books = new Book[numBooks];
            LinkedHashSet<Book> bookList = new LinkedHashSet<>();

            //long[][] valuedBooks = new long[numLibraries][];

            if (numBooks != _numBooks) System.out.println("!!! Bad file format (first line first parameter ignored)\n");
            for (int i = 0; i < ln.length; i++) {
                books[i] = new Book(i, Integer.parseInt(ln[i]));
                bookList.add(books[i]);
            }

            for (int i = 0; i < numLibraries; i++) {
                ln = reader.readLine().split(p.pattern());
                int _librarySize = Integer.parseInt(ln[0]);
                libraries[i] = new Library(i,Integer.parseInt(ln[1]),Integer.parseInt(ln[2]),_librarySize);
                libraryList.add(libraries[i]);
                ln = reader.readLine().split(p.pattern());
                int librarySize = ln.length;
                if (librarySize != _librarySize) System.out.println("!!! Bad file format inconsistent size for library " + i + "\n");
                final Library library = libraries[i];
                Arrays.stream(ln).forEach(
                        s ->
                        {
                            int book = Integer.parseInt(s);
                            library.addBook(books[book]);
                        }
                );
            }

            /*for (Library lib : libraries) {
                System.out.println(lib.toString());
            }*/
            /*for (Book book : books) {
                System.out.println(book.toExtendedString());
            }*/

            Result winner = valBookLibBestFit(numDays, libraryList);


            if (DEBUG) System.out.print(winner.toString());
            else {
                BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\tmp\\result_" + filename + ".txt"));
                writer.write(winner.toString());
                writer.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Ended in: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds.");
    }

    private static Result valBookFit(int numDays, List<Library> libraryList) {
        LinkedList<Library> available = new LinkedList<>(libraryList);
        LinkedList<Library> resultLibraries = new LinkedList<>();
        LinkedList<Library> working = new LinkedList<>();
        Library nextLibrary = null;
        int nextLogday = 0;
        int score = 0;

        LinkedList<Library> stopped = new LinkedList<>();
        for (int day = 0; day < numDays; day++) {
            if (0 == day % ((int)(numDays / 100))) System.out.println(day*100/numDays+"%");
            if (DEBUG) System.out.println("STARTING DAY " + day);
            if (nextLogday == day) {
                if (null != nextLibrary) {
                    if (DEBUG) System.out.println("LIBRARY AWAKE " + nextLibrary.getName());
                    resultLibraries.add(nextLibrary);
                    working.add(nextLibrary);
                }
                nextLibrary = available.pollFirst();
                if (null != nextLibrary) {
                    nextLogday = day + nextLibrary.getLogintime();
                    if (DEBUG) System.out.println("LIBRARY NEXT " + nextLibrary.getName() + " @ " + nextLogday);
                }
            }
            stopped.clear();
            for (Library lib : working) {
                for (int rep = 0; rep < lib.getParallel(); rep++) {
                    Book sent = lib.sendBook();
                    if (null == sent) {
                        stopped.add(lib);
                        if (DEBUG) System.out.println("LIBRARY STOP " + lib.getName());
                    } else {
                        if (DEBUG) System.out.println("LIBRARY " + lib.getName() + " SENDS " + sent.getName() + "(" + sent.getValue() +")");
                        score += sent.getValue();
                    }
                }
            }
            working.removeAll(stopped);
            if (DEBUG) System.out.println("------------ " + day);
        }
        return new Result(score, resultLibraries);
    }

    private static Result valBookLibBestFit(int numDays, List<Library> libraryList) {
        LinkedList<Library> available = new LinkedList<>(libraryList);
        LinkedList<Library> resultLibraries = new LinkedList<>();
        LinkedList<Library> working = new LinkedList<>();
        Library nextLibrary = null;
        int nextLogday = 0;
        int score = 0;

        LinkedList<Library> stopped = new LinkedList<>();
        for (int day = 0; day < numDays; day++) {
            if (0 == day % ((int) (numDays / 100))) System.out.println(day * 100 / numDays + "%");
            if (DEBUG) System.out.println("STARTING DAY " + day);
            if (nextLogday == day) {
                if (null != nextLibrary) {
                    if (DEBUG) System.out.println("LIBRARY AWAKE " + nextLibrary.getName());
                    resultLibraries.add(nextLibrary);
                    working.add(nextLibrary);
                }
                //nextLibrary = available.pollFirst();
                nextLibrary = nextLibraryMaxValSized(available, numDays - day, false);
                if (null != nextLibrary) {
                    nextLogday = day + nextLibrary.getLogintime();
                    if (DEBUG) System.out.println("LIBRARY NEXT " + nextLibrary.getName() + " @ " + nextLogday);
                }
            }
            stopped.clear();
            for (Library lib : working) {
                for (int rep = 0; rep < lib.getParallel(); rep++) {
                    Book sent = lib.sendBook();
                    if (null == sent) {
                        stopped.add(lib);
                        if (DEBUG) System.out.println("LIBRARY STOP " + lib.getName());
                    } else {
                        if (DEBUG)
                            System.out.println("LIBRARY " + lib.getName() + " SENDS " + sent.getName() + "(" + sent.getValue() + ")");
                        score += sent.getValue();
                        for (Library affectedLib : sent.getLibraries()) {
                            affectedLib.getBooks().remove(sent);
                        }
                    }
                }
            }
            working.removeAll(stopped);
            if (DEBUG) System.out.println("------------ " + day);
        }
        return new Result(score, resultLibraries);
    }

    private static Library nextLibraryMaxValSized(LinkedList<Library> available,int remaining, boolean trueVal)
    {
        Library best = null;
        int bestScore = 0;
        double fbestScore = 0;
        for(Library lib:available)
        {
            int size = lib.logintime;
            long maxsent = lib.parallel * (remaining-size);
            int i = 0;
            int score = 0;
            double fscore = 0;
            for (Book book : lib.getBooks()) {
                if (i >= maxsent) break;
                else i++;
                if (trueVal) fscore += book.getTrueValue();
                else score += book.getValue();
            }
            if(fscore + score > bestScore + fbestScore)
            {
                best = lib;
                bestScore = score;
                fbestScore = fscore;
            }
        }
        available.remove(best);
        if (DEBUG) {
            assert best != null;
            System.out.println("BEST LIBRARY" + best.getName() + "(" + (long) (bestScore + fbestScore) + ")");
        }
        return best;
    }

    private static class Result {
        int score;
        LinkedList<Library> resultLibraries;

        public Result(int score, LinkedList<Library> resultLibraries) {
            this.score = score;
            this.resultLibraries = resultLibraries;
        }

        public int getScore() {
            return score;
        }

        public LinkedList<Library> getResultLibraries() {
            return resultLibraries;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("");
            sb.append(resultLibraries.size()).append('\n');
            for (Library lib : resultLibraries) {
                int size = lib.getSentBooks().size();
                if (size > 0) {
                    sb.append(lib.getName()).append(' ').append(size).append('\n');
                    for (Book book : lib.getSentBooks()) {
                        sb.append(book.getName()).append(' ');
                    }
                    sb.append('\n');
                }
            }
            return sb.toString();
        }
    }

    static class Book implements Comparable<Book>{

        int name;
        int value;
        float trueValue;

        //long l = (((long)x) << 32) | (y & 0xffffffffL);
        //int x = (int)(l >> 32);
        //int y = (int)l;
        long composedValue;

        List<Library> libraries;

        public Book(int name, int value) {
            this.name = name;
            this.value = value;
            this.composedValue = (((long)value) << 32) | (name & 0xffffffffL);
            this.libraries = new ArrayList<>();
            updateTrueValue();
        }

        public int getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        public List<Library> getLibraries() {
            return libraries;
        }

        public void addLibrary(Library library) {
            updateTrueValue();
            this.libraries.add(library);
        }

        private void updateTrueValue()
        {
            this.trueValue =
                    BOOK_LIBRARY_REPETITION_FACTOR * value +
                            (((1 - BOOK_LIBRARY_REPETITION_FACTOR) * value * (numLibraries - libraries.size())) / numLibraries);
        }

        @Override
        public int compareTo(Book o) {
            //Reverse order
            return Long.compare(o.composedValue,this.composedValue);
        }

        @Override
        public String toString() {
            return name + "(" + value + " -> "+ trueValue + ")";
        }

        public String toExtendedString() {
            StringBuilder sb = new StringBuilder("Book ");
            sb.append(name).append("(").append(value).append(" -> ").append(trueValue).append(")\n Libraries :");
            for (Library lib : this.libraries) {
                sb.append(" ").append(lib.getName());
            }
            return sb.toString();
        }

        public float getTrueValue() {
            return trueValue;
        }
    }

    static class Library {

        int name;
        int logintime;
        int parallel;

        TreeSet<Book> books;
        LinkedList<Book> sentBooks;

        public Library(int name, int logintime, int parallel, int size) {
            this.name = name;
            this.logintime = logintime;
            this.parallel = parallel;
            this.books = new TreeSet<>();
            this.sentBooks = new LinkedList<>();
        }

        public int getName() {
            return name;
        }

        public int getLogintime() {
            return logintime;
        }

        public int getParallel() {
            return parallel;
        }

        public Set<Book> getBooks() {
            return books;
        }

        public void addBook(Book book) {
            this.books.add(book);
            book.addLibrary(this);
        }

        public Book sendBook() {
            Book result = this.books.pollFirst();
            if (null != result) {
                this.sentBooks.add(result);
            }
            return result;
        }

        public LinkedList<Book> getSentBooks() {
            return sentBooks;
        }

        @Override
        public String toString() {
            return "Library " + name + "time:" + logintime + " parallel:" + parallel + "\n\tBooks:" + books.toString();
        }
    }

    /*static class SendEffect
    {
        Book sent;
        Set<Library> libraries;

        public SendEffect() {
            this.sent = null;
            libraries = new HashSet<>();
        }


        public Book getSent() {
            return sent;
        }

        public void setSent(Book sent) {
            this.sent = sent;
        }

        public Set<Library> getLibraries() {
            return libraries;
        }
    }*/


}
