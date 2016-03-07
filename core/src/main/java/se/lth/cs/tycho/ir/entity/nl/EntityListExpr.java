package se.lth.cs.tycho.ir.entity.nl;

import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.ToolAttribute;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.ir.util.Lists;

import java.util.List;
import java.util.function.Consumer;

public class EntityListExpr extends EntityExpr {

	public EntityListExpr(List<EntityExpr> entityList) {
		this(null, entityList);
	}

	private EntityListExpr(EntityListExpr original, List<EntityExpr> entityList) {
		super(original);
		this.entityList = ImmutableList.from(entityList);
	}

	public EntityListExpr copy(List<EntityExpr> entityList) {
		if (Lists.sameElements(this.entityList, entityList)) {
			return this;
		}
		return new EntityListExpr(this, entityList);
	}

	public ImmutableList<EntityExpr> getEntityList() {
		return entityList;
	}

	@Override
	public <R, P> R accept(EntityExprVisitor<R, P> v, P p) {
		return v.visitEntityListExpr(this, p);
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer("[");
		String sep = "";
		for(EntityExpr e : entityList){
			sb.append(sep);
			sep = ", ";
			sb.append(e);
		}
		sb.append("]");
		return sb.toString();
	}

	private ImmutableList<EntityExpr> entityList;

	@Override
	public void forEachChild(Consumer<? super IRNode> action) {
		entityList.forEach(action);
		getAttributes().forEach(action);
	}

	@Override
	public EntityListExpr withAttributes(List<ToolAttribute> attributes) {
		return (EntityListExpr) super.withAttributes(attributes);
	}

	@Override
	public EntityListExpr transformChildren(Transformation transformation) {
		return copy(
				transformation.mapChecked(EntityExpr.class, entityList)
		).withAttributes(transformation.mapChecked(ToolAttribute.class, getAttributes()));
	}
}
