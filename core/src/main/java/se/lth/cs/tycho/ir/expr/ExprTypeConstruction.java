package se.lth.cs.tycho.ir.expr;

import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.ir.util.Lists;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ExprTypeConstruction extends Expression {

	private String type;
	private ImmutableList<Expression> args;

	public ExprTypeConstruction(String type, List<Expression> args) {
		this(null, type, args);
	}

	private ExprTypeConstruction(IRNode original, String type, List<Expression> args) {
		super(original);
		this.type = type;
		this.args = ImmutableList.from(args);
	}

	public String getType() {
		return type;
	}

	public ImmutableList<Expression> getArgs() {
		return args;
	}

	public ExprTypeConstruction copy(String type, List<Expression> args) {
		if (Objects.equals(type, getType()) && Lists.sameElements(args, getArgs())) {
			return this;
		} else {
			return new ExprTypeConstruction(this, type, args);
		}
	}

	@Override
	public void forEachChild(Consumer<? super IRNode> action) {
		args.forEach(action);
	}

	@Override
	public Expression transformChildren(Transformation transformation) {
		return copy(getType(), (List) getArgs().map(transformation));
	}
}
