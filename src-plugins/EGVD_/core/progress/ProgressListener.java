package core.progress;

import java.util.EventListener;

public interface ProgressListener extends EventListener {

	public void progressOccurred(ProgressEvent event);
}
