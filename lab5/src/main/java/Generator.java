import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Generator {
    public static void main(String[] args) throws Exception {
        File file = new File("books.txt");

        List<String> words =Arrays.asList("ala", "ma", "kota", "psa", "i", "w", "nad", "pod", "as", "dom", "dla", "na", "karuzela");
        Random random = new Random(System.currentTimeMillis());

        words.forEach(w1 -> words.forEach(w2 -> words.forEach(w3 -> {
            PrintWriter printWriter;
            try {
                printWriter = new PrintWriter(new FileOutputStream(file, true), true);
                printWriter.println(w1 + " " + w2 + " " + w3 + "::" + random.nextInt(333));
                printWriter.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        })));

    }
}
