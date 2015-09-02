package se.lth.cs.tycho.ir.entity.xdf;

import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.entity.Entity;
import se.lth.cs.tycho.ir.entity.EntityVisitor;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.util.ImmutableList;

import java.util.function.Consumer;

public class XDFNetwork extends Entity {

	private final ImmutableList<XDFInstance> instances;
	private final ImmutableList<XDFConnection> connections;

	public XDFNetwork(ImmutableList<PortDecl> inputPorts, ImmutableList<PortDecl> outputPorts, ImmutableList<XDFInstance> instances, ImmutableList<XDFConnection> connections) {
		this(null, inputPorts, outputPorts, instances, connections);
	}

	public XDFNetwork(IRNode original, ImmutableList<PortDecl> inputPorts, ImmutableList<PortDecl> outputPorts,
			ImmutableList<XDFInstance> instances, ImmutableList<XDFConnection> connections) {
		super(original, inputPorts, outputPorts, ImmutableList.empty(), ImmutableList.empty());
		this.instances = instances;
		this.connections = connections;
	}

	public ImmutableList<XDFInstance> getInstances() {
		return instances;
	}

	public ImmutableList<XDFConnection> getConnections() {
		return connections;
	}

	@Override
	public <R, P> R accept(EntityVisitor<R, P> visitor, P param) {
		return visitor.visitXDFNetwork(this, param);
	}

	@Override
	public void forEachChild(Consumer<? super IRNode> action) {
		super.forEachChild(action);
		instances.forEach(action);
		connections.forEach(action);
	}
}
