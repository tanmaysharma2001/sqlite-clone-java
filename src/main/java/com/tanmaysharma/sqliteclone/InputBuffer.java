import java.util.Scanner;

public class InputBuffer {
    String buffer;
    int inputLength;

    public InputBuffer() {
        this.buffer = "";
        this.inputLength = 0;
    }

    public void readInput(Scanner scanner) {
        this.buffer = scanner.nextLine().trim();
        this.inputLength = buffer.length();
    }
}
