package reconstructreader.reconstruct;

import org.w3c.dom.Element;

public class ReconstructProfileList implements ContourSet
{

    private final int oid;
    private final Translator translator;
    private String name;

    public ReconstructProfileList(final Element e, final Translator t)
    {
        translator = t;
        oid = translator.nextOID();
    }

    public void addContour(final Element e) {

    }

    public void appendContourSectionWise(StringBuilder sb, int index) {

    }

    public boolean equals(final Object o)
    {
        if (o instanceof ReconstructProfileList)
        {
            ReconstructProfileList rpl = (ReconstructProfileList)o;
            return name.equals(rpl.name);
        }
        else if (o instanceof Element)
        {
            Element e = (Element)o;
            return name.equals(e.getAttribute("name"));
        }
        else
        {
            return false;
        }
    }
}
