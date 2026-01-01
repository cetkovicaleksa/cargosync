package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception;

public class BaseException extends RuntimeException {
    public BaseException(String message) {
        super(message);
    }
    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
