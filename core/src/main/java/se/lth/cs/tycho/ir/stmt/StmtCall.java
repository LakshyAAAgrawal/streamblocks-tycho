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

package se.lth.cs.tycho.ir.stmt;

import java.util.Objects;
import java.util.function.Consumer;

import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.ir.util.Lists;

public class StmtCall extends Statement {

	public <R, P> R accept(StatementVisitor<R, P> v, P p) {
		return v.visitStmtCall(this, p);
	}

	public StmtCall(Expression procedure, ImmutableList<Expression> args) {
		this(null, procedure, args);
	}

	private StmtCall(StmtCall original, Expression procedure, ImmutableList<Expression> args) {
		super(original);
		this.procedure = procedure;
		this.args = ImmutableList.copyOf(args);
	}

	public StmtCall copy(Expression procedure, ImmutableList<Expression> args) {
		if (Objects.equals(this.procedure, procedure) && Lists.equals(this.args, args)) {
			return this;
		}
		return new StmtCall(this, procedure, args);
	}

	public Expression getProcedure() {
		return procedure;
	}

	public ImmutableList<Expression> getArgs() {
		return args;
	}

	private Expression procedure;
	private ImmutableList<Expression> args;

	@Override
	public void forEachChild(Consumer<? super IRNode> action) {
		action.accept(procedure);
		args.forEach(action);
	}
}
