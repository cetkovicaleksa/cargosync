package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.CONFLICT;

@ResponseStatus(CONFLICT)
public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(message);
    }
}
