package se.lth.cs.tycho.phase;

import javafx.util.Pair;
import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import org.multij.MultiJ;
import se.lth.cs.tycho.compiler.CompilationTask;
import se.lth.cs.tycho.compiler.Context;
import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.Variable;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.expr.*;
import se.lth.cs.tycho.ir.stmt.*;
import se.lth.cs.tycho.ir.stmt.lvalue.LValue;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;
import se.lth.cs.tycho.ir.stmt.ssa.ExprPhi;
import se.lth.cs.tycho.ir.stmt.ssa.StmtLabeled;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.reporting.CompilationException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static se.lth.cs.tycho.ir.Variable.variable;

public class SsaPhase implements Phase {

    private static CollectStatements collector = null;
    private static CollectExpressions exprCollector = null;

    public SsaPhase() {
        collector = MultiJ.from(CollectStatements.class).instance();
        exprCollector = MultiJ.from(CollectExpressions.class).instance();
    }

    @Override
    public String getDescription() {
        return "Applies SsaPhase transformation to ExprProcReturn";
    }


    @Override
    public CompilationTask execute(CompilationTask task, Context context) throws CompilationException {
        CollectStatements collector = MultiJ.from(CollectStatements.class).instance();
        CollectExpressions exprCollector = MultiJ.from(CollectExpressions.class).instance();

        Transformation transformation = MultiJ.from(SsaPhase.Transformation.class)
                .bind("collector").to(collector)
                .bind("exprCollector").to(exprCollector)
                .instance();

        return task.transformChildren(transformation);
    }

    @Module
    interface CollectStatements {

        List<Expression> collect(Statement s);

        default List<Expression> collect(StmtCall call) {
            return new LinkedList<>(call.getArgs());
        }

        default List<Expression> collect(StmtIf iff) {
            return new LinkedList<>(Collections.singletonList(iff.getCondition()));
        }

        default List<Expression> collect(StmtCase casee) {
            return new LinkedList<>(Collections.singletonList(casee.getScrutinee()));
        }

        default List<Expression> collect(StmtAssignment assignment) {
            return new LinkedList<>(Collections.singletonList(assignment.getExpression()));
        }

        default List<Expression> collect(StmtForeach forEach) {
            return new LinkedList<>(forEach.getFilters());
        }

        default List<Expression> collect(StmtReturn ret) {
            return new LinkedList<>(Collections.singletonList(ret.getExpression()));
        }

        default List<Expression> collect(StmtWhile whilee) {
            return new LinkedList<>(Collections.singletonList(whilee.getCondition()));
        }
        //TODO localvardecl in stmtblock

    }

    @Module
    interface CollectExpressions {
        List<Expression> collectInternalExpr(Expression e);

        default List<Expression> collectInternalExpr(ExprApplication app) {
            return new LinkedList<>(app.getArgs());
        }

        default List<Expression> collectInternalExpr(ExprBinaryOp bin) {
            return new LinkedList<>(bin.getOperands());
        }

        default List<Expression> collectInternalExpr(ExprCase casee) {
            return new LinkedList<>(Collections.singletonList(casee.getScrutinee()));
        }

        default List<Expression> collectInternalExpr(ExprCase.Alternative alt) {
            return new LinkedList<>(Collections.singletonList(alt.getExpression()));
        }

        default List<Expression> collectInternalExpr(ExprComprehension comp) {
            return new LinkedList<>(comp.getFilters());
        }

        default List<Expression> collectInternalExpr(ExprDeref deref) {
            return new LinkedList<>(Collections.singletonList(deref.getReference()));
        }

        default List<Expression> collectInternalExpr(ExprIf eif) {
            return new LinkedList<>(Arrays.asList(eif.getCondition(), eif.getElseExpr(), eif.getThenExpr()));
        }

        default List<Expression> collectInternalExpr(ExprLambda lambda) {
            return new LinkedList<>(Collections.singletonList(lambda.getBody()));
        }

        default List<Expression> collectInternalExpr(ExprList list) {
            return new LinkedList<>(list.getElements());
        }

        default List<Expression> collectInternalExpr(ExprSet set) {
            return new LinkedList<>(set.getElements());
        }

        default List<Expression> collectInternalExpr(ExprTuple tuple) {
            return new LinkedList<>(tuple.getElements());
        }

        default List<Expression> collectInternalExpr(ExprTypeConstruction typeConstr) {
            return new LinkedList<>(typeConstr.getArgs());
        }

        default List<Expression> collectInternalExpr(ExprUnaryOp unary) {
            return new LinkedList<>(Collections.singletonList(unary.getOperand()));
        }

        default List<Expression> collectInternalExpr(ExprVariable var) {
            return new LinkedList<>();
        }

        default List<Expression> collectInternalExpr(ExprField field) {
            return new LinkedList<>(Collections.singletonList(field.getStructure()));
        }

        default List<Expression> collectInternalExpr(ExprNth nth) {
            return new LinkedList<>(Collections.singletonList(nth.getStructure()));
        }

        default List<Expression> collectInternalExpr(ExprTypeAssertion typeAssert) {
            return new LinkedList<>(Collections.singletonList(typeAssert.getExpression()));
        }

        //TODO check
        default List<Expression> collectInternalExpr(ExprIndexer indexer) {
            //TODO
            return new LinkedList<>(Arrays.asList(indexer.getStructure(), indexer.getStructure()));
        }

        default List<Expression> collectInternalExpr(ExprLet let) {
            //TODO
            return new LinkedList<>();
        }

        default List<Expression> collectInternalExpr(ExprMap map) {
            //TODO
            return new LinkedList<>();
        }
    }

    @Module
    interface Transformation extends IRNode.Transformation {

        @Binding(BindingKind.INJECTED)
        CollectStatements collector();

        @Binding(BindingKind.INJECTED)
        CollectExpressions collectInternalExpr();

        @Override
        default IRNode apply(IRNode node) {
            return node.transformChildren(this);
        }

        default IRNode apply(ExprProcReturn proc) {
            //StmtLabeled rootCFG = create_CFG(proc, ReturnNode.ROOT);
            StmtLabeled exitCFG = create_CFG(proc, ReturnNode.EXIT);
            recApplySSA(exitCFG);

            return proc;
        }
    }

    //--------------- CFG Generation ---------------//

    private static StmtLabeled create_CFG(ExprProcReturn proc, ReturnNode node) {
        StmtBlock body = (StmtBlock) proc.getBody().get(0);
        ImmutableList<Statement> stmts;
        if (!(body.getVarDecls().isEmpty() && body.getTypeDecls().isEmpty())) {
            StmtBlock startingBlock = new StmtBlock(body.getTypeDecls(), body.getVarDecls(), body.getStatements());
            stmts = ImmutableList.of(startingBlock);
        } else {
            stmts = body.getStatements();
        }

        StmtLabeled entry = new StmtLabeled("ProgramEntry", null);
        StmtLabeled exit = new StmtLabeled("ProgramExit", null);

        LinkedList<StmtLabeled> sub = iterateSubStmts(stmts, exit);
        wireRelations(sub, entry, exit);

        return (node == ReturnNode.ROOT) ? entry : exit;
    }

    private enum ReturnNode {
        ROOT,
        EXIT
    }

    //TODO define labeling convention
    private static String assignLabel(Statement stmt) {
        return stmt.getClass().toString().substring(30);
    }

    private static String assignBufferLabel(Statement type, boolean isEntry) {
        return assignLabel(type) + ((isEntry) ? "Entry" : "Exit");
    }

    private static boolean isTerminalStmt(Statement stmt) {
        return stmt instanceof StmtCall ||
                stmt instanceof StmtConsume ||
                stmt instanceof StmtWrite ||
                stmt instanceof StmtRead;
    }

    private static LinkedList<StmtLabeled> iterateSubStmts(List<Statement> stmts, StmtLabeled exitBlock) {
        LinkedList<StmtLabeled> currentBlocks = new LinkedList<>();

        for (Statement currentStmt : stmts) {

            //TODO add cases
            if (isTerminalStmt(currentStmt)) {
                currentBlocks.add(createTerminalBlock(currentStmt));
            } else if (currentStmt instanceof StmtWhile) {
                currentBlocks.add(createWhileBlock((StmtWhile) currentStmt, exitBlock));
            } else if (currentStmt instanceof StmtIf) {
                currentBlocks.add(createIfBlock((StmtIf) currentStmt, exitBlock));
            } else if (currentStmt instanceof StmtBlock) {
                currentBlocks.add(createStmtBlock((StmtBlock) currentStmt, exitBlock));
            } else if (currentStmt instanceof StmtCase) {
                currentBlocks.add(createCaseBlock((StmtCase) currentStmt, exitBlock));
            } else if (currentStmt instanceof StmtForeach) {
                currentBlocks.add(createForEachBlock((StmtForeach) currentStmt, exitBlock));
            } else if (currentStmt instanceof StmtReturn) {
                currentBlocks.add(createReturnBlock((StmtReturn) currentStmt, exitBlock));
            } else if (currentStmt instanceof StmtAssignment) {
                currentBlocks.add(createStmtAssignment((StmtAssignment) currentStmt));
            } else throw new NoClassDefFoundError("Unknown Stmt type");
        }
        return currentBlocks;
    }

    //TODO FIX EMPTY CURRENTBLOCKS CASE FOR PRED AND SUCC
    private static void wireRelations(LinkedList<StmtLabeled> currentBlocks, StmtLabeled pred, StmtLabeled succ) {

        final ListIterator<StmtLabeled> it = currentBlocks.listIterator();

        StmtLabeled prev = pred;
        StmtLabeled current;
        StmtLabeled next;

        while (it.hasNext()) {
            current = it.next();
            if (it.hasNext()) {
                next = it.next();
                it.previous();
            } else {
                //if last stmt is a return stmt, go to the end of the program
                next = (current.getOriginalStmt() instanceof StmtReturn) ? current.getSuccessors().get(0) : succ;
            }
            if (current.lastIsNull()) {
                current.setRelations(ImmutableList.concat(ImmutableList.of(prev), ImmutableList.from(current.getPredecessors())),
                        ImmutableList.concat(ImmutableList.of(next), ImmutableList.from(current.getSuccessors())));
                prev = current;
            } else {
                current.setPredecessors(ImmutableList.concat(ImmutableList.of(prev), ImmutableList.from(current.getPredecessors())));
                current.getExitBlock().setSuccessors(ImmutableList.of(next));
                prev = current.getExitBlock();
            }
        }
        //set frontier blocks relations
        pred.setSuccessors(ImmutableList.concat(pred.getSuccessors(), ImmutableList.of(currentBlocks.getFirst())));
        succ.setPredecessors(ImmutableList.concat(succ.getPredecessors(), ImmutableList.of(currentBlocks.getLast().getExitBlock())));
    }

    private static StmtLabeled createReturnBlock(StmtReturn stmt, StmtLabeled exitBlock) {
        StmtLabeled stmtRet = new StmtLabeled(assignLabel(stmt), stmt);
        stmtRet.setSuccessors(ImmutableList.of(exitBlock));
        return stmtRet;
    }

    private static StmtLabeled createIfBlock(StmtIf stmt, StmtLabeled exitBlock) {

        StmtLabeled stmtIfLabeled = new StmtLabeled(assignLabel(stmt), stmt);
        StmtLabeled ifExitBuffer = new StmtLabeled(assignBufferLabel(stmt, false), null);

        LinkedList<StmtLabeled> ifBlocks = iterateSubStmts(stmt.getThenBranch(), exitBlock);
        LinkedList<StmtLabeled> elseBlocks = iterateSubStmts(stmt.getElseBranch(), exitBlock);

        wireRelations(ifBlocks, stmtIfLabeled, ifExitBuffer);
        wireRelations(elseBlocks, stmtIfLabeled, ifExitBuffer);
        stmtIfLabeled.setExit(ifExitBuffer);

        return stmtIfLabeled;
    }

    private static StmtLabeled createWhileBlock(StmtWhile stmt, StmtLabeled exitBlock) {
        ImmutableList<Statement> stmts = stmt.getBody();
        LinkedList<StmtLabeled> currentBlocks = iterateSubStmts(stmts, exitBlock);

        StmtLabeled stmtWhileLabeled = new StmtLabeled(assignLabel(stmt), stmt);

        //Add the while stmt as both predecessors and successor of its body
        wireRelations(currentBlocks, stmtWhileLabeled, stmtWhileLabeled);

        StmtLabeled entryWhile = new StmtLabeled(assignBufferLabel(stmt, true), null);
        StmtLabeled exitWhile = new StmtLabeled(assignBufferLabel(stmt, false), null);

        wireRelations(new LinkedList<>(Collections.singletonList(stmtWhileLabeled)), entryWhile, exitWhile);
        entryWhile.setExit(exitWhile);

        return entryWhile;
    }

    private static StmtLabeled createTerminalBlock(Statement stmt) {
        return new StmtLabeled(assignLabel(stmt), stmt);
    }

    private static StmtLabeled createStmtBlock(StmtBlock stmt, StmtLabeled exitBlock) {

        StmtLabeled stmtBlockLabeled = new StmtLabeled(assignLabel(stmt), stmt);

        List<LocalVarDecl> localVarDecls = stmt.getVarDecls();
        localVarDecls.forEach(SsaPhase::createNewLocalVar);
        localVarDecls.forEach(v -> stmtBlockLabeled.addLocalValueNumber(v.withName(getNewLocalValueName(v.getOriginalName())), false));

        List<Statement> body = stmt.getStatements();
        LinkedList<StmtLabeled> currentBlocks = iterateSubStmts(body, exitBlock);

        StmtLabeled stmtBlockExit = new StmtLabeled(assignBufferLabel(stmt, false), null);
        wireRelations(currentBlocks, stmtBlockLabeled, stmtBlockExit);
        stmtBlockLabeled.setExit(stmtBlockExit);

        return stmtBlockLabeled;
    }

    private static StmtLabeled createStmtAssignment(StmtAssignment stmt) {
        StmtLabeled stmtAssignLabeled = new StmtLabeled(assignLabel(stmt), stmt);
        LValue v = stmt.getLValue();
        if (v instanceof LValueVariable) {
            String varName = ((LValueVariable) v).getVariable().getOriginalName();
            LocalVarDecl currentVarDecl = originalLVD.get(varName);
            LocalVarDecl newVarDecl = currentVarDecl.withName(getNewLocalValueName(varName)).withValue(stmt.getExpression());
            stmtAssignLabeled.addLocalValueNumber(newVarDecl, false);
        }
        return stmtAssignLabeled;
    }

    private static StmtLabeled createCaseBlock(StmtCase stmt, StmtLabeled exitBlock) {
        StmtLabeled stmtCaseLabeled = new StmtLabeled(assignLabel(stmt), stmt);
        StmtLabeled stmtCaseExit = new StmtLabeled(assignBufferLabel(stmt, false), null);

        for (StmtCase.Alternative alt : stmt.getAlternatives()) {
            LinkedList<StmtLabeled> currentBlocks = iterateSubStmts(alt.getStatements(), exitBlock);
            wireRelations(currentBlocks, stmtCaseLabeled, stmtCaseExit);
        }
        stmtCaseLabeled.setExit(stmtCaseExit);
        return stmtCaseLabeled;
    }

    private static StmtLabeled createForEachBlock(StmtForeach stmt, StmtLabeled exitBlock) {
        StmtLabeled stmtFELabeled = new StmtLabeled(assignLabel(stmt), stmt);
        StmtLabeled stmtFEExit = new StmtLabeled(assignBufferLabel(stmt, false), null);

        LinkedList<StmtLabeled> currentBlocks = iterateSubStmts(stmt.getBody(), exitBlock);
        wireRelations(currentBlocks, stmtFELabeled, stmtFEExit);

        stmtFELabeled.setExit(stmtFEExit);
        return stmtFELabeled;
    }


    //--------------- SSA Algorithm Application ---------------//

    private static void recApplySSAExpr(StmtLabeled stmtLabeled) {
        if (stmtLabeled.hasPredecessors() || stmtLabeled.hasBeenVisted()) {
            return;
        }
        //TODO^
/*
        Statement originalStmt = stmtLabeled.getOriginalStmt();
        if (originalStmt != null) {
            List<Expression> stmtExpr = collector.collect(originalStmt);
            stmtExpr.forEach(e -> {
                List<Expression> internalExpr = exprCollector.collectInternalExpr(e);
                internalExpr.forEach(resolveSSAName(e)););
            });
        }
        List<Expression> expr =*/


    }

    private static Pair<LocalVarDecl, Integer> resolveSSAName(StmtLabeled stmt, Expression e, int recLvl) {
        if (stmt.getLabel().equals("ProgramEntry")) {
            //Reaches top without finding definition
            return new Pair<>(null, -1);
        }

        if (e instanceof ExprVariable) {
            String originalVarRef = ((ExprVariable) e).getVariable().getOriginalName();
            LocalVarDecl localVarDecl = stmt.containsVarDef(originalVarRef);
            if (localVarDecl == null) {

                //TODO handle case where no assignment of a variable happen to a phi situation variable. This means there's no SSA available

                List<Pair<LocalVarDecl, Integer>> prevVarFound = new LinkedList<>();
                stmt.getPredecessors().forEach(pred -> prevVarFound.add(resolveSSAName(pred, e, recLvl + 1)));

                //TODO check logic
                boolean foundPhi = false;
                int nb_found = 0;
                int smallest = Integer.MAX_VALUE;
                for (Pair p : prevVarFound) {
                    int recValue = (int) p.getValue();
                    if (recValue <= smallest) {
                        if (recValue == smallest) {
                            foundPhi = false;
                        } else {
                            smallest = recValue;
                            foundPhi = ((LocalVarDecl) p.getKey()).getValue() instanceof ExprPhi;
                        }

                        if (recValue != -1) {
                            ++nb_found;
                        }

                    }
                }
            if (nb_found > 0){
                if(!foundPhi){
                //TODO must apply algorithm
                } else {
                    //TODO return what's been found
                }
            } else {
                //TODO nothing found, return error
            }

            //found locally
        } else {
            return new Pair<>(localVarDecl, recLvl);
        }
    }
        return null;
}


    private static void recApplySSA(StmtLabeled stmtLabeled) {
        //Stop recursion at the top of the cfg
        if (stmtLabeled.getPredecessors().isEmpty() || stmtLabeled.hasBeenVisted()) {
            return;
        }

        //if in Assignment or Block
        LinkedList<LocalVarDecl> lvd = new LinkedList<>(stmtLabeled.getLocalValueNumbers().keySet());
        lvd.removeIf(lv -> lv.getValue() instanceof ExprPhi || stmtLabeled.getLocalValueNumbers().get(lv)); //if ExprPhi or lvd has already been visited
        if (!lvd.isEmpty()) {
            lvd.forEach(l -> stmtLabeled.addLocalValueNumber(l.withValue(recReadLocalVarExpr(l.getValue(), stmtLabeled)), true));
        }
        stmtLabeled.setHasBeenVisted(); //TODO Problem for While EDIT : Fix with whileExitBlock
        stmtLabeled.getPredecessors().forEach(SsaPhase::recApplySSA);
    }


    private static Expression recReadLocalVarExpr(Expression expr, StmtLabeled stmtLabeled) {

        if (expr instanceof ExprLiteral) {
            return expr;

        } else if (expr instanceof ExprVariable) {

            LocalVarDecl result = readVar(stmtLabeled, ((ExprVariable) expr).getVariable());
            ExprVariable newVar = ((ExprVariable) expr).copy(variable(result.getName()), ((ExprVariable) expr).getOld());
            return newVar;

        } else if (expr instanceof ExprBinaryOp) {

            List<Expression> newOperands = new LinkedList<>(((ExprBinaryOp) expr).getOperands());
            //TODO Handle self reference
            newOperands.replaceAll(o -> recReadLocalVarExpr(o, stmtLabeled));
            ExprBinaryOp newVar = ((ExprBinaryOp) expr).copy(((ExprBinaryOp) expr).getOperations(), ImmutableList.from(newOperands));
            return newVar;

        } else if (expr instanceof ExprUnaryOp) {
            Expression newOperand = recReadLocalVarExpr(((ExprUnaryOp) expr).getOperand(), stmtLabeled);
            ExprUnaryOp newVar = ((ExprUnaryOp) expr).copy(((ExprUnaryOp) expr).getOperation(), newOperand);
            return newVar;
            //TODO
        } else {
            return null;
        }
    }

    private static Variable findSelfReference(List<Expression> operands, LocalVarDecl originalStmt) {
        //find variable corresponding to local var declaration in operands of BinaryExpr or return null
        return operands.stream()
                .filter(o -> (o instanceof ExprVariable && originalStmt.getOriginalName().equals(((ExprVariable) o).getVariable().getOriginalName())))
                .findAny()
                .map(expression -> ((ExprVariable) expression).getVariable())
                .orElse(null);
    }

    //Respects Statements immutability
    private static Statement setSsaResult(Statement stmt, List<Expression> ssaExpr) {
        if (stmt instanceof StmtAssignment) {
            return ((StmtAssignment) stmt).copy(((StmtAssignment) stmt).getLValue(), ssaExpr.get(0));
        } else if (stmt instanceof StmtCall) {
            return ((StmtCall) stmt).copy(((StmtCall) stmt).getProcedure(), ssaExpr);
        } else if (stmt instanceof StmtForeach) {
            return ((StmtForeach) stmt).copy(((StmtForeach) stmt).getGenerator(), ssaExpr, ((StmtForeach) stmt).getBody());
        } else if (stmt instanceof StmtIf) {
            return ((StmtIf) stmt).copy(ssaExpr.get(0), ((StmtIf) stmt).getThenBranch(), ((StmtIf) stmt).getElseBranch());
        } else if (stmt instanceof StmtReturn) {
            return ((StmtReturn) stmt).copy(ssaExpr.get(0));
        } else if (stmt instanceof StmtWhile) {
            return ((StmtWhile) stmt).copy(ssaExpr.get(0), ((StmtWhile) stmt).getBody());
        } else {
            return stmt;
        }
    }

    //TODO check if additions are needed
    private static boolean containsExpression(Statement stmt) {
        return stmt instanceof StmtAssignment ||
                stmt instanceof StmtCall ||
                stmt instanceof StmtForeach ||
                stmt instanceof StmtIf ||
                stmt instanceof StmtReturn ||
                stmt instanceof StmtWhile;
    }


    //TODO check for all cases if expr are needed or not
//TODO check if potential infinite loops
    private static List<ExprVariable> recFindExprVar(Expression expr) {
        List<ExprVariable> exprVar = new LinkedList<>();
        List<Expression> subExpr = new LinkedList<>();

        if (expr instanceof ExprApplication) {
            subExpr.addAll(((ExprApplication) expr).getArgs());

        } else if (expr instanceof ExprBinaryOp) {
            subExpr.addAll(((ExprBinaryOp) expr).getOperands());

        } else if (expr instanceof ExprCase) {
            subExpr.add(((ExprCase) expr).getScrutinee());
            ((ExprCase) expr).getAlternatives().forEach(a -> subExpr.add(a.getExpression()));

        } else if (expr instanceof ExprComprehension) { //TODO check collection
            subExpr.addAll(((ExprComprehension) expr).getFilters());

        } else if (expr instanceof ExprDeref) {
            subExpr.add(((ExprDeref) expr).getReference());

        } else if (expr instanceof ExprIf) {
            subExpr.addAll(new ArrayList<>(Arrays.asList(((ExprIf) expr).getCondition(), ((ExprIf) expr).getElseExpr(), ((ExprIf) expr).getThenExpr())));

        } else if (expr instanceof ExprLambda) {
            subExpr.add(((ExprLambda) expr).getBody());

        } else if (expr instanceof ExprList) {
            subExpr.addAll(((ExprList) expr).getElements());

        } else if (expr instanceof ExprSet) {
            subExpr.addAll(((ExprSet) expr).getElements());

        } else if (expr instanceof ExprTuple) {
            subExpr.addAll(((ExprTuple) expr).getElements());

        } else if (expr instanceof ExprTypeConstruction) {
            subExpr.addAll(((ExprTypeConstruction) expr).getArgs());

        } else if (expr instanceof ExprUnaryOp) {
            subExpr.add(((ExprUnaryOp) expr).getOperand());

        } else if (expr instanceof ExprVariable) {
            exprVar.add((ExprVariable) expr);
            return exprVar;

        }//TODO check all necessary cases;
        else if (expr instanceof ExprLet) {
            //TODO
        } else if (expr instanceof ExprRef) {
            //TODO
        }
        subExpr.forEach(e -> exprVar.addAll(recFindExprVar(e)));
        return exprVar;
    }

    private static List<ExprVariable> findExprVar(List<Expression> expr) {
        List<ExprVariable> exprVar = new LinkedList<>();
        expr.forEach(e -> exprVar.addAll(recFindExprVar(e)));
        return exprVar;
    }

    //--------------- Local Value Numbering ---------------//
    private static HashMap<String, LocalVarDecl> originalLVD = new HashMap<>();
    private static HashMap<String, Integer> localValueCounter = new HashMap<>();

    private static void createNewLocalVar(LocalVarDecl v) {
        originalLVD.put(v.getOriginalName(), v);
    }

    private static String getNewLocalValueName(String var) {
        if (localValueCounter.containsKey(var)) {
            localValueCounter.merge(var, 1, Integer::sum);
            return var + "_SSA_" + (localValueCounter.get(var)).toString();
        } else {
            localValueCounter.put(var, 0);
            return var + "_SSA_0";
        }
    }

    private static LocalVarDecl createLVDWithVNAndExpr(Variable var, Expression expr) {
        String newName = getNewLocalValueName(var.getOriginalName());
        LocalVarDecl originalDef = originalLVD.get(var.getOriginalName());
        return originalDef.withName(newName).withValue(expr);
    }


//--------------- SSA Algorithm ---------------//

    private static Expression replaceVariableInExpr(Expression originalExpr, LocalVarDecl varToReplace, LocalVarDecl replacementVar, StmtLabeled stmt) {
        //TODO add cases for other types of Expressions
        //Should work for interlocked ExprBinary
        if (originalExpr instanceof ExprBinaryOp) {
            List<Expression> operands = ((ExprBinaryOp) originalExpr).getOperands();
            ExprVariable updatedVariable = new ExprVariable(variable(replacementVar.getName()));
            //find and replace variable looked for, apply algorithm to all other operands
            List<Expression> newOperands = operands.stream()
                    .map(o -> (o instanceof ExprVariable && ((ExprVariable) o).getVariable().getOriginalName().equals(varToReplace.getOriginalName())) ? updatedVariable : recReadLocalVarExpr(o, stmt))
                    .collect(Collectors.toList());
            return new ExprBinaryOp(((ExprBinaryOp) originalExpr).getOperations(), ImmutableList.from(newOperands));
        }
        return null;
    }

    private static LocalVarDecl handleSelfReference(StmtLabeled stmt, Variable var, LocalVarDecl selfAssignedVar) {

        //TODO check for multiple self references
        LocalVarDecl temp = new LocalVarDecl(selfAssignedVar.getAnnotations(), selfAssignedVar.getType(), "u", null, selfAssignedVar.isConstant());
        createNewLocalVar(temp);

        Variable loopStartVarName = variable(temp.getName());
        LocalVarDecl loopStartVar = temp.withName(getNewLocalValueName(temp.getOriginalName()));  //u0
        LocalVarDecl exitVar = temp.withName(getNewLocalValueName(temp.getName())).withValue(new ExprVariable(variable(loopStartVar.getName()))); //u1

        //replace problematic reference
        Expression oldExpr = selfAssignedVar.getValue();
        Expression newExpr = replaceVariableInExpr(oldExpr, selfAssignedVar, exitVar, stmt);
        LocalVarDecl updatedSelfAssignedVar = selfAssignedVar.withValue(newExpr);

        LocalVarDecl loopEndVar = temp.withName(getNewLocalValueName(temp.getName())).withValue(new ExprVariable(variable(updatedSelfAssignedVar.getName()))); //u2

        LocalVarDecl predecessorVariable = readVarRec(stmt, var);
        ExprPhi phiWithSelf = new ExprPhi(loopStartVarName, ImmutableList.from(Arrays.asList(predecessorVariable, loopEndVar)));
        loopStartVar = loopStartVar.withValue(phiWithSelf);

        //add all localVarDeclarations resulting from the operation
        stmt.addLocalValueNumber(Stream.of(loopStartVar, loopEndVar, updatedSelfAssignedVar, exitVar).collect(Collectors.toMap(lv -> lv, lv -> true)));

        //redirect to exitVar
        return exitVar;
    }

    //TODO check which type of "var" to look for. Tried with Variable
    private static LocalVarDecl readVar(StmtLabeled stmt, Variable var) {

        //look for self reference
        Optional<LocalVarDecl> matchingLVD = stmt.getLocalValueNumbers().keySet().stream().filter(l -> l.getOriginalName().equals(var.getOriginalName())).findAny();
        if (matchingLVD.isPresent()) {
            //Locally found
            LocalVarDecl lv = matchingLVD.get();
            Expression lvExpr = lv.getValue();
            if (lvExpr instanceof ExprBinaryOp) {
                Variable selfRefVar = findSelfReference(((ExprBinaryOp) lvExpr).getOperands(), lv);
                if (selfRefVar != null) {
                    LocalVarDecl result = handleSelfReference(stmt, selfRefVar, lv);
                    return result;
                }
            }
            //no self reference
            return lv;
        } else {
            //No def found in current Statement
            return readVarRec(stmt, var);
        }
    }

    private static LocalVarDecl readVarRec(StmtLabeled stmt, Variable var) {

        if (stmt.getPredecessors().size() == 1) {
            return readVar(stmt.getPredecessors().get(0), var);
        } else {
            ExprPhi phiExpr = new ExprPhi(var, ImmutableList.empty());
            //Add Phi to Global value numbering
            LocalVarDecl localVarPhi = createLVDWithVNAndExpr(var, phiExpr);
            stmt.addLocalValueNumber(localVarPhi, true);
            localVarPhi = addPhiOperands(localVarPhi, var, stmt.getPredecessors());

            Expression phiResult = localVarPhi.getValue();
            if (phiResult instanceof ExprPhi && !((ExprPhi) phiResult).isUndefined()) {
                stmt.addLocalValueNumber(localVarPhi, true);
            }
            return localVarPhi;
        }
    }

    private static LocalVarDecl addPhiOperands(LocalVarDecl phi, Variable var, List<StmtLabeled> predecessors) {
        LinkedList<LocalVarDecl> phiOperands = new LinkedList<>();

        for (StmtLabeled stmt : predecessors) {
            LocalVarDecl lookedUpVar = readVar(stmt, var);
            phiOperands.add(lookedUpVar);
            //add Phi to list of users of its operands
            if (lookedUpVar.getValue() instanceof ExprPhi) {
                ((ExprPhi) lookedUpVar.getValue()).addUser(ImmutableList.of(phi));
            }
        }
        ((ExprPhi) phi.getValue()).setOperands(phiOperands);
        return tryRemoveTrivialPhi(phi);
    }

    private static LocalVarDecl tryRemoveTrivialPhi(LocalVarDecl phi) {
        LocalVarDecl currentOp = null;
        ImmutableList<LocalVarDecl> operands = ((ExprPhi) phi.getValue()).getOperands();
        for (LocalVarDecl op : operands) {
            //Unique value or self reference
            if (!op.equals(currentOp) && !op.equals(phi)) {
                if (currentOp != null) {
                    return phi;
                }
            }
            currentOp = op;
        }

        if (currentOp == null) {
            ((ExprPhi) phi.getValue()).becomesUndefined();
        }

        LinkedList<LocalVarDecl> phiUsers = ((ExprPhi) phi.getValue()).getUsers();
        phiUsers.remove(phi);

        for (LocalVarDecl userPhi : phiUsers) {
            LinkedList<LocalVarDecl> userPhiUsers = ((ExprPhi) userPhi.getValue()).getUsers();
            userPhiUsers.set(userPhiUsers.indexOf(phi), currentOp);
            ((ExprPhi) userPhi.getValue()).addUser(userPhiUsers);
        }

        for (LocalVarDecl user : phiUsers) {
            tryRemoveTrivialPhi(user);
        }

        return (((ExprPhi) phi.getValue()).isUndefined()) ? phi : currentOp;
    }
}
