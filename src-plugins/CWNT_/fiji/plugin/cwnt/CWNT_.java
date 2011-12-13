package fiji.plugin.cwnt;

import java.awt.Graphics2D;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;
import fiji.plugin.trackmate.visualization.hyperstack.TrackOverlay;

/** 
 * A collection of static methods used when pluging the Crown-Wearing Nuclei Tracker in the TrackMate plugin.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Dec 2011
 */
public class CWNT_ {

	/**
	 * Return a displayer where the tracks and the spots are only displayed in the current or nearing Z
	 * slices, to accommodate the large data spread in Z that is typically met by CWNT.
	 * @return
	 */
	public static HyperStackDisplayer createLocalSliceDisplayer() {
		
		return new HyperStackDisplayer() {
			
			@Override
			protected SpotOverlay createSpotOverlay() {
				
				return new SpotOverlay(model, model.getSettings().imp, displaySettings) {
					
					public void drawSpot(final Graphics2D g2d, final Spot spot, final float zslice, 
							final int xcorner, final int ycorner, final float magnification) {

						final float x = spot.getFeature(Spot.POSITION_X);
						final float y = spot.getFeature(Spot.POSITION_Y);
						final float z = spot.getFeature(Spot.POSITION_Z);
						final float dz2 = (z - zslice) * (z - zslice);
						float radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
						final float radius = spot.getFeature(Spot.RADIUS)*radiusRatio;
						if (dz2 >= radius * radius)
							return;

						// In pixel units
						final float xp = x / calibration[0];
						final float yp = y / calibration[1];
						// Scale to image zoom
						final float xs = (xp - xcorner) * magnification ;
						final float ys = (yp - ycorner) * magnification ;

						final float apparentRadius =  (float) (Math.sqrt(radius*radius - dz2) / calibration[0] * magnification); 
						g2d.drawOval(Math.round(xs - apparentRadius), Math.round(ys - apparentRadius), 
								Math.round(2 * apparentRadius), Math.round(2 * apparentRadius));		
						boolean spotNameVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES);
						if (spotNameVisible ) {
							String str = spot.toString();
							int xindent = fm.stringWidth(str) / 2;
							int yindent = fm.getAscent() / 2;
							g2d.drawString(spot.toString(), xs-xindent, ys+yindent);
						}
					}
				};
			}
			
			@Override
			protected TrackOverlay createTrackOverlay() {
			
				return new TrackOverlay(model, model.getSettings().imp, displaySettings) {
					
					@Override
					protected void drawEdge(Graphics2D g2d, Spot source, Spot target, int xcorner, int ycorner,	float magnification) {
						// Find x & y in physical coordinates
						final float x0i = source.getFeature(Spot.POSITION_X);
						final float y0i = source.getFeature(Spot.POSITION_Y);
						final float z0i = source.getFeature(Spot.POSITION_Z);
						final float x1i = target.getFeature(Spot.POSITION_X);
						final float y1i = target.getFeature(Spot.POSITION_Y);
						final float z1i = target.getFeature(Spot.POSITION_Z);
						// In pixel units
						final float x0p = x0i / calibration[0];
						final float y0p = y0i / calibration[1];
						final float z0p = z0i / calibration[2];
						final float x1p = x1i / calibration[0];
						final float y1p = y1i / calibration[1];
						final float z1p = z1i / calibration[2];
						// Check if we are nearing their plane
						final int czp = (imp.getSlice()-1);
						if (Math.abs(czp-z1p) > 3 && Math.abs(czp-z0p) > 3) {
							return;
						}
						// Scale to image zoom
						final float x0s = (x0p - xcorner) * magnification ;
						final float y0s = (y0p - ycorner) * magnification ;
						final float x1s = (x1p - xcorner) * magnification ;
						final float y1s = (y1p - ycorner) * magnification ;
						// Round
						final int x0 = Math.round(x0s);
						final int y0 = Math.round(y0s);
						final int x1 = Math.round(x1s);
						final int y1 = Math.round(y1s);

						g2d.drawLine(x0, y0, x1, y1);
					}
				};
				
			}
			
		};
		
	}

	

}
