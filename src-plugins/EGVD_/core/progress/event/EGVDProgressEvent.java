package core.progress.event;

import core.progress.ProgressEvent;

public class EGVDProgressEvent extends ProgressEvent {

	private static final long serialVersionUID = 1L;
	
	public int currentLevel;
	public int totalLevels;
	
	public EGVDProgressEvent(Object source) {
		super(source);
	}
}
