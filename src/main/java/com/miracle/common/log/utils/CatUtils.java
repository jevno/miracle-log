package com.miracle.common.log.utils;

import com.dianping.cat.Cat;
import com.dianping.cat.message.spi.MessageTree;

public class CatUtils {

    public static String getCatRootMsgId()
    {
        String msgId = null;
        if(Cat.getManager() != null && Cat.getManager().getThreadLocalMessageTree() != null) {
            MessageTree tree = Cat.getManager().getThreadLocalMessageTree();
            if(tree.getRootMessageId() != null)
            {
                msgId = tree.getRootMessageId();
            }
            else
            {
                msgId = tree.getMessageId();
            }
        }
        return msgId;
    }

    public static String getCatCurrMsgId()
    {
        return Cat.getCurrentMessageId();
    }
}
