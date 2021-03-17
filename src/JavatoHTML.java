import java.io.File;
import java.util.Scanner;

public class JavatoHTML {
    public static void main(String[] args) throws Exception {
        JavatoHTMLServer converter = new JavatoHTMLServer();
        Scanner input = new Scanner(System.in);
        System.out.print("enter file path: ");
        String filename = input.nextLine();
        System.out.print("enter output file path: ");
        converter.setOutputFileName(input.nextLine());

        File file = new File(filename);
        if (file.exists()) {
            System.out.println(file.getName() + " has been converted to HTML file ");
            converter.convertToHTML(file);
        }
        else {
            System.out.println("File " + filename + " does not exist");
            System.exit(0);
        }
    }
}
