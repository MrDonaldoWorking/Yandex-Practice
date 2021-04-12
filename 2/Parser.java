import javafx.css.Match;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.javacpp.*;

import org.bytedeco.opencv.opencv_core.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_highgui.*;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.abs;

public class Parser {
    private static final Character[] LETTERS = {'U', 'A', 'Y', 'B', 'G', 'I', 'X', 'T', 'W', 'R', 'O', 'M', 'V', 'D'};
    private static int SQUARE_SIZE, FRAME_SIZE;

    public static double compareImage(BufferedImage biA, BufferedImage biB) {
        DataBuffer dbA = biA.getData().getDataBuffer();
        DataBuffer dbB = biB.getData().getDataBuffer();

        if (dbA.getSize() != dbB.getSize()) {
            return 100;
        }

        double cnt = 0;
        for (int i = 0; i < dbA.getSize(); ++i) {
            if (dbA.getElem(i) != dbB.getElem(i)) {
                cnt += abs(dbA.getElem(i) - dbB.getElem(i));
            }
        }

        return cnt / dbA.getSize() / 255.0 * 100.0;
    }

    public static int videoToFrames(String path) {
        int frames = 0;
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path);
            grabber.start();
            BufferedImage prev = new BufferedImage(1, 1, BufferedImage.TYPE_INT_BGR), curr = null;
            for (int i = 0; i < grabber.getLengthInFrames(); ++i) {
                do {
                    curr = Java2DFrameUtils.toBufferedImage(grabber.grab());
                } while (curr == null);
                if (compareImage(prev, curr) >= 1) {
                    prev = curr;
                    ImageIO.write(curr, "png", new File("frames/frame-" + (++frames) + ".png"));
                }
            }
            grabber.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return frames;
    }

    private static class MatchArea {
        private final int x, y, width, height;
        private final double score;

        public MatchArea(int x, int y, int width, int height, double score) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.score = score;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        public int getY() {
            return y;
        }

        public int getX() {
            return x;
        }

        public double getScore() {
            return score;
        }
    }

    private static int cnt = 0;
    public static MatchArea templateMatching(String sourcePath, String templatePath) {
        // read in image default colors
        Mat sourceColor = imread(sourcePath);
        Mat sourceGrey = new Mat(sourceColor.size(), CV_8UC1);
        cvtColor(sourceColor, sourceGrey, COLOR_BGR2GRAY);
        // load in template in grey
        Mat template = imread(templatePath, IMREAD_GRAYSCALE);//int = 0
        // Size for the result image
        Size size = new Size(sourceGrey.cols() - template.cols() + 1, sourceGrey.rows() - template.rows() + 1);
        Mat result = new Mat(size, CV_32FC1);
        matchTemplate(sourceGrey, template, result, TM_CCORR_NORMED);

        DoublePointer minVal = new DoublePointer(1);
        DoublePointer maxVal = new DoublePointer(1);
        Point min = new Point();
        Point max = new Point();
        minMaxLoc(result, minVal, maxVal, min, max, null);
//        System.out.println("x = " + max.x() + ", y = " + max.y() + ", w = " + template.cols() + ", h = " + template.rows());
//        rectangle(sourceColor, new Rect(max.x(), max.y(), template.cols(), template.rows()),
//                randColor(), 2, 0, 0);

/*
        imshow("Original marked", sourceColor);
        imshow("Template", template);
        imshow("Results matrix", result);
*/
         // imwrite(String.format("%s%s%.2f%s", templatePath, ".res", maxVal.get(), ".png"), sourceColor);
/*
        waitKey(0);
        destroyAllWindows();
*/

        return new MatchArea(max.x(), max.y(), template.cols(), template.rows(), maxVal.get());
    }

    public static Scalar randColor() {
        int b, g, r;
        b = ThreadLocalRandom.current().nextInt(0, 255 + 1);
        g = ThreadLocalRandom.current().nextInt(0, 255 + 1);
        r = ThreadLocalRandom.current().nextInt(0, 255 + 1);
        return new Scalar(b, g, r, 0);
    }

    private static int[][] convertBufferedImageToMatrix(BufferedImage image) {
        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final boolean hasAlphaChannel = image.getAlphaRaster() != null;

        int[][] result = new int[height][width];
        if (hasAlphaChannel) {
            final int pixelLength = 4;
            for (int pixel = 0, row = 0, col = 0; pixel + 3 < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
                argb += ((int) pixels[pixel + 1] & 0xff); // blue
                argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
                result[row][col] = argb;
                if (++col == width) {
                    col = 0;
                    ++row;
                }
            }
        } else {
            final int pixelLength = 3;
            for (int pixel = 0, row = 0, col = 0; pixel + 2 < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += -16777216; // 255 alpha
                argb += ((int) pixels[pixel] & 0xff); // blue
                argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
                result[row][col] = argb;
                if (++col == width) {
                    col = 0;
                    ++row;
                }
            }
        }

        return result;
    }

    private static class MutableInt {
        int value = 1;

        public void increase() {
            ++value;
        }

        public int get() {
            return value;
        }
    }

    private static boolean isPartOfLetter(int background, int suspect) {
        int diffCnt = 0;
        while (background != 0 || suspect != 0) {
            if ((background % 2 + 2) % 2 != (suspect % 2 + 2) % 2) {
                ++diffCnt;
            }

            background /= 2;
            suspect /= 2;
        }

        return diffCnt >= 5;
    }

    public static int cropImage(String imagePath) {
        int background = 0;
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            int[][] pixels = convertBufferedImageToMatrix(image);
            Map<Integer, MutableInt> colors = new TreeMap<>();
            for (int[] currRow : pixels) {
                for (int currPixel : currRow) {
                    addIfAbsentOrIncrease(colors, currPixel);
                }
            }

/*
            if (colors.size() != 2) {
                System.out.println(imagePath + " has " + colors.size() + " colors");
                for (Map.Entry<Integer, MutableInt> entry : colors.entrySet()) {
                    System.out.println(Integer.toBinaryString(entry.getKey()) + " -> " + entry.getValue().get());
                }
            }
*/

            int maxValue = 0;
            for (Map.Entry<Integer, MutableInt> entry : colors.entrySet()) {
                if (maxValue < entry.getValue().get()) {
                    maxValue = entry.getValue().get();
                    background = entry.getKey();
                }
            }
//            System.out.println("Background");
//            System.out.println(Integer.toBinaryString(background));

            int minX = pixels.length, maxX = 0, minY = pixels.length, maxY = 0;
            for (int i = 0; i < pixels.length; ++i) {
                for (int j = 0; j < pixels.length; ++j) {
                    if (isPartOfLetter(background, pixels[i][j])) {
                        minX = Math.min(minX, j);
                        maxX = Math.max(maxX, j);
                        minY = Math.min(minY, i);
                        maxY = Math.max(maxY, i);
                    }
                }
            }

//            System.out.println(imagePath);
//            System.out.println(minX + " " + maxX + " " + minY + " " + maxY);
            BufferedImage cropped = image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
            ImageIO.write(cropped, "png", new File(imagePath + ".crop.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return background;
    }

    public static Mat scaleImage(String imagePath, double percent) {
        Mat sourceImage = imread(imagePath);

        int width = (int) Math.ceil(sourceImage.cols() * percent);
        int height = (int) Math.ceil(sourceImage.rows() * percent);

        Mat scaledImage = new Mat();
        Size sz = new Size(width, height);
        resize(sourceImage, scaledImage, sz, 0, 0, INTER_AREA);

//        imwrite(String.format("%s%s%.2f%s", imagePath, ".scaled", percent, ".png"), scaledImage);
        imwrite(imagePath + ".scaled.png", scaledImage);

        return scaledImage;
    }

    private static Character areaToLetter(MatchArea area) {
        if (area.getY() < 175) {
            return null;
        }

        int offset = (FRAME_SIZE - SQUARE_SIZE * 7) / 2;
        int row = (area.getY() > 204 ? 1 : 0);
        int col = (area.getX() - offset) / SQUARE_SIZE;
        return LETTERS[row * (LETTERS.length / 2) + col];
    }

    private static <T> void addIfAbsentOrIncrease(Map<T, MutableInt> map, T key) {
        MutableInt cnt = map.get(key);
        if (cnt == null) {
            map.put(key, new MutableInt());
        } else {
            cnt.increase();
        }
    }

    private static int findLetter(Character letter) {
        for (int i = 0; i < LETTERS.length; ++i) {
            if (LETTERS[i] == letter) {
                return i;
            }
        }

        return -1;
    }

    public static void main(String[] args) {
        // int frames = videoToFrames("task.mp4");
        System.out.println("Finished parsing the video");

        try {
            BufferedImage first = ImageIO.read(new File("frames/frame-1.png"));
            FRAME_SIZE = first.getWidth();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        SQUARE_SIZE = FRAME_SIZE / (LETTERS.length / 2);

        Map<Character, MutableInt> lettersCnt = new TreeMap<>();
        try (FileWriter writer = new FileWriter(new File("parser.log"))) {
            for (int i = 2; i <= 886; ++i) {
                writer.write(String.format("=====%d=====%n", i));
                if (i % 10 == 0) {
                    System.out.println("Running " + i + "th frame...");
                }
                int background = cropImage("frames/frame-" + i + ".png");

//                Map<Character, MutableInt> possibleLetters = new TreeMap<>();
                MatchArea bestArea = new MatchArea(0, 0, 0, 0, 0);
                char bestChar = 0;
                for (double percent = 1.00; percent > 0.00; percent -= 0.01) {
                    Mat scaledImage = scaleImage("frames/frame-" + i + ".png.crop.png", percent);
                    if (scaledImage.cols() <= 35) {
                        break;
                    }
                    for (char letter : LETTERS) {
                        MatchArea currArea = templateMatching(
                                "letters/" + letter + ".png.png", "frames/frame-" + i + ".png.crop.png.scaled.png");
                        if (bestArea.getScore() < currArea.getScore()) {
                            bestArea = currArea;
                            bestChar = letter;
                            writer.write(String.format("%c with %.2f%% matched %.3f%%%n", letter, percent * 100, currArea.getScore()));
                        }
                    }
                }

                if (bestArea.getScore() <= 0.85) {
                    writer.write(String.format("%s%n", "Check is needed!!!"));
                }

                if (bestChar == 0) {
                    System.err.println(i + "th frame couldn't parsed");
                } else {
                    addIfAbsentOrIncrease(lettersCnt, bestChar);
                }
                writer.write(String.format("%s%n", "Result = " + bestChar));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (Map.Entry<Character, MutableInt> entry : lettersCnt.entrySet()) {
            System.out.print(String.format("%c%d", entry.getKey(), entry.getValue().get()));
        }
    }
}
