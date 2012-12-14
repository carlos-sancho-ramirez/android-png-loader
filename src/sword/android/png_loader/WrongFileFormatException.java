package sword.android.png_loader;

public class WrongFileFormatException extends Exception {
    private static final long serialVersionUID = 1423402242133694839L;

    public WrongFileFormatException(String message) {
        super(message);
    }

    public WrongFileFormatException() {
        super();
    }
}
