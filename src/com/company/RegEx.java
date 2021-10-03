package com.company;

import java.util.Scanner;
import java.util.ArrayList;

import java.lang.Exception;

public class RegEx {
    //MACROS
    static final int CONCAT = 0xC04CA7;
    static final int STAR = 0xE7011E;
    static final int ALTERNATION = 0xA17E54;
    static final int PROTECTION = 0xBADDAD;

    static final int OPENING_PARENTHESIS = 0x16641664;
    static final int CLOSING_PARENTHESIS = 0x51515151;
    static final int DOT = 0xD07;

    //REGEX
    private static String regEx;

    //CONSTRUCTOR
    public RegEx() {
    }

    //MAIN
    public static void main(String arg[]) {
        System.out.println("Welcome to Bogota, Mr. Thomas Anderson.");
        if (arg.length != 0) {
            regEx = arg[0];
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("  >> Please enter a regEx: ");
            regEx = scanner.next();
        }
        System.out.println("  >> Parsing regEx \"" + regEx + "\".");
        System.out.println("  >> ...");

        if (regEx.length() < 1) {
            System.err.println("  >> ERROR: empty regEx.");
        } else {
            System.out.print("  >> ASCII codes: [" + (int) regEx.charAt(0));
            for (int i = 1; i < regEx.length(); i++) System.out.print("," + (int) regEx.charAt(i));
            System.out.println("].");
            RegExTree ret = null;
            try {
                ret = parse();
                System.out.println("  >> Tree result: " + ret.toString() + ".");
            } catch (Exception e) {
                System.err.println("  >> ERROR: syntax error for regEx \"" + regEx + "\".");
            }
            NDFAutomaton ndfa = step2_AhoUllman(ret);
            System.out.println("  >> NDFA construction:\n\nBEGIN NDFA\n" + ndfa.toString() + "END NDFA.\n");
        }

        System.out.println("  >> ...");
        System.out.println("  >> Parsing completed.");
        System.out.println("Goodbye Mr. Anderson.");
    }

    //FROM REGEX TO SYNTAX TREE
    private static RegExTree parse() throws Exception {
        //BEGIN DEBUG: set conditionnal to true for debug example
        if (false) throw new Exception();
        RegExTree example = exampleAhoUllman();
        if (false) return example;
        //END DEBUG

        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        for (int i = 0; i < regEx.length(); i++)
            result.add(new RegExTree(charToRoot(regEx.charAt(i)), new ArrayList<RegExTree>()));

        return parse(result);
    }

    private static int charToRoot(char c) {
        if (c == '.') return DOT;
        if (c == '*') return STAR;
        if (c == '|') return ALTERNATION;
        if (c == '(') return OPENING_PARENTHESIS;
        if (c == ')') return CLOSING_PARENTHESIS;
        return (int) c;
    }

    private static RegExTree parse(ArrayList<RegExTree> result) throws Exception {
        while (containParenthese(result)) result = processParenthese(result);
        while (containStar(result)) result = processStar(result);
        while (containConcat(result)) result = processConcat(result);
        while (containAlternation(result)) result = processAlternation(result);

        if (result.size() > 1) throw new Exception();

        return removeProtection(result.get(0));
    }

    private static boolean containParenthese(ArrayList<RegExTree> trees) {
        for (RegExTree t : trees) if (t.root == CLOSING_PARENTHESIS || t.root == OPENING_PARENTHESIS) return true;
        return false;
    }

    private static ArrayList<RegExTree> processParenthese(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        for (RegExTree t : trees) {
            if (!found && t.root == CLOSING_PARENTHESIS) {
                boolean done = false;
                ArrayList<RegExTree> content = new ArrayList<RegExTree>();
                while (!done && !result.isEmpty())
                    if (result.get(result.size() - 1).root == OPENING_PARENTHESIS) {
                        done = true;
                        result.remove(result.size() - 1);
                    } else content.add(0, result.remove(result.size() - 1));
                if (!done) throw new Exception();
                found = true;
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(parse(content));
                result.add(new RegExTree(PROTECTION, subTrees));
            } else {
                result.add(t);
            }
        }
        if (!found) throw new Exception();
        return result;
    }

    private static boolean containStar(ArrayList<RegExTree> trees) {
        for (RegExTree t : trees) if (t.root == STAR && t.subTrees.isEmpty()) return true;
        return false;
    }

    private static ArrayList<RegExTree> processStar(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        for (RegExTree t : trees) {
            if (!found && t.root == STAR && t.subTrees.isEmpty()) {
                if (result.isEmpty()) throw new Exception();
                found = true;
                RegExTree last = result.remove(result.size() - 1);
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(last);
                result.add(new RegExTree(STAR, subTrees));
            } else {
                result.add(t);
            }
        }
        return result;
    }

    private static boolean containConcat(ArrayList<RegExTree> trees) {
        boolean firstFound = false;
        for (RegExTree t : trees) {
            if (!firstFound && t.root != ALTERNATION) {
                firstFound = true;
                continue;
            }
            if (firstFound) if (t.root != ALTERNATION) return true;
            else firstFound = false;
        }
        return false;
    }

    private static ArrayList<RegExTree> processConcat(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        boolean firstFound = false;
        for (RegExTree t : trees) {
            if (!found && !firstFound && t.root != ALTERNATION) {
                firstFound = true;
                result.add(t);
                continue;
            }
            if (!found && firstFound && t.root == ALTERNATION) {
                firstFound = false;
                result.add(t);
                continue;
            }
            if (!found && firstFound && t.root != ALTERNATION) {
                found = true;
                RegExTree last = result.remove(result.size() - 1);
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(last);
                subTrees.add(t);
                result.add(new RegExTree(CONCAT, subTrees));
            } else {
                result.add(t);
            }
        }
        return result;
    }

    private static boolean containAlternation(ArrayList<RegExTree> trees) {
        for (RegExTree t : trees) if (t.root == ALTERNATION && t.subTrees.isEmpty()) return true;
        return false;
    }

    private static ArrayList<RegExTree> processAlternation(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        RegExTree left = null;
        boolean done = false;
        for (RegExTree t : trees) {
            if (!found && t.root == ALTERNATION && t.subTrees.isEmpty()) {
                if (result.isEmpty()) throw new Exception();
                found = true;
                left = result.remove(result.size() - 1);
                continue;
            }
            if (found && !done) {
                if (left == null) throw new Exception();
                done = true;
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(left);
                subTrees.add(t);
                result.add(new RegExTree(ALTERNATION, subTrees));
            } else {
                result.add(t);
            }
        }
        return result;
    }

    private static RegExTree removeProtection(RegExTree tree) throws Exception {
        if (tree.root == PROTECTION && tree.subTrees.size() != 1) throw new Exception();
        if (tree.subTrees.isEmpty()) return tree;
        if (tree.root == PROTECTION) return removeProtection(tree.subTrees.get(0));

        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        for (RegExTree t : tree.subTrees) subTrees.add(removeProtection(t));
        return new RegExTree(tree.root, subTrees);
    }

    //EXAMPLE
    // --> RegEx from Aho-Ullman book Chap.10 Example 10.25
    private static RegExTree exampleAhoUllman() {
        RegExTree a = new RegExTree((int) 'a', new ArrayList<RegExTree>());
        RegExTree b = new RegExTree((int) 'b', new ArrayList<RegExTree>());
        RegExTree c = new RegExTree((int) 'c', new ArrayList<RegExTree>());
        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        subTrees.add(c);
        RegExTree cStar = new RegExTree(STAR, subTrees);
        subTrees = new ArrayList<RegExTree>();
        subTrees.add(b);
        subTrees.add(cStar);
        RegExTree dotBCStar = new RegExTree(CONCAT, subTrees);
        subTrees = new ArrayList<RegExTree>();
        subTrees.add(a);
        subTrees.add(dotBCStar);
        return new RegExTree(ALTERNATION, subTrees);
    }

    private static NDFAutomaton step2_AhoUllman(RegExTree ret) {

        if (ret.subTrees.isEmpty()) {
            //IMPLICIT REPRESENTATION HERE: INIT STATE IS ALWAYS 0; FINAL STATE IS ALWAYS transitionTable.length-1
            int[][] tTab = new int[2][256];
            ArrayList<Integer>[] eTab = new ArrayList[2];

            //DUMMY VALUES FOR INITIALIZATION
            for (int i = 0; i < tTab.length; i++) for (int col = 0; col < 256; col++) tTab[i][col] = -1;
            for (int i = 0; i < eTab.length; i++) eTab[i] = new ArrayList<Integer>();

            if (ret.root != DOT) tTab[0][ret.root] = 1; //transition ret.root from initial state "0" to final state "1"
            else
                for (int i = 0; i < 256; i++) tTab[0][i] = 1; //transition DOT from initial state "0" to final state "1"

            return new NDFAutomaton(tTab, eTab);
        }

        if (ret.root == CONCAT) {
            //IMPLICIT REPRESENTATION HERE: INIT STATE IS ALWAYS 0; FINAL STATE IS ALWAYS transitionTable.length-1
            NDFAutomaton left = step2_AhoUllman(ret.subTrees.get(0));
            int[][] tTab_g = left.transitionTable;
            ArrayList<Integer>[] eTab_g = left.epsilonTransitionTable;
            NDFAutomaton right = step2_AhoUllman(ret.subTrees.get(1));
            int[][] tTab_d = right.transitionTable;
            ArrayList<Integer>[] eTab_d = right.epsilonTransitionTable;
            int lg = tTab_g.length;
            int ld = tTab_d.length;
            int[][] tTab = new int[lg + ld][256];
            ArrayList<Integer>[] eTab = new ArrayList[lg + ld];

            //DUMMY VALUES FOR INITIALIZATION
            for (int i = 0; i < tTab.length; i++) for (int col = 0; col < 256; col++) tTab[i][col] = -1;
            for (int i = 0; i < eTab.length; i++) eTab[i] = new ArrayList<Integer>();

            eTab[lg - 1].add(lg); //epsilon transition from old final state "left" to old initial state "right"

            for (int i = 0; i < lg; i++)
                for (int col = 0; col < 256; col++) tTab[i][col] = tTab_g[i][col]; //copy old transitions
            for (int i = 0; i < lg; i++) eTab[i].addAll(eTab_g[i]); //copy old transitions
            for (int i = lg; i < lg + ld - 1; i++)
                for (int col = 0; col < 256; col++)
                    if (tTab_d[i - lg][col] != -1) tTab[i][col] = tTab_d[i - lg][col] + lg; //copy old transitions
            for (int i = lg; i < lg + ld - 1; i++)
                for (int s : eTab_d[i - lg]) eTab[i].add(s + lg); //copy old transitions

            return new NDFAutomaton(tTab, eTab);
        }

        if (ret.root == ALTERNATION) {
            //IMPLICIT REPRESENTATION HERE: INIT STATE IS ALWAYS 0; FINAL STATE IS ALWAYS transitionTable.length-1
            NDFAutomaton left = step2_AhoUllman(ret.subTrees.get(0));
            int[][] tTab_g = left.transitionTable;
            ArrayList<Integer>[] eTab_g = left.epsilonTransitionTable;
            NDFAutomaton right = step2_AhoUllman(ret.subTrees.get(1));
            int[][] tTab_d = right.transitionTable;
            ArrayList<Integer>[] eTab_d = right.epsilonTransitionTable;
            int lg = tTab_g.length;
            int ld = tTab_d.length;
            int[][] tTab = new int[2 + lg + ld][256];
            ArrayList<Integer>[] eTab = new ArrayList[2 + lg + ld];

            //DUMMY VALUES FOR INITIALIZATION
            for (int i = 0; i < tTab.length; i++) for (int col = 0; col < 256; col++) tTab[i][col] = -1;
            for (int i = 0; i < eTab.length; i++) eTab[i] = new ArrayList<Integer>();

            eTab[0].add(1); //epsilon transition from new initial state to old initial state
            eTab[0].add(1 + lg); //epsilon transition from new initial state to old initial state
            eTab[1 + lg - 1].add(2 + lg + ld - 1); //epsilon transition from old final state to new final state
            eTab[1 + lg + ld - 1].add(2 + lg + ld - 1); //epsilon transition from old final state to new final state

            for (int i = 1; i < 1 + lg; i++)
                for (int col = 0; col < 256; col++)
                    if (tTab_g[i - 1][col] != -1) tTab[i][col] = tTab_g[i - 1][col] + 1; //copy old transitions
            for (int i = 1; i < 1 + lg; i++) for (int s : eTab_g[i - 1]) eTab[i].add(s + 1); //copy old transitions
            for (int i = 1 + lg; i < 1 + lg + ld - 1; i++)
                for (int col = 0; col < 256; col++)
                    if (tTab_d[i - 1 - lg][col] != -1)
                        tTab[i][col] = tTab_d[i - 1 - lg][col] + 1 + lg; //copy old transitions
            for (int i = 1 + lg; i < 1 + lg + ld; i++)
                for (int s : eTab_d[i - 1 - lg]) eTab[i].add(s + 1 + lg); //copy old transitions

            return new NDFAutomaton(tTab, eTab);
        }

        if (ret.root == STAR) {
            //IMPLICIT REPRESENTATION HERE: INIT STATE IS ALWAYS 0; FINAL STATE IS ALWAYS transitionTable.length-1
            NDFAutomaton fils = step2_AhoUllman(ret.subTrees.get(0));
            int[][] tTab_fils = fils.transitionTable;
            ArrayList<Integer>[] eTab_fils = fils.epsilonTransitionTable;
            int l = tTab_fils.length;
            int[][] tTab = new int[2 + l][256];
            ArrayList<Integer>[] eTab = new ArrayList[2 + l];

            //DUMMY VALUES FOR INITIALIZATION
            for (int i = 0; i < tTab.length; i++) for (int col = 0; col < 256; col++) tTab[i][col] = -1;
            for (int i = 0; i < eTab.length; i++) eTab[i] = new ArrayList<Integer>();

            eTab[0].add(1); //epsilon transition from new initial state to old initial state
            eTab[0].add(2 + l - 1); //epsilon transition from new initial state to new final state
            eTab[2 + l - 2].add(2 + l - 1); //epsilon transition from old final state to new final state
            eTab[2 + l - 2].add(1); //epsilon transition from old final state to old initial state

            for (int i = 1; i < 2 + l - 1; i++)
                for (int col = 0; col < 256; col++)
                    if (tTab_fils[i - 1][col] != -1) tTab[i][col] = tTab_fils[i - 1][col] + 1; //copy old transitions
            for (int i = 1; i < 2 + l - 1; i++)
                for (int s : eTab_fils[i - 1]) eTab[i].add(s + 1); //copy old transitions

            return new NDFAutomaton(tTab, eTab);
        }

        return null;
    }
}

//UTILITARY CLASS
class RegExTree {
    protected int root;
    protected ArrayList<RegExTree> subTrees;

    public RegExTree(int root, ArrayList<RegExTree> subTrees) {
        this.root = root;
        this.subTrees = subTrees;
    }

    //FROM TREE TO PARENTHESIS
    public String toString() {
        if (subTrees.isEmpty()) return rootToString();
        String result = rootToString() + "(" + subTrees.get(0).toString();
        for (int i = 1; i < subTrees.size(); i++) result += "," + subTrees.get(i).toString();
        return result + ")";
    }

    private String rootToString() {
        if (root == RegEx.CONCAT) return ".";
        if (root == RegEx.STAR) return "*";
        if (root == RegEx.ALTERNATION) return "|";
        if (root == RegEx.DOT) return ".";
        return Character.toString((char) root);
    }
}

class NDFAutomaton {
    //IMPLICIT REPRESENTATION HERE: INIT STATE IS ALWAYS 0; FINAL STATE IS ALWAYS transitionTable.length-1
    protected int[][] transitionTable; //ASCII transition
    protected ArrayList<Integer>[] epsilonTransitionTable; //epsilon transition list

    public NDFAutomaton(int[][] transitionTable, ArrayList<Integer>[] epsilonTransitionTable) {
        this.transitionTable = transitionTable;
        this.epsilonTransitionTable = epsilonTransitionTable;
    }

    //PRINT THE AUTOMATON TRANSITION TABLE
    public String toString() {
        String result = "Initial state: 0\nFinal state: " + (transitionTable.length - 1) + "\nTransition list:\n";
        for (int i = 0; i < epsilonTransitionTable.length; i++)
            for (int state : epsilonTransitionTable[i])
                result += "  " + i + " -- epsilon --> " + state + "\n";
        for (int i = 0; i < transitionTable.length; i++)
            for (int col = 0; col < 256; col++)
                if (transitionTable[i][col] != -1)
                    result += "  " + i + " -- " + (char) col + " --> " + transitionTable[i][col] + "\n";
        return result;
    }
}
