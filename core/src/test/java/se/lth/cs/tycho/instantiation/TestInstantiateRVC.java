package se.lth.cs.tycho.instantiation;

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import se.lth.cs.tycho.instance.Instance;
import se.lth.cs.tycho.instance.net.Network;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.loader.DeclarationLoader;
import se.lth.cs.tycho.loader.FileSystemCalRepository;
import se.lth.cs.tycho.loader.FileSystemXdfRepository;
import se.lth.cs.tycho.messages.NullMessageReporter;
import se.lth.cs.tycho.transform.caltoam.ActorStates;
import se.lth.cs.tycho.transform.filter.SelectFirstInstruction;
import se.lth.cs.tycho.transform.util.StateHandler;

public class TestInstantiateRVC {

	private static final QID DECODER = QID.parse("org.sc29.wg11.mpeg4.part2.sp.RVC_decoder");
	private static final Path RVC_PATH = Paths.get("../../orc-apps/RVC/src");

	@Test
	public void testInstantiateRVC() {
		DeclarationLoader loader = new DeclarationLoader(new NullMessageReporter());
		loader.addRepository(new FileSystemXdfRepository(RVC_PATH));
		loader.addRepository(new FileSystemCalRepository(RVC_PATH));
		StateHandler.FilterConstructor<ActorStates.State> filter = SelectFirstInstruction<ActorStates.State>::new;
		Instantiator instantiator = new Instantiator(loader, Arrays.asList(filter));
		Instance net = instantiator.instantiate(DECODER, Collections.emptyList(), Collections.emptyList());
		assertTrue(net instanceof Network);
		Network network = (Network) net;
	}
}
