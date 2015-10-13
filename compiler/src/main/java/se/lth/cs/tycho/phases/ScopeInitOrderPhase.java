package se.lth.cs.tycho.phases;

import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;

public class ScopeInitOrderPhase implements Phase {
	@Override
	public String getDescription() {
		return "Orders actor machine scopes in scope initialization order.";
	}

	@Override
	public CompilationTask execute(CompilationTask task, Context context) {
		return task;
	}
}
