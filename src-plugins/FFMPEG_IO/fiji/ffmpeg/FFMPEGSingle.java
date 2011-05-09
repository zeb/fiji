package fiji.ffmpeg;

/*
 * Base class to handle loading the consolidated FFMPEG library.
 */

public class FFMPEGSingle extends JNALibraryLoader {
	protected interface FFMPEG extends AVUTIL, AVDEVICE, AVCODEC, AVFORMAT, SWSCALE { }
	protected static FFMPEG AVUTIL;
	protected static FFMPEG AVDEVICE;
	protected static FFMPEG AVCODEC;
	protected static FFMPEG AVFORMAT;
	protected static FFMPEG SWSCALE;

	public boolean loadFFMPEG() {
		if (AVFORMAT != null)
			return true;

		try {
			AVDEVICE = AVCODEC = AVFORMAT = SWSCALE = AVUTIL =
				(FFMPEG)loadLibrary("ffmpeg", -1, FFMPEG.class);
		} catch (UnsatisfiedLinkError e) {
			showException(e);
			return false;
		}
		return true;
	}
}
