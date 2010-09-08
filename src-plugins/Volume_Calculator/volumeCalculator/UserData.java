
package volumeCalculator;

/**
 * A structure that contains the Graphic information and the color index
 * of a edge or node.
 * 
 * @author pcmarks
 */
class UserData {

    public static final int INITIAL_COLOR_INDEX = 99;

    public UserData() {
        this(null, INITIAL_COLOR_INDEX);
    }
    
    public UserData(Object graphInfo) {
        this(graphInfo,INITIAL_COLOR_INDEX);
    }

    public UserData(Object graphInfo, int colorIndex) {
        this.graphInfo = graphInfo;
        this.colorIndex = colorIndex;
    }

    protected Object graphInfo;

    /**
     * Get the value of graphInfo
     *
     * @return the value of graphInfo
     */
    public Object getGraphInfo() {
        return graphInfo;
    }

    /**
     * Set the value of graphInfo
     *
     * @param graphInfo new value of graphInfo
     */
    public void setGraphInfo(Object graphInfo) {
        this.graphInfo = graphInfo;
    }
    protected int colorIndex;

    /**
     * Get the value of colorIndex
     *
     * @return the value of colorIndex
     */
    public int getColorIndex() {
        return colorIndex;
    }

    /**
     * Set the value of colorIndex
     *
     * @param colorIndex new value of colorIndex
     */
    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    /**
     * Construct a printable version of an instance.
     *
     */
    @Override
     public String toString() {
         return graphInfo +"/"+colorIndex;
     }
}
