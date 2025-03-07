package Filters;

import core.DImage;
public class Card {
    public enum Color {
        RED, GREEN, PURPLE
    }
    public enum Shape {
        DIAMOND, OVAL, SQUIGGLE
    }
    public enum Fill {
        EMPTY, SHADED, SOLID
    }

    private final Blob blob;
    private Color color;
    private int number;
    private Shape shape;
    private Fill fill;

    public Card (Blob blob, DImage img, boolean[][] thresholdMask) {
        this.blob = blob;
        color = findColor(img, thresholdMask);
        number = findNumber();
        fill = findFill();
        shape = findShape();
    }

    private Shape findShape() {
        return null;
    }

    private Fill findFill() {
        return null;
    }

    private int findNumber() {
        return 0;
    }

    private Color findColor(DImage img, boolean[][] thresholdMask) {
        short[][] red = img.getRedChannel();
        short[][] green = img.getGreenChannel();
        short[][] blue = img.getBlueChannel();

        double sumRed = 0;
        double sumGreen = 0;
        double sumBlue = 0;

        for (Pixel pixel : blob.getPixels()) {
            // Skip pixels that are white
            if (thresholdMask[pixel.r][pixel.c]) {
                continue;
            }

            sumRed += red[pixel.r][pixel.c];
            sumGreen += green[pixel.r][pixel.c];
            sumBlue += blue[pixel.r][pixel.c];
        }

        double avgRed = sumRed / blob.getPixels().length;
        double avgGreen = sumGreen / blob.getPixels().length;
        double avgBlue = sumBlue / blob.getPixels().length;

        System.out.println("Red: " + avgRed);
        System.out.println("Green: " + avgGreen);
        System.out.println("Blue: " + avgBlue);
        System.out.println();

        if (avgRed > avgGreen && avgRed > avgBlue) {
            return Color.RED;
        } else if (avgGreen > avgRed && avgGreen > avgBlue) {
            return Color.GREEN;
        } else {
            return Color.PURPLE;
        }
    }

    public Color getColor() {
        return color;
    }
}
