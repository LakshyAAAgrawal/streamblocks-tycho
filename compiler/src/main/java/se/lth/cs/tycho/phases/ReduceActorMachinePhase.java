package se.lth.cs.tycho.phases;

import org.multij.Binding;
import org.multij.Module;
import org.multij.MultiJ;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.decl.Decl;
import se.lth.cs.tycho.ir.decl.EntityDecl;
import se.lth.cs.tycho.ir.entity.Entity;
import se.lth.cs.tycho.ir.entity.am.ActorMachine;
import se.lth.cs.tycho.ir.entity.am.ctrl.State;
import se.lth.cs.tycho.phases.reduction.MergeStates;
import se.lth.cs.tycho.phases.reduction.SingleInstructionState;
import se.lth.cs.tycho.phases.reduction.TransformedController;
import se.lth.cs.tycho.settings.IntegerSetting;
import se.lth.cs.tycho.settings.Setting;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReduceActorMachinePhase implements Phase {
	private static final Setting<Integer> amStateMergeIterations = new IntegerSetting() {
		@Override
		public String getKey() {
			return "am-state-merge-interations";
		}

		@Override
		public String getDescription() {
			return "Number of iterations to merge identical states in the actor machines.";
		}

		@Override
		public Integer defaultValue() {
			return 10; // As some point RVC_MPEG4_SP_Decoder needed 13 iterations to be fully reduced.
		}
	};

	@Override
	public List<Setting<?>> getPhaseSettings() {
		return Collections.singletonList(amStateMergeIterations);
	}

	@Override
	public String getDescription() {
		return "Reduces the actor machines to deterministic actor machines.";
	}

	@Override
	public CompilationTask execute(CompilationTask task, Context context) {
		int iterations = context.getConfiguration().get(amStateMergeIterations);
		List<Function<State, State>> transformations =
				Stream.concat(Stream.of(selectFirst), Stream.generate(MergeStates::new).limit(iterations))
				.collect(Collectors.toList());
		return task.transformChildren(MultiJ.from(ReduceActorMachine.class)
				.bind("transformations").to(new TransformationList(transformations)).instance());
	}

	public static class TransformationList {
		public final List<Function<State, State>> transformations;

		public TransformationList(List<Function<State, State>> transformations) {
			this.transformations = transformations;
		}
	}

	@Module
	interface ReduceActorMachine extends Function<IRNode, IRNode> {
		@Binding
		TransformationList transformations();

		@Override
		default IRNode apply(IRNode node) {
			return node.transformChildren(this);
		}

		default IRNode apply(Decl decl) {
			return decl;
		}

		default IRNode apply(EntityDecl decl) {
			return decl.transformChildren(this);
		}

		default IRNode apply(Entity entity) {
			return entity;
		}

		default IRNode apply(ActorMachine actorMachine) {
			return actorMachine.withController(TransformedController.from(actorMachine.controller(),
					transformations().transformations));
		}
	}

	private static final Function<State, State> selectFirst =
			state -> new SingleInstructionState(state.getInstructions().get(0));
}
