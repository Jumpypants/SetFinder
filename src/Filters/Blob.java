package Filters;

import java.util.ArrayList;

public class Blob {
    private final Pixel[] pixels;

    public Pixel topLeft;
    public Pixel topRight;
    public Pixel bottomLeft;
    public Pixel bottomRight;

    public Blob(Pixel[] pixels) {
        this.pixels = pixels;

        // Find the 4 corners of the blob
        topLeft = pixels[0];
        topRight = pixels[0];
        bottomLeft = pixels[0];
        bottomRight = pixels[0];

        for (Pixel pixel : pixels) {
            if (pixel.c + pixel.r < topLeft.c + topLeft.r) {
                topLeft = pixel;
            }
            if (pixel.c + pixel.r > bottomRight.c + bottomRight.r) {
                bottomRight = pixel;
            }
            if (pixel.c - pixel.r > topRight.c - topRight.r) {
                topRight = pixel;
            }
            if (pixel.c - pixel.r < bottomLeft.c - bottomLeft.r) {
                bottomLeft = pixel;
            }
        }
    }

    public boolean isQuadrilateral(double threshold) {
        // Check if the pixels mostly conform to the quadrilateral made by the corners

        // Count the number of pixels in the blob that are outside the quadrilateral
        int outsideCount = 0;
        for (Pixel pixel : pixels) {
            if (!isInsideQuadrilateral(pixel, topLeft, topRight, bottomLeft, bottomRight)) {
                outsideCount++;
            }
        }


        double score = getScore(outsideCount);

        return score >= threshold;
    }

    private double getScore(int outsideCount) {
        return 1 - (double) outsideCount / pixels.length;
    }

    private boolean isInsideQuadrilateral(Pixel pixel, Pixel topLeft, Pixel topRight, Pixel bottomLeft, Pixel bottomRight) {
        // Check if the pixel is inside the quadrilateral
        return isInsideTriangle(pixel, topLeft, topRight, bottomLeft)
                || isInsideTriangle(pixel, topRight, bottomRight, bottomLeft);
    }

    private boolean isInsideTriangle(Pixel pixel, Pixel a, Pixel b, Pixel c) {
        // Check if the pixel is inside the triangle
        double areaOfTriangle = Math.abs((a.c * (b.r - c.r) + b.c * (c.r - a.r) + c.c * (a.r - b.r)) / 2);
        double area1 = Math.abs((a.c * (b.r - pixel.r) + b.c * (pixel.r - a.r) + pixel.c * (a.r - b.r)) / 2);
        double area2 = Math.abs((a.c * (pixel.r - c.r) + pixel.c * (c.r - a.r) + c.c * (a.r - pixel.r)) / 2);
        double area3 = Math.abs((pixel.c * (b.r - c.r) + b.c * (c.r - pixel.r) + c.c * (pixel.r - b.r)) / 2);

        return areaOfTriangle == area1 + area2 + area3;
    }

    public static ArrayList<Blob> findBlobs(boolean[][] mask, int minSize) {
        ArrayList<Blob> blobs = new ArrayList<>();
        for (int r = 0; r < mask.length; r++) {
            for (int c = 0; c < mask[r].length; c++) {
                if (mask[r][c]) {
                    ArrayList<Pixel> pixels = floodFill(mask, r, c);
                    blobs.add(new Blob(pixels.toArray(new Pixel[0])));
                }
            }
        }

        // Remove blobs that are too small
        for (int i = blobs.size() - 1; i >= 0; i--) {
            if (blobs.get(i).pixels.length < minSize) {
                blobs.remove(i);
            }
        }

        return blobs;
    }

    private static ArrayList<Pixel> floodFill(boolean[][] mask, int r, int c) {
        ArrayList<Pixel> pixels = new ArrayList<>();
        ArrayList<Pixel> toCheck = new ArrayList<>();
        toCheck.add(new Pixel(r, c));

        while (!toCheck.isEmpty()) {
            Pixel pixel = toCheck.remove(0);

            if (pixel.r < 0 || pixel.r >= mask.length || pixel.c < 0 || pixel.c >= mask[0].length) {
                continue;
            }

            if (mask[pixel.r][pixel.c]) {
                pixels.add(pixel);
                mask[pixel.r][pixel.c] = false;

                toCheck.add(new Pixel(pixel.r - 1, pixel.c));
                toCheck.add(new Pixel(pixel.r + 1, pixel.c));
                toCheck.add(new Pixel(pixel.r, pixel.c - 1));
                toCheck.add(new Pixel(pixel.r, pixel.c + 1));
            }
        }

        return pixels;
    }

    public Pixel[] getPixels() {
        return pixels;
    }
}
