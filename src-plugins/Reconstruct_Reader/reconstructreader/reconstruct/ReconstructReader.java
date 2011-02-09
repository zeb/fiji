package reconstructreader.reconstruct;


import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class ReconstructReader implements PlugIn
{
    public void run(final String arg) {
        String file;
        if (arg.equals(""))
        {
            OpenDialog od = new OpenDialog("Select Reconstruct ser File", "");
            file = od.getDirectory() + od.getFileName();
        }
        else
        {
            file = arg;
        }


    }
}
