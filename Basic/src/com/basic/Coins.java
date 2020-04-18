package com.basic;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

class Coins {

    enum StopMode{
        BY_TIME,
        BY_REPS,
    }

    static final long TIME_CAP = 10000000000L;
    static final int REPS = 1000;
    static final StopMode stopMode= StopMode.BY_TIME;

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        int randomTries = 0;
        List<Integer> list = new ArrayList<Integer>();
        //File file = new File("c:\\tmp\\d_quite_big.in");
        File file = new File("c:\\tmp\\e_also_big.in");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Pattern p = Pattern.compile("[\\s]+");
            String[] ln1 = reader.readLine().split(p.pattern());
            String[] ln2 = reader.readLine().split(p.pattern());
            long target = Long.parseLong(ln1[0]);
            long _numPizzas = Integer.parseInt(ln1[1]);
            int numPizzas = ln2.length;
            if(_numPizzas != numPizzas) System.out.println("!!! Bad file format (second parameter ignored)\n");
            long[] pizzas = Arrays.stream(ln2).mapToLong(Long::parseLong).toArray();

            Score winner = bestFit(target, pizzas);
            Score candidate = worstFit(target, pizzas);
            if (winner.getScore() < candidate.getScore()) winner = candidate;

            switch (stopMode)
            {
                case BY_TIME:
                    do {
                        candidate = randomFit(target, pizzas);
                        if (winner.getScore() < candidate.getScore()) winner = candidate;
                        randomTries++;
                        if (winner.getScore() == target) break;
                    } while (System.nanoTime() - startTime < TIME_CAP);
                    break;
                case BY_REPS:
                    for (int i = 0; i < REPS; i++) {
                        candidate = randomFit(target, pizzas);
                        if (winner.getScore() < candidate.getScore()) winner = candidate;
                        randomTries++;
                        if (winner.getScore() == target) break;
                    }
                    break;
            }

            List<Boolean> used = winner.getUsed();
            StringBuilder sbIndex = new StringBuilder("");
            StringBuilder sbValue = new StringBuilder("");
            int count = 0;
            boolean notFirst = false;
            for (int i = 0; i < numPizzas; i++) {
                if (used.get(i)) {
                    if (notFirst) {
                        sbValue.append(' ');
                        sbIndex.append(' ');
                    }
                    count++;
                    sbValue.append(pizzas[i]);
                    sbIndex.append(i);
                    notFirst = true;
                }
            }
            System.out.println(winner.getName() + ":" + winner.getScore() + "(" + (target - winner.getScore()) +")");
            System.out.println(count);
            System.out.println(sbIndex.toString());
            System.out.println(sbValue.toString());
            System.out.println("Ended in: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds.");
            System.out.println("          " + randomTries + " randomFit tries.");
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    private static Score bestFit(long target, long[] pizzas)
    {
        int numPizzas = pizzas.length;
        boolean[] used = new boolean[numPizzas];
        Arrays.fill(used, false);
        long current = target;
        for (int i = numPizzas - 1; i >= 0; i--) {
            long pizza;
            if (!used[i] && ((pizza = pizzas[i]) <= current)) {
                current -= pizza;
                used[i] = true;
            }
            if (0 == current) break;
        }
        return new Score("bestFit",target-current,toArrayList(used));
    }

    private static Score worstFit(long target, long[] pizzas)
    {
        int numPizzas = pizzas.length;
        boolean[] used = new boolean[numPizzas];
        Arrays.fill(used, false);
        long current = target;
        for (int i = 0; i < numPizzas; i++) {
            long pizza;
            if (!used[i] && ((pizza = pizzas[i]) <= current)) {
                current -= pizza;
                used[i] = true;
            }
            if (0 == current) break;
        }
        return new Score("worstFit",target-current,toArrayList(used));
    }

    private static Score randomFit(long target, long[] pizzas)
    {
        int numPizzas = pizzas.length;
        boolean[] used = new boolean[numPizzas];
        Arrays.fill(used, false);
        long current = target;
        boolean morePizzas = false;
        do {
            long pizza;
            int i = new Random().nextInt(numPizzas);
            if (!used[i] && ((pizza = pizzas[i]) <= current)) {
                current -= pizza;
                used[i] = true;
            }
            if (0 == current) break;
            morePizzas = false;
            for (int j = 0; j < numPizzas; j++) {
                if (!used[j] && (pizzas[j]) <= current) {
                    morePizzas = true;
                    break;
                }
            }
        } while (morePizzas);
        return new Score("randomFit",target-current,toArrayList(used));
    }

    private static ArrayList<Boolean> toArrayList(boolean[] array) {
        ArrayList<Boolean> result = new ArrayList<>(array.length);
        for (boolean b : array) result.add(b);
        return result;
    }

    private static class Score {

        String name;
        long score;
        List<Boolean> used;

        public Score(String name, long score, List<Boolean> used) {
            this.name = name;
            this.score = score;
            this.used = used;
        }

        public String getName() {
            return name;
        }

        public long getScore() {
            return score;
        }

        public List<Boolean> getUsed() {
            return used;
        }
    }

}