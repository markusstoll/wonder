package your.app.components;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

import er.extensions.components.ERXComponent;

public class Main extends ERXComponent {
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int ivNumber;
    private int ivIndex;

    public int maxCount = 30;

    public Main(WOContext context) {
		super(context);
	}

    public WOActionResults reload()
    {
        return null;
    }

    public WOActionResults reload2()
    {
        return null;
    }

    public int number()
    {
        return ivNumber*(ivIndex + 1);
    }

    public void setNumber(int number)
    {
        ivNumber = number/(ivIndex + 1);
    }

    /**
     * @return the index
     */
    public int index()
    {
        return ivIndex;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(int index)
    {
        ivIndex = index;
    }

    public NSArray<String> SucArray()
    {
        NSMutableArray<String> t = new NSMutableArray<String>();
        for(int i = 0; i< maxCount; i++)
        {
            if(i != ivIndex)
                t.add("Sinput" + i);
        }
        return t;
    }
    public NSArray<String> MucArray()
    {
        NSMutableArray<String> t = new NSMutableArray<String>();
        for(int i = 0; i< maxCount; i++)
        {
            if(i != ivIndex)
                t.add("Minput" + i);
        }
        return t;
    }

}
