package my.project.qri3a.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ResourceNotValidException extends RuntimeException{
    public ResourceNotValidException(String message){
        super(message);
    }
}
