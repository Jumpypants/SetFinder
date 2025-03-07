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
        // Color mask to pure white for cards and pure black for background
        new ColorMaskFilter().processImage(img, COLOR_THRESHOLD);
        boolean[][] mask = ColorMaskFilter.processImageBoolean(img, COLOR_THRESHOLD);

        // Create a list of blobs using a "flood fill" algorithm
        ArrayList<Blob> blobs = Blob.findBlobs(mask, BLOB_MIN_SIZE);
        System.out.println("Found " + blobs.size() + " blobs");
        System.out.println("Is first blob a quadrilateral? " + blobs.get(0).isQuadrilateral(QUADRILATERAL_THRESHOLD));

        short[][] red = img.getRedChannel();
        short[][] green = img.getGreenChannel();
        short[][] blue = img.getBlueChannel();

        for (Blob blob : blobs) {
            if (!blob.isQuadrilateral(QUADRILATERAL_THRESHOLD)) {
                continue;
            }
            for (Pose2d pixel : blob.getPixels()) {
                red[pixel.r][pixel.c] = 255;
                green[pixel.r][pixel.c] = 120;
                blue[pixel.r][pixel.c] = 120;
            }
        }

        img.setColorChannels(red, green, blue);

        // Create a list of cards by determining which blobs conform to a quadrilateral

        // For each card, determine it's color, number, shape, and fill

        // Find a valid set, if there is one, and highlight it on the image

        return img;
    }
}

