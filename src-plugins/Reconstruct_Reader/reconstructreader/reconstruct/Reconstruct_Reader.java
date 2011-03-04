package reconstructreader.reconstruct;

import ij.IJ;
import ij.gui.MessageDialog;

import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ini.trakem2.Project;

public class Reconstruct_Reader implements PlugIn
{

    public void run(final String arg) {
        String fname;
        Translator translator;

        if (arg.equals(""))
        {
            OpenDialog od = new OpenDialog("Select Reconstruct ser File", "");
            fname = od.getDirectory() + od.getFileName();
        }
        else
        {
            fname = arg;
        }

        translator = new Translator(fname);
        System.out.println("done");

        if (translator.process())
        {
            String projectFileName = translator.writeTrakEM2();
            System.out.println(projectFileName);
            /*if (projectFileName != null)
            {
                Project.openFSProject(projectFileName);
            }*/
        }
        else
        {
            new MessageDialog(IJ.getInstance(), "Error", "Encountered an Error while translating");
        }

    }

}