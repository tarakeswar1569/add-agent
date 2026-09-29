package com.cgi.messagecentre.exception;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class GlobalExceptionHandler {
@ExceptionHandler(ApplicationException.class) public void handleApplicationException(ApplicationException ex) {}

}
