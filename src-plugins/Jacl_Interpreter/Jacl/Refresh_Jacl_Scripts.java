package Jacl;

import common.RefreshScripts;

import fiji.ThreadLocalPrintStream;

import ij.IJ;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclString;

public class Refresh_Jacl_Scripts extends RefreshScripts {
	public void run(String arg) {
		setLanguageProperties(".tcl", "Jacl");
		setVerbose(false);
		super.run(arg);
	}

	/** Runs the script at path */
	public void runScript(String path) {
		try {
			if (!path.endsWith(".tcl") || !new File(path).exists()) {
				IJ.log("Not a Jacl script or not found: " + path);
				return;
			}
			// The stream will be closed by runScript(InputStream)
			runScript(new BufferedInputStream(new FileInputStream(new File(path))), path);
		} catch (Throwable error) {
			printError(error);
		}
	}

	/** Will consume and close the stream. */
	public void runScript(InputStream istream) {
		runScript(istream, null);
	}

	/** Will consume and close the stream. */
	public void runScript(InputStream istream, String sourceFileName) {
		try {
			Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
			if (out != null)
				ThreadLocalPrintStream.setErrAndOut(out);
			Interp interp = new Interp();
			Jacl_Interpreter jacl = new Jacl_Interpreter();
			try {
				interp.setVar("argv0", TclString.newInstance(sourceFileName), TCL.GLOBAL_ONLY);
				interp.setVar("tcl_interactive", TclString.newInstance("0"), TCL.GLOBAL_ONLY);
			} catch (TclException e) {
				printError(e);
			}
			interp.eval(jacl.getImportStatement());
			interp.eval(readStream(istream));
		} catch (Throwable error) {
			printError(error);
		}
	}

	protected String readStream(InputStream in) throws IOException {
		StringBuilder result = new StringBuilder();
		Reader reader = new InputStreamReader(in);
		char[] buffer = new char[16384];
		for (;;) {
			int len = reader.read(buffer);
			if (len < 0)
				break;
			result.append(buffer, 0, len);
		}
		reader.close();
		return result.toString();
	}
}
