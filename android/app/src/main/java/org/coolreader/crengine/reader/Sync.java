package org.coolreader.crengine.reader;

public class Sync<T> extends Object {
        private volatile T result = null;
        private volatile boolean completed = false;

        public void set(T res) {
            result = res;
            completed = true;
            synchronized (this) {
                notify();
            }
        }

        public T get() {
            while (!completed) {
                try {
                    synchronized (this) {
                        if (!completed)
                            wait();
                    }
                } catch (InterruptedException e) {
                    // ignore
                } catch (Exception e) {
                    // ignore
                }
            }
            return result;
        }
    }
