package reconstructreader.reconstruct;


import org.xml.sax.Attributes;
import reconstructreader.TranslationTag;

import java.util.List;

public class SerFile {

    public class Series extends TranslationTag
    {

        @Override
        protected List<String> getOrderList() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void addAttribute(Attributes attr, int index) {

        }
    }

    private TranslationTag series;

    public SerFile()
    {
        series = new
    }

}
