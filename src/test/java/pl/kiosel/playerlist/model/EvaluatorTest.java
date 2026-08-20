package pl.kiosel.playerlist.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluatorTest {

	@Test
	void initializesAndEvaluatesConditionsOnTheCurrentJavaRuntime() {
		assertTrue(Evaluator.initialize());
		assertNotEquals("none", Evaluator.getEngineSource());
		assertTrue(Evaluator.evaluateCondition("true && !false"));

		Object sum = Evaluator.evaluate("1 + 1");
		assertTrue(sum instanceof Number);
		assertEquals(2, ((Number) sum).intValue());
	}
}
