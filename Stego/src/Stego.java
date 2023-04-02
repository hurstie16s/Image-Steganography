import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class Stego {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Pattern REGEX = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

    public static void main(String[] args) {
        while (true) {
            try {
                run();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void run() throws Exception{
        System.out.print(">");
        var input = SCANNER.nextLine();
        String[] args = getArgs(input);
        Command command;
        try {
            command = Command.valueOf(args[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'"+args[0]+"' is not a recognized command");
        }
        switch (command) {
            case CONFIG -> config();
            case ENCODE -> {
                try {
                    encode(args[1], args[2]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    throw new ArrayIndexOutOfBoundsException(e.getMessage());
                }
            }
            case DECODE -> {
                try {
                    decode(args[1]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new ArrayIndexOutOfBoundsException("Too few arguments for command '"+args[0]+"'");
                }
            }
        }
    }

    private static String[] getArgs(String argsString) {
        var args = new ArrayList<String>();
        var matcher = REGEX.matcher(argsString);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Add double-quoted string without the quotes
                args.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                // Add single-quoted string without the quotes
                args.add(matcher.group(2));
            } else {
                // Add unquoted word
                args.add(matcher.group());
            }
        }
        return args.toArray(new String[0]);
    }

    private static void encode(String inputFilePath, String imageFilePath) throws IOException {
        //ENCODE TestFile.txt "C:\Users\samue\OneDrive\Documents\Home\Git Repositories\Image-Steganography\Stego\TestImage.png"

        BufferedImage image;
        try {
            File file = new File(imageFilePath);
            FileImageInputStream stream = new FileImageInputStream(file);
            image = ImageIO.read(stream);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to read '"+imageFilePath+"'");
        }

        byte[] inputFile;
        try {
            inputFile = Files.readAllBytes(Paths.get(inputFilePath));
        } catch (IOException e) {
            throw new IOException("File '"+inputFilePath+"' could not be found");
        }

        if ((image.getHeight()*image.getWidth()*3) < inputFile.length) {
            throw new IOException("Image file too small to encode input file");
        }
        /*
        Need 3 pixels at a time
         */
        int width = 0;
        int height = 0;

        for (int i = 0; i < inputFile.length; i++) {
            byte b = inputFile[i];
            if (height + 3 >= image.getHeight()) {
                height = 0;
                width++;
            }
            String[] binary = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0').split("");

            //Define pixels;
            int[] vals = new int[9];

            var pix1 = new Color(image.getRGB(width, height), true);
            vals[0] = pix1.getRed();
            vals[1] = pix1.getGreen();
            vals[2] = pix1.getBlue();

            var pix2 = new Color(image.getRGB(width, height+1), true);
            vals[3] = pix2.getRed();
            vals[4] = pix2.getGreen();
            vals[5] = pix2.getBlue();

            var pix3 = new Color(image.getRGB(width, height+2), true);
            vals[6] = pix3.getRed();
            vals[7] = pix3.getGreen();
            vals[8] = pix3.getBlue();

            // Odd = 1, Even  = 0
            for (int j = 0; j<8; j++) {
                if (
                        (Objects.equals(binary[j], "1") && vals[j]%2==0)
                        || (Objects.equals(binary[j], "0") && vals[j]%2!=0)
                ) {
                    vals[j] -= 1;
                    if (vals[j]==-1){
                        vals[j] = 255;
                    } else if (vals[j]==256) {
                        vals[j] = 0;
                    }
                }
            }
            //Odd for more data to come, Even for finished
            if (i+1 == inputFile.length && vals[8]%2!=0) {
                vals[8] -= 1;
            }

            // Set new colours
            pix1 = new Color(vals[0], vals[1], vals[2]);
            pix2 = new Color(vals[3], vals[4], vals[5]);
            System.out.println(vals[6]);
            pix3 = new Color(vals[6], vals[7], vals[8]);

            image.setRGB(width, height, pix1.getRGB());
            image.setRGB(width, height+1, pix2.getRGB());
            image.setRGB(width, height+3, pix3.getRGB());

            // Increment height
            height++;
        }
        File file = new File(imageFilePath);
        String fileExtension = imageFilePath.substring(imageFilePath.lastIndexOf('.')+1);
        ImageIO.write(image, fileExtension, file);
        System.out.println("File encoded");
    }
    private static void decode(String imageFilePath) {}
    private static void config() {}

}