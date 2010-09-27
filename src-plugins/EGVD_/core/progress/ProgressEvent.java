package core.progress;

import java.util.EventObject;

public class ProgressEvent extends EventObject {

	private static final long serialVersionUID = 1L;

	public ProgressEvent(Object source) {
		super(source);
	}
}
