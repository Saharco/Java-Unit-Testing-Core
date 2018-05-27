package OOP.Tests;

import OOP.Solution.OOPTestSummary;
import OOP.Solution.OOPUnitCore;
import org.junit.Test;

import static OOP.Tests.TestFunctions.*;

/**
 * Created by elran on 08/01/17.
 */
public class TestRunAll {
	@Test
	public void test() {
		launchTest(TestOrdered.class,11,2,3);
		launchTest(TestOrderedInher.class,13,2,3);
		launchTest(UnorderedTest.class,2,0,1);
		launchTest(UnorderedInheriting.class,4,1,2);
	}
}
