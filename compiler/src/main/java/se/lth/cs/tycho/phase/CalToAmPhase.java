package se.lth.cs.tycho.phase;

import se.lth.cs.tycho.compiler.CompilationTask;
import se.lth.cs.tycho.compiler.Context;
import se.lth.cs.tycho.compiler.Transformations;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.attribute.ConstantEvaluator;
import se.lth.cs.tycho.transformation.cal2am.CalToAm;
import se.lth.cs.tycho.transformation.cal2am.KnowledgeRemoval;
import se.lth.cs.tycho.settings.Setting;

import java.util.List;

public class CalToAmPhase implements Phase {
	@Override
	public String getDescription() {
		return "Translates all Cal actors to actor machines";
	}

	@Override
	public CompilationTask execute(CompilationTask task, Context context) {
		return Transformations.transformEntityDecls(task, decl -> {
			if (decl.getEntity() instanceof CalActor) {
                CalToAm translator = new CalToAm((CalActor) decl.getEntity(), context.getConfiguration(), task.getModule(ConstantEvaluator.key));
				return decl.withEntity(translator.buildActorMachine());
			} else {
				return decl;
			}
		});
	}

	@Override
	public List<Setting<?>> getPhaseSettings() {
		return ImmutableList.of(
				KnowledgeRemoval.forgetOnExec,
				KnowledgeRemoval.forgetOnWait
		);
	}
}
