package tw.com.softleader.dh.basic;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class VerifyException extends Exception {

	private final List<String> msgs;

	public VerifyException(final List<String> msgs) {
		super(msgs.stream().collect(Collectors.joining(",")));
		this.msgs = msgs;
	}

	public List<String> getMsgs() {
		return msgs;
	}

}
