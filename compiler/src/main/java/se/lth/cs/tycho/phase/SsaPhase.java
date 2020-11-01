package se.lth.cs.tycho.phase;

import org.multij.Module;
import org.multij.MultiJ;
import se.lth.cs.tycho.compiler.CompilationTask;
import se.lth.cs.tycho.compiler.Context;
import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.expr.ExprProcReturn;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.*;
import se.lth.cs.tycho.ir.stmt.lvalue.LValue;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;
import se.lth.cs.tycho.ir.stmt.ssa.ExprPhi;
import se.lth.cs.tycho.ir.stmt.ssa.StmtLabeled;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.reporting.CompilationException;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class SsaPhase implements Phase {

    @Override
    public String getDescription() {
        return "Applies SsaPhase transformation to ExprProcReturn";
    }


    @Override
    public CompilationTask execute(CompilationTask task, Context context) throws CompilationException {
        Transformation transformation = MultiJ.from(SsaPhase.Transformation.class)
                .instance();

        return task.transformChildren(transformation);
    }

    @Module
    interface Transformation extends IRNode.Transformation {
        @Override
        default IRNode apply(IRNode node) {
            return node.transformChildren(this);
        }

        default IRNode apply(ExprProcReturn proc) {
            //ImmutableList<ParameterVarDecl> paramVarDecl = proc.getValueParameters();
            ImmutableList<Statement> stmts = proc.getBody();
            StmtLabeled rootCFG = create_CFG(proc);
            return proc;
        }

    }

    //TODO define labeling convention
    private static String assignLabel(Statement stmt) {
        return stmt.getClass().toString().substring(30);
    }


    private static LinkedList<StmtLabeled> iterateSubStmts(List<Statement> stmts) {
        LinkedList<StmtLabeled> currentBlocks = new LinkedList<>();

        for (Statement currentStmt : stmts) {

            //TODO add cases
            if (isTerminalStmt(currentStmt)) {
                currentBlocks.add(create_SimpleBlock(currentStmt));
            } else if (currentStmt instanceof StmtWhile) {
                currentBlocks.add(create_WhileBlock((StmtWhile) currentStmt));
            } else if (currentStmt instanceof StmtIf) {
                currentBlocks.add(create_IfBlock((StmtIf) currentStmt));
            } else if (currentStmt instanceof StmtBlock) {
            } else if (currentStmt instanceof StmtCase) {
            } else if (currentStmt instanceof StmtForeach) {
            } else if (currentStmt instanceof StmtReturn) {
            }
        }
        return currentBlocks;
    }

    private static StmtLabeled create_SimpleBlock(Statement stmt) {
        StmtLabeled ret = new StmtLabeled(assignLabel(stmt), stmt);
        return ret;
    }

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
                next = succ;
            }
            if (current.lastIsNull()) {
                current.setRelations(ImmutableList.of(prev), ImmutableList.of(next));
                prev = current;
            } else {
                current.setPredecessors(ImmutableList.of(prev));
                current.getExitBlock().setSuccessors(ImmutableList.of(next));
                prev = current.getExitBlock();
            }
        }
        //set frontier blocks relations
        pred.setSuccessors(ImmutableList.concat(pred.getSuccessors(), ImmutableList.of(currentBlocks.getFirst())));
        succ.setPredecessors(ImmutableList.concat(succ.getPredecessors(), ImmutableList.of(currentBlocks.getLast().getExitBlock())));
    }

    private static StmtLabeled create_IfBlock(StmtIf stmt) {

        StmtLabeled stmtIfLabeled = new StmtLabeled(assignLabel(stmt), stmt);
        StmtLabeled ifExitBuffer = new StmtLabeled("ExitBuffer", null);

        LinkedList<StmtLabeled> ifBlocks = iterateSubStmts(stmt.getThenBranch());
        LinkedList<StmtLabeled> elseBlocks = iterateSubStmts(stmt.getElseBranch());

        wireRelations(ifBlocks, stmtIfLabeled, ifExitBuffer);
        wireRelations(elseBlocks, stmtIfLabeled, ifExitBuffer);
        stmtIfLabeled.setExit(ifExitBuffer);
        //TODO make immutable
        return stmtIfLabeled;
    }

    private static StmtLabeled create_WhileBlock(StmtWhile stmt) {
        ImmutableList<Statement> stmts = stmt.getBody();
        LinkedList<StmtLabeled> currentBlocks = iterateSubStmts(stmts);

        StmtLabeled stmtWhileLabeled = new StmtLabeled(assignLabel(stmt), stmt);

        //Add the while stmt as both predecessors and successor of its body
        wireRelations(currentBlocks, stmtWhileLabeled, stmtWhileLabeled);

        return stmtWhileLabeled;


    /*    StmtBlockLabeled entryBuffer = new StmtBlockLabeled("buffer", null, ImmutableList.empty())
                .withRelations(ImmutableList.of(pred), ImmutableList.of(finished));
        StmtBlockLabeled outputBuffer = new StmtBlockLabeled("buffer", null, ImmutableList.empty())
                .withRelations(ImmutableList.of(finished), ImmutableList.of(succ));*/
    }


    private static StmtLabeled create_CFG(ExprProcReturn proc) {
        StmtBlock body = (StmtBlock) proc.getBody().get(0);
        ImmutableList<Statement> stmts = body.getStatements();

        StmtLabeled entry = new StmtLabeled("entry", null);
        StmtLabeled exit = new StmtLabeled("exit", null);

        LinkedList<StmtLabeled> sub = iterateSubStmts(stmts);
        wireRelations(sub, entry, exit);

        return entry;
    }

    private static boolean isTerminalStmt(Statement stmt) {
        return stmt instanceof StmtAssignment ||
                stmt instanceof StmtCall ||
                stmt instanceof StmtConsume ||
                stmt instanceof StmtWrite ||
                stmt instanceof StmtRead;
    }


//--------------- SSA Algorithm ---------------//

    private Expression readVar(StmtLabeled stmt, LValue var) {
        Statement originalStmt = stmt.getOriginalStmt();
        if (originalStmt instanceof StmtAssignment) {
            LValue v = ((StmtAssignment) originalStmt).getLValue();
            if (v.equals(var)) {
                return ((StmtAssignment) originalStmt).getExpression();
            }
        } else if (originalStmt instanceof StmtBlock) {
            ImmutableList<LocalVarDecl> localVarDecls = ((StmtBlock) originalStmt).getVarDecls();
            for (LocalVarDecl v : localVarDecls) {
                if (v.getName().equals(((LValueVariable) var).getVariable().getName())) {
                    return v.getValue();
                }
            }
        }
        return readVarRec(stmt, var);
    }

    private Expression readVarRec(StmtLabeled stmt, LValue var) {
        //Statement originalStmt = stmt.getOriginalStmt();
        if (stmt.getPredecessors().size() == 1) {
            return readVar(stmt.getPredecessors().get(0), var);
        } else {
            ExprPhi phi = new ExprPhi(var, ImmutableList.empty());
            return addPhiOperands(phi, var, stmt.getPredecessors());
        }
    }

    private Expression addPhiOperands(ExprPhi phi, LValue var, List<StmtLabeled> predecessors) {
        LinkedList<Expression> phiOperands = new LinkedList<>();

        for (StmtLabeled stmt : predecessors) {
            Expression lookedUpVar = readVar(stmt, var);
            phiOperands.add(lookedUpVar);
            //add Phi to list of users of its operands
            if (lookedUpVar instanceof ExprPhi) {
                ((ExprPhi) lookedUpVar).addUser(ImmutableList.of(phi));
            }
        }
        ExprPhi newPhi = new ExprPhi(var, phiOperands);
        return tryRemoveTrivialPhi(newPhi);
    }

    private Expression tryRemoveTrivialPhi(ExprPhi phi) {
        Expression currentOp = null;
        ImmutableList<Expression> operands = phi.getOperands();
        for (Expression op : operands) {
            //clean up ugly continue
            if (op.equals(currentOp) || (op instanceof ExprPhi && op.equals(phi))) {
                continue;
            }
            if (currentOp != null) {
                return phi;
            }
            currentOp = op;
        }
        //TODO set currentOp to undefined value

        LinkedList<Expression> phiUsers = phi.getUsers();
        phiUsers.remove(phi);
        //TODO check if can only be done for ExprPhi
        for (Expression userPhi : phiUsers) {
            if (userPhi instanceof ExprPhi) {
                LinkedList<Expression> userPhiUsers = ((ExprPhi) userPhi).getUsers();
                userPhiUsers.set(userPhiUsers.indexOf(phi), currentOp);
                ((ExprPhi) userPhi).addUser(userPhiUsers);
            }
        }

        for (Expression user : phiUsers) {
            if (user instanceof ExprPhi) {
                tryRemoveTrivialPhi((ExprPhi) user);
            }
        }
        return currentOp;
    }
}
