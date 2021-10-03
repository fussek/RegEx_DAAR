package com.company;

import java.util.Scanner;
import java.util.ArrayList;

import java.lang.Exception;

public class RegEx {
    //MACROS
    static final int CONCAT = 0xC04CA7;
    static final int ETOILE = 0xE7011E;
    static final int ALTERN = 0xA17E54;
    static final int PROTECTION = 0xBADDAD;

    static final int PARENTHESEOUVRANT = 0x16641664;
    static final int PARENTHESEFERMANT = 0x51515151;
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
        if (c == '*') return ETOILE;
        if (c == '|') return ALTERN;
        if (c == '(') return PARENTHESEOUVRANT;
        if (c == ')') return PARENTHESEFERMANT;
        return (int) c;
    }

    private static RegExTree parse(ArrayList<RegExTree> result) throws Exception {
        while (containParenthese(result)) result = processParenthese(result);
        while (containEtoile(result)) result = processEtoile(result);
        while (containConcat(result)) result = processConcat(result);
        while (containAltern(result)) result = processAltern(result);

        if (result.size() > 1) throw new Exception();

        return removeProtection(result.get(0));
    }

    private static boolean containParenthese(ArrayList<RegExTree> trees) {
        for (RegExTree t : trees) if (t.root == PARENTHESEFERMANT || t.root == PARENTHESEOUVRANT) return true;
        return false;
    }

    private static ArrayList<RegExTree> processParenthese(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        for (RegExTree t : trees) {
            if (!found && t.root == PARENTHESEFERMANT) {
                boolean done = false;
                ArrayList<RegExTree> content = new ArrayList<RegExTree>();
                while (!done && !result.isEmpty())
                    if (result.get(result.size() - 1).root == PARENTHESEOUVRANT) {
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

    private static boolean containEtoile(ArrayList<RegExTree> trees) {
        for (RegExTree t : trees) if (t.root == ETOILE && t.subTrees.isEmpty()) return true;
        return false;
    }

    private static ArrayList<RegExTree> processEtoile(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        for (RegExTree t : trees) {
            if (!found && t.root == ETOILE && t.subTrees.isEmpty()) {
                if (result.isEmpty()) throw new Exception();
                found = true;
                RegExTree last = result.remove(result.size() - 1);
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(last);
                result.add(new RegExTree(ETOILE, subTrees));
            } else {
                result.add(t);
            }
        }
        return result;
    }

    private static boolean containConcat(ArrayList<RegExTree> trees) {
        boolean firstFound = false;
        for (RegExTree t : trees) {
            if (!firstFound && t.root != ALTERN) {
                firstFound = true;
                continue;
            }
            if (firstFound) if (t.root != ALTERN) return true;
            else firstFound = false;
        }
        return false;
    }

    private static ArrayList<RegExTree> processConcat(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        boolean firstFound = false;
        for (RegExTree t : trees) {
            if (!found && !firstFound && t.root != ALTERN) {
                firstFound = true;
                result.add(t);
                continue;
            }
            if (!found && firstFound && t.root == ALTERN) {
                firstFound = false;
                result.add(t);
                continue;
            }
            if (!found && firstFound && t.root != ALTERN) {
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

    private static boolean containAltern(ArrayList<RegExTree> trees) {
        for (RegExTree t : trees) if (t.root == ALTERN && t.subTrees.isEmpty()) return true;
        return false;
    }

    private static ArrayList<RegExTree> processAltern(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        RegExTree gauche = null;
        boolean done = false;
        for (RegExTree t : trees) {
            if (!found && t.root == ALTERN && t.subTrees.isEmpty()) {
                if (result.isEmpty()) throw new Exception();
                found = true;
                gauche = result.remove(result.size() - 1);
                continue;
            }
            if (found && !done) {
                if (gauche == null) throw new Exception();
                done = true;
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(gauche);
                subTrees.add(t);
                result.add(new RegExTree(ALTERN, subTrees));
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
        RegExTree cEtoile = new RegExTree(ETOILE, subTrees);
        subTrees = new ArrayList<RegExTree>();
        subTrees.add(b);
        subTrees.add(cEtoile);
        RegExTree dotBCEtoile = new RegExTree(CONCAT, subTrees);
        subTrees = new ArrayList<RegExTree>();
        subTrees.add(a);
        subTrees.add(dotBCEtoile);
        return new RegExTree(ALTERN, subTrees);
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
            NDFAutomaton gauche = step2_AhoUllman(ret.subTrees.get(0));
            int[][] tTab_g = gauche.transitionTable;
            ArrayList<Integer>[] eTab_g = gauche.epsilonTransitionTable;
            NDFAutomaton droite = step2_AhoUllman(ret.subTrees.get(1));
            int[][] tTab_d = droite.transitionTable;
            ArrayList<Integer>[] eTab_d = droite.epsilonTransitionTable;
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

        if (ret.root == ALTERN) {
            //IMPLICIT REPRESENTATION HERE: INIT STATE IS ALWAYS 0; FINAL STATE IS ALWAYS transitionTable.length-1
            NDFAutomaton gauche = step2_AhoUllman(ret.subTrees.get(0));
            int[][] tTab_g = gauche.transitionTable;
            ArrayList<Integer>[] eTab_g = gauche.epsilonTransitionTable;
            NDFAutomaton droite = step2_AhoUllman(ret.subTrees.get(1));
            int[][] tTab_d = droite.transitionTable;
            ArrayList<Integer>[] eTab_d = droite.epsilonTransitionTable;
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

        if (ret.root == ETOILE) {
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
        if (root == RegEx.ETOILE) return "*";
        if (root == RegEx.ALTERN) return "|";
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
