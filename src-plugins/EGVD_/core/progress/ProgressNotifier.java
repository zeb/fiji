package core.progress;

import javax.swing.event.EventListenerList;

public abstract class ProgressNotifier {

	private EventListenerList listenerList;

	public ProgressNotifier() {
		listenerList = new EventListenerList();
	}

	public void addProgressListener(ProgressListener listener) {
		listenerList.add(ProgressListener.class, listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		listenerList.remove(ProgressListener.class, listener);
	}

	public void notifyProgress() {
		ProgressEvent event = createProgressEvent();
		fireMyEvent(event);
	}

	private void fireMyEvent(ProgressEvent event) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == ProgressListener.class) {
				((ProgressListener) listeners[i + 1]).progressOccurred(event);
			}
		}
	}

	public abstract ProgressEvent createProgressEvent();
}
