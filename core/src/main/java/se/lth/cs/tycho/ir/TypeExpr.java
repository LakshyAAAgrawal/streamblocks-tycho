/* 
BEGINCOPYRIGHT X,UC
	
	Copyright (c) 2007, Xilinx Inc.
	Copyright (c) 2003, The Regents of the University of California
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
 */

package se.lth.cs.tycho.ir;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.ir.util.Lists;

/**
 * @author Christopher Chang <cbc@eecs.berkeley.edu>
 * @author Jorn W. Janneck <jorn.janneck@xilinx.com>
 */

public class TypeExpr extends AbstractIRNode implements Cloneable {
	public String getName() {
		return name;
	}

	public ImmutableList<Parameter<TypeExpr>> getTypeParameters() {
		return typeParameters;
	}

	public ImmutableList<Parameter<Expression>> getValueParameters() {
		return valueParameters;
	}

	public TypeExpr(String name) {
		this(null, name, null, null);
	}

	public TypeExpr(String name, ImmutableList<Parameter<TypeExpr>> typeParameters,
			ImmutableList<Parameter<Expression>> valueParameters) {
		this(null, name, typeParameters, valueParameters);
	}

	private TypeExpr(TypeExpr original, String name, ImmutableList<Parameter<TypeExpr>> typeParameters,
			ImmutableList<Parameter<Expression>> valueParameters) {
		super(original);
		this.name = name;
		this.typeParameters = ImmutableList.from(typeParameters);
		this.valueParameters = ImmutableList.from(valueParameters);
	}

	public TypeExpr copy(String name) {
		return copy(name, null, null);
	}

	public TypeExpr copy(String name, ImmutableList<Parameter<TypeExpr>> typeParameters,
			ImmutableList<Parameter<Expression>> valueParameters) {
		if (Objects.equals(this.name, name) && Lists.equals(this.typeParameters, typeParameters)
				&& Lists.equals(this.valueParameters, valueParameters)) {
			return this;
		}
		return new TypeExpr(this, name, typeParameters, valueParameters);
	}

	private final String name;
	private final ImmutableList<Parameter<TypeExpr>> typeParameters;
	private final ImmutableList<Parameter<Expression>> valueParameters;

	@Override
	public void forEachChild(Consumer<? super IRNode> action) {
		typeParameters.forEach(action);
		valueParameters.forEach(action);
	}

	@Override
	@SuppressWarnings("unchecked")
	public TypeExpr transformChildren(Function<? super IRNode, ? extends IRNode> transformation) {
		return copy(name, (ImmutableList) typeParameters.map(transformation), (ImmutableList) valueParameters.map(transformation));
	}

}
