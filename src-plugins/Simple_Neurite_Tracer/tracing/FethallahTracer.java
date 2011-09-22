/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010, 2011 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import java.awt.Graphics;
import java.io.File;
import java.util.ArrayList;

public class FethallahTracer extends Thread implements SearchInterface {

    public FethallahTracer( File oofFile,
                            float start_x_image,
                            float start_y_image,
                            float start_z_image,
                            float end_x_image,
                            float end_y_image,
                            float end_z_image ) {

        this.oofFile = oofFile;
        this.start_x_image = start_x_image;
        this.start_y_image = start_y_image;
        this.start_z_image = start_z_image;
        this.end_x_image = end_x_image;
        this.end_y_image = end_y_image;
        this.end_z_image = end_z_image;
    }

    protected File oofFile;

    protected float start_x_image;
    protected float start_y_image;
    protected float start_z_image;

    protected float end_x_image;
    protected float end_y_image;
    protected float end_z_image;

    public Path getResult() {
        throw new RuntimeException("FethallahTracer:getResult: Not implemented yet...");
    }

	public void drawProgressOnSlice( int plane,
                                     int currentSliceInPlane,
                                     TracerCanvas canvas,
                                     Graphics g ) {
    }

    protected ArrayList< SearchProgressCallback > progressListeners;

	public void addProgressListener( SearchProgressCallback callback ) {
		progressListeners.add( callback );
	}

    public void requestStop() {
        // FIXME: should probably add a "stoppable" query method to SearchInterface
        throw new RuntimeException("FethallahTracer:requestStop: Not implemented yet...");
    }


	@Override
	public void run( ) {

        // Call the JNI here:



        reportFinished( true );
    }

	public void reportFinished( boolean success ) {
		for( SearchProgressCallback progress : progressListeners )
			progress.finished( this, success );
	}

            

}
