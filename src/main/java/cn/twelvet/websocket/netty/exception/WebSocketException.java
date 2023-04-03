package cn.twelvet.websocket.netty.exception;

/**
 * @author twelvet
 * @WebSite www.twelvet.cn
 * @Description: WebSocketException
 */
public class WebSocketException extends Exception {

    private static final long serialVersionUID = 1L;

    public WebSocketException(String message) {
        super(message);
    }

    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
    }
}
