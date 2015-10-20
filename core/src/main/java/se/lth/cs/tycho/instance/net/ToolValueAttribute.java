package se.lth.cs.tycho.instance.net;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import se.lth.cs.tycho.ir.AbstractIRNode;
import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.util.PrettyPrint;
/**
 * @author Per Andersson <Per.Andersson@cs.lth.se>
 *
 */

public class ToolValueAttribute extends ToolAttribute {
	public ToolValueAttribute(String name, Expression value){
		super(name);
		this.value = value;
	}

	protected ToolValueAttribute(ToolValueAttribute original, String name, Expression value) {
		super (original, name);
		this.value = value;
	}
	
	public ToolValueAttribute copy(String name, Expression value){
		if(Objects.equals(this.name, name) && Objects.equals(this.value, value)){
			return this;
		}
		return new ToolValueAttribute(this, name, value);
	}

	public Expression getValue(){
		return value;
	}
	public void print(java.io.PrintStream out){
		out.append(name);
		out.append(" = ");
		new PrettyPrint().print(value);
	}
	
	@Override
	Kind getKind() {
		return Kind.type;
	}

	private Expression value;

	@Override
	public ToolValueAttribute transformChildren(Function<? super IRNode, ? extends IRNode> transformation) {
		return copy(name, (Expression) transformation.apply(value));
	}

	@Override
	public void forEachChild(Consumer<? super IRNode> action) {
		action.accept(value);
	}
}
