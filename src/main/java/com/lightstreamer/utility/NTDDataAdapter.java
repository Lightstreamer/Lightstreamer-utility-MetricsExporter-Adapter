/*
 *  Copyright (c) Lightstreamer Srl
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
        throw new SubscriptionException("This Data Adapter not support any subscription");
    }

    public void unsubscribe(String itemName) {
        // Should never happen
        return ;
    }

    public boolean isSnapshotAvailable(String itemName) {
        return false;
    }

    @Override
    public void init(Map params, File configDir) throws DataProviderException {
        // Nothing to do

        return ;
    }

    @Override
    public void setListener(ItemEventListener listener) {
        this.listener = listener;
    }
    
}