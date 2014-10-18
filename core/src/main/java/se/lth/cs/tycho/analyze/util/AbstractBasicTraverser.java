package se.lth.cs.tycho.analyze.util;

import java.util.Map.Entry;

import se.lth.cs.tycho.ir.Field;
import se.lth.cs.tycho.ir.GeneratorFilter;
import se.lth.cs.tycho.ir.Parameter;
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.TypeExpr;
import se.lth.cs.tycho.ir.Variable;
import se.lth.cs.tycho.ir.decl.TypeDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.ExprApplication;
import se.lth.cs.tycho.ir.expr.ExprBinaryOp;
import se.lth.cs.tycho.ir.expr.ExprField;
import se.lth.cs.tycho.ir.expr.ExprIf;
import se.lth.cs.tycho.ir.expr.ExprIndexer;
import se.lth.cs.tycho.ir.expr.ExprInput;
import se.lth.cs.tycho.ir.expr.ExprLambda;
import se.lth.cs.tycho.ir.expr.ExprLet;
import se.lth.cs.tycho.ir.expr.ExprList;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprMap;
import se.lth.cs.tycho.ir.expr.ExprProc;
import se.lth.cs.tycho.ir.expr.ExprSet;
import se.lth.cs.tycho.ir.expr.ExprUnaryOp;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.expr.ExpressionVisitor;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StatementVisitor;
import se.lth.cs.tycho.ir.stmt.StmtAssignment;
import se.lth.cs.tycho.ir.stmt.StmtBlock;
import se.lth.cs.tycho.ir.stmt.StmtCall;
import se.lth.cs.tycho.ir.stmt.StmtConsume;
import se.lth.cs.tycho.ir.stmt.StmtForeach;
import se.lth.cs.tycho.ir.stmt.StmtIf;
import se.lth.cs.tycho.ir.stmt.StmtOutput;
import se.lth.cs.tycho.ir.stmt.StmtWhile;
import se.lth.cs.tycho.ir.stmt.lvalue.LValue;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueField;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueIndexer;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVisitor;
import se.lth.cs.tycho.ir.util.ImmutableList;

public abstract class AbstractBasicTraverser<P> implements BasicTraverser<P>, ExpressionVisitor<Void, P>,
		StatementVisitor<Void, P>, LValueVisitor<Void, P> {

	@Override
	public Void visitLValueVariable(LValueVariable lvalue, P parameter) {
		traverseVariable(lvalue.getVariable(), parameter);
		return null;
	}

	@Override
	public Void visitLValueIndexer(LValueIndexer lvalue, P parameter) {
		traverseLValue(lvalue.getStructure(), parameter);
		traverseExpression(lvalue.getIndex(), parameter);
		return null;
	}

	@Override
	public Void visitLValueField(LValueField lvalue, P parameter) {
		traverseLValue(lvalue.getStructure(), parameter);
		traverseField(lvalue.getField(), parameter);
		return null;
	}

	@Override
	public Void visitStmtAssignment(StmtAssignment s, P p) {
		traverseLValue(s.getLValue(), p);
		traverseExpression(s.getExpression(), p);
		return null;
	}

	@Override
	public Void visitStmtBlock(StmtBlock s, P p) {
		traverseTypeDecls(s.getTypeDecls(), p);
		traverseVarDecls(s.getVarDecls(), p);
		traverseStatements(s.getStatements(), p);
		return null;
	}

	@Override
	public Void visitStmtIf(StmtIf s, P p) {
		traverseExpression(s.getCondition(), p);
		traverseStatement(s.getThenBranch(), p);
		traverseStatement(s.getElseBranch(), p);
		return null;
	}

	@Override
	public Void visitStmtCall(StmtCall s, P p) {
		traverseExpression(s.getProcedure(), p);
		traverseExpressions(s.getArgs(), p);
		return null;
	}

	@Override
	public Void visitStmtOutput(StmtOutput s, P p) {
		traversePort(s.getPort(), p);
		traverseExpressions(s.getValues(), p);
		return null;
	}

	@Override
	public Void visitStmtConsume(StmtConsume s, P p) {
		traversePort(s.getPort(), p);
		return null;
	}

	@Override
	public Void visitStmtWhile(StmtWhile s, P p) {
		traverseExpression(s.getCondition(), p);
		traverseStatement(s.getBody(), p);
		return null;
	}

	@Override
	public Void visitStmtForeach(StmtForeach s, P p) {
		traverseGenerators(s.getGenerators(), p);
		traverseStatement(s.getBody(), p);
		return null;
	}

	@Override
	public Void visitExprApplication(ExprApplication e, P p) {
		traverseExpression(e.getFunction(), p);
		traverseExpressions(e.getArgs(), p);
		return null;
	}

	@Override
	public Void visitExprBinaryOp(ExprBinaryOp e, P p) {
		traverseExpressions(e.getOperands(), p);
		return null;
	}

	@Override
	public Void visitExprField(ExprField e, P p) {
		traverseExpression(e.getStructure(), p);
		traverseField(e.getField(), p);
		return null;
	}

	@Override
	public Void visitExprIf(ExprIf e, P p) {
		traverseExpression(e.getCondition(), p);
		traverseExpression(e.getThenExpr(), p);
		traverseExpression(e.getElseExpr(), p);
		return null;
	}

	@Override
	public Void visitExprIndexer(ExprIndexer e, P p) {
		traverseExpression(e.getStructure(), p);
		traverseExpression(e.getIndex(), p);
		return null;
	}

	@Override
	public Void visitExprInput(ExprInput e, P p) {
		traversePort(e.getPort(), p);
		return null;
	}

	@Override
	public Void visitExprLambda(ExprLambda e, P p) {
		traverseTypeParameters(e.getTypeParameters(), p);
		traverseValueParameters(e.getValueParameters(), p);
		traverseTypeExpr(e.getReturnType(), p);
		traverseExpression(e.getBody(), p);
		if(e.isFreeVariablesComputed()){
			traverseVariables(e.getFreeVariables(), p);
		}
		return null;
	}

	@Override
	public Void visitExprLet(ExprLet e, P p) {
		traverseTypeDecls(e.getTypeDecls(), p);
		traverseVarDecls(e.getVarDecls(), p);
		traverseExpression(e.getBody(), p);
		return null;
	}

	@Override
	public Void visitExprList(ExprList e, P p) {
		traverseGenerators(e.getGenerators(), p);
		traverseExpressions(e.getElements(), p);
		return null;
	}

	@Override
	public Void visitExprLiteral(ExprLiteral e, P p) {
		return null;
	}

	@Override
	public Void visitExprMap(ExprMap e, P p) {
		traverseGenerators(e.getGenerators(), p);
		for (Entry<Expression, Expression> mapping : e.getMappings()) {
			traverseExpression(mapping.getKey(), p);
			traverseExpression(mapping.getValue(), p);
		}
		return null;
	}

	@Override
	public Void visitExprProc(ExprProc e, P p) {
		traverseTypeParameters(e.getTypeParameters(), p);
		traverseValueParameters(e.getValueParameters(), p);
		traverseStatement(e.getBody(), p);
		if(e.isFreeVariablesComputed()){
			traverseVariables(e.getFreeVariables(), p);
		}
		return null;
	}

	@Override
	public Void visitExprSet(ExprSet e, P p) {
		traverseGenerators(e.getGenerators(), p);
		traverseExpressions(e.getElements(), p);
		return null;
	}

	@Override
	public Void visitExprUnaryOp(ExprUnaryOp e, P p) {
		traverseExpression(e.getOperand(), p);
		return null;
	}

	@Override
	public Void visitExprVariable(ExprVariable e, P p) {
		traverseVariable(e.getVariable(), p);
		return null;
	}
	
	@Override
	public void traverseQualifiedIdentifier(QID qid, P p) {
	}

	@Override
	public void traverseExpression(Expression expr, P param) {
		if (expr != null) {
			expr.accept(this, param);
		}
	}

	@Override
	public void traverseExpressions(ImmutableList<Expression> expr, P param) {
		for (Expression e : expr) {
			traverseExpression(e, param);
		}
	}

	@Override
	public void traverseStatement(Statement stmt, P param) {
		if (stmt != null) {
			stmt.accept(this, param);
		}
	}

	@Override
	public void traverseStatements(ImmutableList<Statement> stmt, P param) {
		for (Statement s : stmt) {
			traverseStatement(s, param);
		}
	}

	@Override
	public void traverseLValue(LValue lvalue, P param) {
		lvalue.accept(this, param);
	}

	@Override
	public void traverseVarDecl(VarDecl varDecl, P param) {
		traverseTypeExpr(varDecl.getType(), param);
		traverseExpression(varDecl.getValue(), param);
	}

	@Override
	public void traverseVarDecls(ImmutableList<VarDecl> varDecl, P param) {
		for (VarDecl v : varDecl) {
			traverseVarDecl(v, param);
		}
	}

	@Override
	public void traverseTypeDecl(TypeDecl typeDecl, P param) {
	}

	@Override
	public void traverseTypeDecls(ImmutableList<TypeDecl> typeDecl, P param) {
		for (TypeDecl d : typeDecl) {
			traverseTypeDecl(d, param);
		}
	}

	@Override
	public void traverseValueParameter(VarDecl valueParam, P param) {
		traverseTypeExpr(valueParam.getType(), param);
	}

	@Override
	public void traverseValueParameters(ImmutableList<VarDecl> valueParam, P param) {
		for (VarDecl p : valueParam) {
			traverseValueParameter(p, param);
		}
	}

	@Override
	public void traverseTypeParameter(TypeDecl typeParam, P param) {
	}

	@Override
	public void traverseTypeParameters(ImmutableList<TypeDecl> typeParam, P param) {
		for (TypeDecl p : typeParam) {
			traverseTypeParameter(p, param);
		}
	}

	@Override
	public void traverseGenerator(GeneratorFilter generator, P param) {
		traverseExpression(generator.getCollectionExpr(), param);
		traverseVarDecls(generator.getVariables(), param);
		traverseExpressions(generator.getFilters(), param);
	}

	@Override
	public void traverseGenerators(ImmutableList<GeneratorFilter> generator, P param) {
		for (GeneratorFilter gen : generator) {
			traverseGenerator(gen, param);
		}
	}

	@Override
	public void traverseVariable(Variable var, P param) {
	}

	@Override
	public void traverseVariables(ImmutableList<Variable> varList, P param) {
		for(Variable v : varList){
			traverseVariable(v, param);
		}
	}

	@Override
	public void traverseField(Field field, P param) {
	}

	@Override
	public void traversePort(Port port, P param) {
	}

	@Override
	public void traverseTypeExpr(TypeExpr typeExpr, P param) {
		if (typeExpr == null)
			return;
		ImmutableList<Parameter<TypeExpr>> typeParameters = typeExpr.getTypeParameters();
		if (typeParameters != null) {
			for (Parameter<TypeExpr> typeParam : typeParameters) {
				traverseTypeExpr(typeParam.getValue(), param);
			}
		}
		ImmutableList<Parameter<Expression>> valueParameters = typeExpr.getValueParameters();
		if (valueParameters != null) {
			for (Parameter<Expression> valParam : valueParameters) {
				traverseExpression(valParam.getValue(), param);
			}
		}
	}

}
