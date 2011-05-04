package Jacl;

/*
 * A dynamic Jacl interpreter plugin for ImageJ(C).
 * Copyright (C) 2011 Johannes Schindelin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation 
 * (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
*/

import common.AbstractInterpreter;

import fiji.ThreadLocalPrintStream;

import ij.IJ;

import java.io.PrintStream;

import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclString;

public class Jacl_Interpreter extends AbstractInterpreter {

	private Interp interp;

	synchronized public void run(String arg) {
		super.window.setTitle("Jacl Interpreter");
		super.run(arg);
		Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
		if (out != null)
			ThreadLocalPrintStream.setErrAndOut(out);
		interp = new Interp();
		println("Starting Jacl...");
		try {
			interp.setVar("argv0", TclString.newInstance(getClass().getName()), TCL.GLOBAL_ONLY);
			interp.setVar("tcl_interactive", TclString.newInstance("1"), TCL.GLOBAL_ONLY);
			importAll();
			println("Ready -- have fun.\n>>>");
		} catch (TclException e) {
			IJ.handleException(e);
		}
	}

	protected Object eval(final String text) throws Throwable {
		interp.eval(text);
		return interp.getResult();
	}

	protected String getImportStatement(String packageName, Iterable<String> classNames) {
		return "package require java\n";
	}

	protected String getLineCommentMark() {
		return "%";
	}
}
