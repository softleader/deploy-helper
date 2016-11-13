package tw.com.softleader.dh.basic;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;

@SuppressWarnings("serial")
public class DeployVerifyException extends Exception {

	@Getter
	private final List<String> msgs;

	public DeployVerifyException(List<String> msgs) {
		super(msgs.stream().collect(Collectors.joining(",")));
		this.msgs = msgs;
	}

}
