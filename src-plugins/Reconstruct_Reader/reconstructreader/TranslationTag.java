package reconstructreader;

import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Larry Lindsey
 *         To change this template use File | Settings | File Templates.
 */
public abstract class TranslationTag implements Value{

    private HashMap<String, Value> valueMap;
    private HashMap<String, TagList> tagMap;

    public TranslationTag()
    {
        valueMap = new HashMap<String, Value>();
        tagMap = new HashMap<String, TagList>();
    }

    protected abstract List<String> getOrderList();
    public abstract void addAttribute(final Attributes attr, final int index);

    public void addTranslationTag(String name, TranslationTag tag)
    {
        if (tagMap.containsKey(name))
        {
            TagList tagList = tagMap.get(name);
            tagList.add(tag);
        }
        else
        {
            TagList tagList = new TagList();
            tagList.add(tag);
            tagMap.put(name, tagList);
        }
    }

    public String toXML()
    {
        String xml = "";
        ArrayList<String> keyList = new ArrayList<String>(valueMap.keySet());



        //TODO fill it in;

        return xml;
    }

}
