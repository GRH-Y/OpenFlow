package flow.rtsp;

public class RtspRequest {

    private String seq = null;
    private String method = null;
    private String url = null;
    private String[] content = null;

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setContent(String[] content) {
        this.content = content;
    }

    public String getSeq() {
        return seq;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public String[] getContent() {
        return content;
    }
}
