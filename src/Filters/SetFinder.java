package Filters;

import Interfaces.Interactive;
import Interfaces.PixelFilter;
import core.DImage;

import java.util.ArrayList;

public class SetFinder implements PixelFilter, Interactive {
    public static int colorThreshold;
    public static final int BLOB_MIN_SIZE = 5000;
    public static final double QUADRILATERAL_THRESHOLD = 0.5;

    @Override
    public DImage processImage(DImage img) {
        //img = scaleImageToWidth(img, 624);

        DImage original = new DImage(img.getWidth(), img.getHeight());
        original.setColorChannels(img.getRedChannel().clone(), img.getGreenChannel().clone(), img.getBlueChannel().clone());

        // Find the average brightness of the image
        double avgBrightness = 0;
        short[][] red = img.getRedChannel();
        short[][] green = img.getGreenChannel();
        short[][] blue = img.getBlueChannel();

        for (int i = 0; i < red.length; i++) {
            for (int j = 0; j < red[i].length; j++) {
                avgBrightness += red[i][j] + green[i][j] + blue[i][j];
            }
        }

        avgBrightness /= red.length * red[0].length * 3;

        System.out.println("Average brightness: " + avgBrightness);

        colorThreshold = (int) (avgBrightness * 0.37 + 124);

        boolean[][] thresholdMask;

        ArrayList<Blob> blobs;

        thresholdMask = ColorMaskFilter.processImageBoolean(img, colorThreshold);

        blobs = getBlobs(img, thresholdMask);

        highlightBlobs(img, blobs);

        ArrayList<Card> cards = new ArrayList<>();

        for (Blob blob : blobs) {
            cards.add(new Card(blob, original));
        }

        ArrayList<DImage> cardImages = new ArrayList<>();
        ArrayList<ArrayList<DImage>> matchedImages = new ArrayList<>();

        for (Card card : cards) {
            cardImages.add(card.getProjected());
            matchedImages.add(card.getMatchedImages(5));
        }

        DImage results = new DImage(cardImages.get(0).getWidth() * (1 + matchedImages.get(0).size()), cardImages.get(0).getHeight() * cardImages.size());
        for (int i = 0; i < cardImages.size(); i++) {
            addImageToImage(results, cardImages.get(i), i * cardImages.get(i).getHeight(), 0);
            for (int j = 0; j < matchedImages.get(i).size(); j++) {
                addImageToImage(results, matchedImages.get(i).get(j), i * cardImages.get(i).getHeight(), cardImages.get(i).getWidth() * (j + 1));
            }
        }

        DImage finalImage = new DImage(results.getWidth() + img.getWidth(), Math.max(results.getHeight(), img.getHeight()));

        addImageToImage(finalImage, results, 0, 0);
        addImageToImage(finalImage, img, 0, results.getWidth());

        return finalImage;
    }

    private static ArrayList<Blob> getBlobs(DImage img, boolean[][] thresholdMask) {
        ArrayList<Blob> blobs;
        // CardColor thresholdMask to true for pixels that are above the threshold
        thresholdMask = ColorMaskFilter.processImageBoolean(img, colorThreshold);

        // Create a list of blobs using a "flood fill" algorithm
        blobs = Blob.findBlobs(thresholdMask, BLOB_MIN_SIZE);

        // Create a list of cards by determining which blobs conform to a quadrilateral
        blobs.removeIf(blob -> !blob.isQuadrilateral(QUADRILATERAL_THRESHOLD));
        return blobs;
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

    public static void addImageToImage (DImage img, DImage img2, int r, int c) {
        short[][] red = img.getRedChannel();
        short[][] green = img.getGreenChannel();
        short[][] blue = img.getBlueChannel();

        short[][] red2 = img2.getRedChannel();
        short[][] green2 = img2.getGreenChannel();
        short[][] blue2 = img2.getBlueChannel();

        for (int i = 0; i < red2.length; i++) {
            for (int j = 0; j < red2[i].length; j++) {
                int row = r + i;
                int col = c + j;

                if (row >= 0 && row < red.length && col >= 0 && col < red[0].length) {
                    red[row][col] = red2[i][j];
                    green[row][col] = green2[i][j];
                    blue[row][col] = blue2[i][j];
                }
            }
        }

        img.setColorChannels(red, green, blue);
    }

    private DImage scaleImageToWidth (DImage img, int w) {
        double aspectRatio = (double) img.getHeight() / img.getWidth();
        int h = (int) (w * aspectRatio);
        return scaleImage(img, w, h);
    }

    private DImage scaleImage (DImage img, int w, int h) {
        DImage scaled = new DImage(w, h);
        short[][] red = new short[h][w];
        short[][] green = new short[h][w];
        short[][] blue = new short[h][w];

        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                int srcR = (int) ((double) r / h * img.getHeight());
                int srcC = (int) ((double) c / w * img.getWidth());

                red[r][c] = img.getRedChannel()[srcR][srcC];
                green[r][c] = img.getGreenChannel()[srcR][srcC];
                blue[r][c] = img.getBlueChannel()[srcR][srcC];

                System.out.println("r: " + r + " c: " + c + " srcR: " + srcR + " srcC: " + srcC);
            }
        }

        scaled.setRedChannel(red);
        scaled.setGreenChannel(green);
        scaled.setBlueChannel(blue);
        return scaled;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, DImage img) {

    }

    @Override
    public void keyPressed(char key) {
        System.out.println("Color threshold: " + colorThreshold);
        switch (key) {
            case 'm':
                colorThreshold += 5;
                break;
            case 'n':
                colorThreshold -= 5;
                break;
        }
    }
}

