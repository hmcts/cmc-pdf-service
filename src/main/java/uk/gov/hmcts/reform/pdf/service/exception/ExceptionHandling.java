package uk.gov.hmcts.reform.pdf.service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import uk.gov.hmcts.reform.pdf.generator.exception.MalformedTemplateException;

@ControllerAdvice
public class ExceptionHandling {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandling.class);

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public void handleException(Exception exception) {
        LOGGER.error("Unhandled exception:", exception);
    }

    @ExceptionHandler({
        InvalidArgumentException.class,
        MalformedTemplateException.class,
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class,
        ServletRequestBindingException.class })
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public void handleMissingAndMalformedParametersValues(Exception exception) {
        LOGGER.error("Input parameters were missing/malformed:", exception);
    }
}
