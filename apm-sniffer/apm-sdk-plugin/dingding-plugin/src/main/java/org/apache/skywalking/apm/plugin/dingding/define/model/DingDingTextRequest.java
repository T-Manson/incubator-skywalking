package org.apache.skywalking.apm.plugin.dingding.define.model;

public class DingDingTextRequest {

    private String msgtype;

    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }

    public TextModel getText() {
        return text;
    }

    private TextModel text;

    public String getMsgtype() {
        return msgtype;
    }

    public void setText(TextModel text) {
        this.text = text;
    }

    private AtModel at;

    public AtModel getAt() {
        return at;
    }

    public void setAt(AtModel at) {
        this.at = at;
    }

    public DingDingTextRequest() {
        this.msgtype = "text";
    }
}
