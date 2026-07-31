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
import java.util.concurrent.CompletionStage;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.data.DataAdapter;
import com.lightstreamer.adapter.data.ItemManager;
import com.lightstreamer.adapter.data.SubscriptionException;


public class NTDDataAdapter implements DataAdapter {

    @Override
    public CompletionStage<ItemManager> getItemManager(String itemName)
            throws SubscriptionException {
        throw new SubscriptionException("This Data Adapter does not support any subscription");
    }

    @Override
    public void init(Map<String, String> params, File configDir) throws InitException {
        // Nothing to do
    }

    @Override
    public void shutdown() {
    }
}