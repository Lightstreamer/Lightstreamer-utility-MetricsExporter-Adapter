package com.lightstreamer.utility;

import java.io.File;
import java.util.Map;

import javax.annotation.processing.Filer;

import com.lightstreamer.interfaces.data.DataProvider;
import com.lightstreamer.interfaces.data.DataProviderException;
import com.lightstreamer.interfaces.data.ItemEventListener;
import com.lightstreamer.interfaces.data.SubscriptionException;

public class NTDDataAdapter implements DataProvider {

    private volatile ItemEventListener listener;

    
    public void subscribe(String itemName, boolean needsIterator)
            throws SubscriptionException {
        // ...
        return ;
    }

    public void unsubscribe(String itemName) {
        // ...
        return ;
    }

    public boolean isSnapshotAvailable(String itemName) {
        return false;
    }

    @Override
    public void init(Map params, File configDir) throws DataProviderException {
        // ...

        return ;
    }

    @Override
    public void setListener(ItemEventListener listener) {
        this.listener = listener;
    }
    
}