package pl.kiosel.playerlist.command.fakesubcommand;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditSubCommandTest {

	@Test
	void joinsTheCompleteMultiWordPlaceholderValue() {
		String[] args = {"FakePlayer", "addplaceholder", "title", "hello", "beautiful", "world"};

		assertEquals("hello beautiful world", EditSubCommand.joinArguments(args, 3));
	}
}
