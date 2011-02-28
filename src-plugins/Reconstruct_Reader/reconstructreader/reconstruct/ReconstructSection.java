package reconstructreader.reconstruct;

import org.w3c.dom.Document;

public class ReconstructSection {

    private final int index, oid;
    private final Document doc;

    public ReconstructSection(final int i, final int id, final Document d)
    {
        index = i;
        oid = id;
        doc = d;
    }

    public int getOID()
    {
        return oid;
    }

    public int getIndex()
    {
        return index;
    }

    public Document getDocument()
    {
        return doc;
    }

}
