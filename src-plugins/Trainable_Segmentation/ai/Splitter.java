package ai;
/**
 *
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), 
 * 			Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import java.io.Serializable;
import java.util.ArrayList;

import weka.core.Instance;
import weka.core.Instances;

public class Splitter implements Serializable
{
	
	/** Serial version ID */
	private static final long serialVersionUID = 52652189902462L;
	/** split function template */
	private final SplitFunction template;

	/**
	 * Construct a split function producer
	 * 
	 * @param sfn split function template
	 */
	public Splitter(SplitFunction sfn)
	{
		this.template = sfn;
	}

	/**
	 * Calculate split function based on the input data
	 * @return split function
	 */
	public SplitFunction getSplitFunction(
			final Instance[] ins,
			final int numAttributes,
			final int numClasses,
			final int classIndex)
	{
			final SplitFunction sf = template.newInstance();
			sf.init(ins, numAttributes, numClasses, classIndex);
			return sf;
	}

	/**
	 * Calculate split function based on the input data
	 * @param ins The instances array to draw from.
	 * @param indices The subset of indices to use from the {@param ins} instances array.
	 * @return split function
	 */
	public SplitFunction getSplitFunction(
			final Instance[] ins,
			final ArrayList<Integer> indices,
			final int numAttributes,
			final int numClasses,
			final int classIndex)
	{
		// Shorten ins to ins2
		final Instance[] ins2 = new Instance[indices.size()];
		for (int i=0; i<ins2.length; i++) {
			ins2[i] = ins[indices.get(i)];
		}
		return getSplitFunction(ins2, numAttributes, numClasses, classIndex);
	}
}
