package org.coolreader.crengine.reader;

import org.coolreader.crengine.Engine;

public abstract class Task implements Engine.EngineTask {

        public void done() {
            // override to do something useful
        }

        public void fail(Exception e) {
            // do nothing, just log exception
            // override to do custom action
        }
    }
