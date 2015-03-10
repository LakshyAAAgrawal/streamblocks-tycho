package se.lth.cs.tycho.transform.reduction.util;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import se.lth.cs.tycho.transform.util.Controller;
import se.lth.cs.tycho.transform.util.GenInstruction.Call;
import se.lth.cs.tycho.transform.util.GenInstruction.Test;
import se.lth.cs.tycho.transform.util.GenInstruction.Wait;

public class ShortestMinimumPathToCall<S> extends ShortestPath<Integer, S> {
	
	public ShortestMinimumPathToCall(Controller<S> controller) {
		super(controller, Comparator.naturalOrder());
	}

	@Override
	protected Optional<Integer> distanceFromWait(Wait<S> wait) {
		return Optional.empty();
	}

	@Override
	protected Optional<Integer> distanceFromCall(Call<S> call) {
		return Optional.of(0);
	}

	@Override
	protected Optional<Integer> distanceFromTest(Test<S> test) {
		Optional<Integer> s0 = distanceFromState(test.S0());
		Optional<Integer> s1 = distanceFromState(test.S1());
		return Stream.of(s0, s1)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.min(comparator)
				.map(d -> d + 1);
	}

}
