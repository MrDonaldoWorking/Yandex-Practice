import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Parser {
    private static final int N = 37, SIZE = 100;
    private static final String[] BLACK = {"black", "dark", "not white"};
    private static final String[] WHITE = {"white", "+", "rtagte"};
    private static final char[] DELIMITERS = {' ', ',', '\n', '(', ')'};

    public static void main(String[] args) {
        BufferedImage matrix;
        try {
            matrix = ImageIO.read(new File("blandwh.png"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        BufferedImage result = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setColor(Color.red);
        g2d.fillRect(0, 0, result.getWidth(), result.getHeight());

        Tesseract tesseract = new Tesseract();
        try (FileWriter writer = new FileWriter(new File("cells.txt"))){
            tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");

            for (int i = 0; i < N; ++i) {
                writer.write("========= " + (i + 1) + " line =========\n");
                for (int j = 0; j < N; ++j) {
                    int x = i * SIZE, y = j * SIZE;
                    File cell = new File("pic/" + (i + 1) + "x" + (j + 1) + ".png");
                    // ImageIO.write(matrix.getSubimage(x, y, SIZE, SIZE), "png", cell);
                    String res = tesseract.doOCR(cell).toLowerCase();

                    boolean painted = false;
                    for (String s : BLACK) {
                        if (res.contains(s)) {
                            g2d.setColor(Color.black);
                            g2d.fillRect(x, y, SIZE, SIZE);
                            painted = true;
                            break;
                        }
                    }

                    for (String s : WHITE) {
                        if (res.contains(s) && !painted) {
                            g2d.setColor(Color.white);
                            g2d.fillRect(x, y, SIZE, SIZE);
                            painted = true;
                            break;
                        }
                    }

                    if (painted) {
                        continue;
                    }

                    boolean last_delim = false;
                    for (char c : DELIMITERS) {
                        if (res.charAt(res.length() - 1) == c) {
                            last_delim = true;
                            break;
                        }
                    }
                    if (!last_delim) {
                        res = res + '\n';
                    }

                    List<String> nums = new ArrayList<>();
                    StringBuilder curr = new StringBuilder();
                    for (int p = 0; p < res.length(); ++p) {
                        boolean isDelim = false;
                        for (char c : DELIMITERS) {
                            if (res.charAt(p) == c) {
                                isDelim = true;
                                if (!curr.toString().isEmpty()) {
                                    nums.add(curr.toString());
                                    curr = new StringBuilder();
                                }
                            }
                        }
                        if (!isDelim) {
                            curr.append(res.charAt(p));
                        }
                    }

                    if (nums.size() < 3) {
                        writer.write((i + 1) + "x" + (j + 1) + "\n");
                        writer.write(res + "\n");
                        continue;
                    }

                    try {
                        int r = Integer.parseInt(nums.get(0));
                        int g = Integer.parseInt(nums.get(1));
                        int b = Integer.parseInt(nums.get(2));
                        g2d.setColor(new Color(r, g, b));
                        g2d.fillRect(x, y, SIZE, SIZE);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        writer.write((i + 1) + "x" + (j + 1) + "\n");
                        writer.write(res + "\n");
                    }
                }
                System.out.println((i + 1) + " line done");
            }

            ImageIO.write(result, "png", new File("result.png"));
        } catch (TesseractException | IOException e) {
            e.printStackTrace();
            return;
        }

/*
        try (FileWriter writer = new FileWriter(new File("result.txt"))) {
            tesseract.setLanguage("jpn_vert");
            writer.write(tesseract.doOCR(new File("/home/donaldo/Downloads/[studio A (Inanaki Shiki)] Houshi-bu no Uragawa 04 (Yahari Ore no Seishun Love Come wa Machigatteiru ) [Digital]/2.jpg")));
        } catch (TesseractException | IOException e) {
            e.printStackTrace();
        }
*/
    }
}
