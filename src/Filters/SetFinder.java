package Filters;

import Interfaces.PixelFilter;
import core.DImage;

import java.util.ArrayList;

public class SetFinder implements PixelFilter {
    public static final int COLOR_THRESHOLD = 180;
    public static final int BLOB_MIN_SIZE = 200;
    public static final double QUADRILATERAL_THRESHOLD = 0.5;

    @Override
    public DImage processImage(DImage img) {
        DImage original = new DImage(img.getWidth(), img.getHeight());
        original.setColorChannels(img.getRedChannel().clone(), img.getGreenChannel().clone(), img.getBlueChannel().clone());

        // Color thresholdMask to pure white for cards and pure black for background
        new ColorMaskFilter().processImage(img, COLOR_THRESHOLD);
        boolean[][] thresholdMask = ColorMaskFilter.processImageBoolean(img, COLOR_THRESHOLD);

        // Create a list of blobs using a "flood fill" algorithm
        ArrayList<Blob> blobs = Blob.findBlobs(thresholdMask, BLOB_MIN_SIZE);

        // Create a list of cards by determining which blobs conform to a quadrilateral
        blobs.removeIf(blob -> !blob.isQuadrilateral(QUADRILATERAL_THRESHOLD));

        highlightBlobs(img, blobs);

        ArrayList<Card> cards = new ArrayList<>();

        for (Blob blob : blobs) {
            cards.add(new Card(blob, original, thresholdMask));
        }

        System.out.println("Number of cards: " + cards.size());
        for (Card card : cards) {
            System.out.println(card.getColor());
        }

        // For each card, determine it's color, number, shape, and fill

        // Find a valid set, if there is one, and highlight it on the image

        return img;
    }

    private static void highlightBlobs(DImage img, ArrayList<Blob> blobs) {
        for (Blob blob : blobs) {
            short[][] red = img.getRedChannel();
            short[][] green = img.getGreenChannel();
            short[][] blue = img.getBlueChannel();

            for (Pixel pixel : blob.getPixels()) {
                red[pixel.r][pixel.c] = 255;
                green[pixel.r][pixel.c] = 120;
                blue[pixel.r][pixel.c] = 120;
            }

            img.setColorChannels(red, green, blue);

            drawCircle(img, 5, blob.topLeft.r, blob.topLeft.c);
            drawCircle(img, 5, blob.topRight.r, blob.topRight.c);
            drawCircle(img, 5, blob.bottomLeft.r, blob.bottomLeft.c);
            drawCircle(img, 5, blob.bottomRight.r, blob.bottomRight.c);
        }
    }

    public static void drawCircle(DImage img, int radius, int r, int c) {
        short[][] red = img.getRedChannel();
        short[][] green = img.getGreenChannel();
        short[][] blue = img.getBlueChannel();

        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                if (i * i + j * j <= radius * radius) {
                    int row = r + i;
                    int col = c + j;

                    if (row >= 0 && row < red.length && col >= 0 && col < red[0].length) {
                        red[row][col] = 0;
                        green[row][col] = 0;
                        blue[row][col] = 255;
                    }
                }
            }
        }

        img.setColorChannels(red, green, blue);
    }
}

