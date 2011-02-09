package reconstructreader;


import java.util.ArrayList;

public class TagList implements Value
{
    private final ArrayList<TranslationTag> list;

    public TagList()
    {
        list = new ArrayList<TranslationTag>();
    }

    public void add(final TranslationTag tag)
    {
        list.add(tag);
    }

    public ArrayList<TranslationTag> getList()
    {
        return list;
    }

    public String toXML() {
        String xml = "";
        for (TranslationTag t : list)
        {
            xml += t.toXML();
            xml += "\n";
        }
        return xml;
    }
}
