package akka;

import java.io.Serializable;

public class ResponseMessage implements Serializable{
    private final String message;

    public ResponseMessage(String message){
        this.message=message;
    }

    public String getMessage() {
        return message;
    }
}
