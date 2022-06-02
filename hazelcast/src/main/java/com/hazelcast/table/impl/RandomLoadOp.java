/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.table.impl;

import com.hazelcast.tpc.engine.iouring.StorageScheduler;
import com.hazelcast.tpc.requestservice.Op;
import com.hazelcast.tpc.requestservice.OpCodes;
import com.hazelcast.tpc.engine.iouring.IOUringEventloop;

import static com.hazelcast.tpc.engine.frame.Frame.OFFSET_REQ_CALL_ID;

public final class RandomLoadOp extends Op {

    private boolean loaded;

    public RandomLoadOp() {
        super(OpCodes.RANDOM_LOAD);
    }

    @Override
    public void clear() {
        super.clear();
        loaded = false;
    }

    public void handle_IORING_OP_READ(int res, int flags) {
        loaded = true;
        scheduler.schedule(this);
    }

    @Override
    public int run() throws Exception {
        if (!loaded) {
            IOUringEventloop eventloop = (IOUringEventloop) this.scheduler.getEventloop();
            eventloop.getStorageScheduler().scheduleLoad(StorageScheduler.dummyFile, 0, 100, this);
            return BLOCKED;
        } else {
            response.writeResponseHeader(partitionId, callId())
                    .writeComplete();

            return COMPLETED;
        }
    }
}