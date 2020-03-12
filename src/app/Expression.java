package app;

import java.io.*;
import java.util.*;

//import structures.Stack;

public class Expression {
	public static String delims = " \t*+-/()[]";
    /**
     * Populates the vars list with simple variables, and arrays lists with arrays
     * in the expression. For every variable (simple or array), a SINGLE instance is created
     * and stored, even if it appears more than once in the expression.
     * At this time, values for all variables and all array items are set to
     * zero - they will be loaded from a file in the loadVariableValues method.
     *
     * @param expr The expression
     * @param vars The variables array list - already created by the caller
     * @param arrays The arrays array list - already created by the caller
     */
    public static void makeVariableLists(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
        expr = expr.replaceAll(" ","");
        String expression = expr;
        StringTokenizer stringTokenizer = new StringTokenizer(expr, delims);
        int index;
        while (stringTokenizer.hasMoreTokens()){
            String token = stringTokenizer.nextToken();
            index = expression.indexOf(token);
            int value = token.charAt(0);
            int size = token.length();

            if((value>64 && value<91) || (value>96 && value<123)){
                if(!arrays.contains(new Array(token)) && !vars.contains(new Variable(token))){
                    int nextChar = index+size;
                    if(nextChar<expression.length()) {
                        if (expression.charAt(nextChar) == '[') {
                            arrays.add(new Array(token));
                        }else {
                            vars.add(new Variable(token));
                        }
                    }else {
                        vars.add(new Variable(token));
                    }
                }
            }

            expression = expression.substring(index+size);
        }
    }

    /**
     * Loads values for variables and arrays in the expression
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input
     * @param vars The variables array list, previously populated by makeVariableLists
     * @param arrays The arrays array list - previously populated by makeVariableLists
     */
    public static void loadVariableValues(Scanner sc, ArrayList<Variable> vars, ArrayList<Array> arrays)
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String tok = st.nextToken();
            Variable var = new Variable(tok);
            Array arr = new Array(tok);
            int vari = vars.indexOf(var);
            int arri = arrays.indexOf(arr);
            if (vari == -1 && arri == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                vars.get(vari).value = num;
            } else { // array symbol
            	arr = arrays.get(arri);
            	arr.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    arr.values[index] = val;
                }
            }
        }
    }

    /**
     * Evaluates the expression.
     *
     * @param vars The variables array list, with values for all variables in the expression
     * @param arrays The arrays array list, with values for all array items
     * @return Result of evaluation
     */
    public static float evaluate(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
        String expression = expr.replaceAll(" ","");
        while(expression.contains("]")){
            expression = shrinkArrays(expression,vars,arrays);
        }

        while(expression.contains(")")){
            expression = shrinkParentheses(expression,vars);
        }
        return solve(expression,vars);
    }

    private static String shrinkParentheses(String exp, ArrayList<Variable> vars) {
        String expression = exp;
        StringTokenizer stringTokenizer = new StringTokenizer(expression, "()",true);
        int index;
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            index = expression.indexOf(token);
            if(token.equals("(")){
                expression = expression.substring(index+1);
            }
            else if(token.equals(")")){
                String segment = expression.substring(0,expression.indexOf(")"));
                float ans = solve(segment, vars);
                String e = exp.substring(0, exp.indexOf(")") - segment.length() - 1);
                e +=  + ans + exp.substring(exp.indexOf(")") + 1);
                return e;
            }
        }
        return "";
    }

    private static String shrinkArrays(String exp, ArrayList<Variable> vars, ArrayList<Array> arrays) {
        String expression = exp;
        StringTokenizer stringTokenizer = new StringTokenizer(expression, "[]",true);
        int index ;
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            index = expression.indexOf(token);
            if(token.equals("[")){
                expression = expression.substring(index+1);
            }
            else if(token.equals("]")){
                String segment = expression.substring(0,expression.indexOf("]"));
                while(segment.contains(")")){
                    segment = shrinkParentheses(segment,vars);
                }
                float ans = solve(segment, vars);
                String beginning = exp.substring(0 , exp.indexOf("]")-segment.length()-1);
                String e = replaceArray(beginning, (int) ans,arrays);
                e += exp.substring(exp.indexOf("]")+1);
                return e;
            }
        }
        return "";
    }

    private static String replaceArray(String expression,int value, ArrayList<Array> arrays){
        StringTokenizer stringTokenizer = new StringTokenizer(expression, delims);
        int index,size =0;
        Array array = new Array("");
        StringBuilder text= new StringBuilder();
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            index = expression.indexOf(token);
            if(arrays.contains(new Array(token))){
                size = token.length();
                array = new Array(token);
                text.append(expression, 0, index + size);
                expression = expression.substring(index+size);
            }
        }
        text = new StringBuilder(text.substring(0, text.length() - size));
        int a = 0;
        if(arrays.size()!=0) {
            if (arrays.size() > arrays.indexOf(array)) {
                if(arrays.get(arrays.indexOf(array)).values.length>value)
                a = arrays.get(arrays.indexOf(array)).values[value];
            }
        }
        return text.toString() +a;
    }

    private static float solve (String operation, ArrayList<Variable> vars){
        String expression = operation.replaceAll(" ","");
        StringTokenizer stringTokenizer = new StringTokenizer(operation, delims);
        int index;
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            index = expression.indexOf(token);
            int size = token.length();
            if(vars.contains(new Variable(token))){
                expression = expression.substring(0,index) + vars.get(vars.indexOf(new Variable(token))).value + expression.substring(index + size);
            }
        }

        ArrayList<Float> numbers = new ArrayList<>();
        ArrayList<String> operators = new ArrayList<>();
        ArrayList<Integer> removableOperators = new ArrayList<>();

        StringTokenizer tokenizer = new StringTokenizer(expression, "+"+"-"+"*"+"/" ,true);
        boolean minus = true;
        while(tokenizer.hasMoreTokens()){
            String token = tokenizer.nextToken();
            if(minus && token.equals("-")){
                token = tokenizer.nextToken();
                numbers.add(Float.parseFloat("-"+token));
                minus = false;
            }else if(token.equals("+")|| token.equals("-") || token.equals("*")|| token.equals("/")) {
                operators.add(token);
                minus = true;
            }
            else {
                numbers.add(Float.parseFloat(token));
                minus = false;
            }
        }
        for(int x = 0; x<operators.size(); x++){
            if(operators.get(x).equals("*")) {
                float temp = numbers.get(x);
                numbers.set(x+1,temp * numbers.get(x + 1));
                removableOperators.add(x);
            }
            else if(operators.get(x).equals("/")) {
                float temp = numbers.get(x);
                float ans = (temp / numbers.get(x + 1));
                numbers.set(x+1, ans);
                removableOperators.add(x);
            }
        }
        for(int x = removableOperators.size()-1; x>=0; x--){
            int temp = removableOperators.get(x);
            operators.remove(temp);
            numbers.remove(temp);
        }
        for(int x = 0; x<operators.size(); x++){
            if(operators.get(x).equals( "+")) {
                float temp = numbers.get(x);
                numbers.set(x+1,temp + numbers.get(x + 1));
            }
            else if(operators.get(x).equals("-")) {
                float temp = numbers.get(x) - numbers.get(x+1);
                numbers.set(x+1,temp);
            }
        }
        return numbers.get(numbers.size()-1);
    }
}
