package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class BadRequestException extends BaseException {
    public BadRequestException(String message) {
        super(message);
    }
}
