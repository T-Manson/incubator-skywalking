package org.apache.skywalking.apm.plugin.dingding.model;

public class AtModel {

    private boolean isAtAll;

    public boolean isAtAll() {
        return isAtAll;
    }

    public void setAtAll(boolean atAll) {
        isAtAll = atAll;
    }

    public AtModel() {
        this.isAtAll = true;
    }
}
