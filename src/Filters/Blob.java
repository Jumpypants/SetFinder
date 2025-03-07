package Filters;

import core.DImage;

import java.util.ArrayList;

public class Blob {
    private final Pose2d[] pixels;

    public Pose2d topLeft;
    public Pose2d topRight;
    public Pose2d bottomLeft;
    public Pose2d bottomRight;

    public Blob(Pose2d[] pixels) {
        this.pixels = pixels;

        // Find the 4 corners of the blob
        topLeft = pixels[0];
        topRight = pixels[0];
        bottomLeft = pixels[0];
        bottomRight = pixels[0];

        for (Pose2d pixel : pixels) {
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
        for (Pose2d pixel : pixels) {
            if (!isInsideQuadrilateral(pixel, topLeft, topRight, bottomLeft, bottomRight)) {
                outsideCount++;
            }
        }

        System.out.println("Outside count: " + outsideCount);

        double score = getScore(outsideCount);

        System.out.println("Score: " + score);

        return score >= threshold;
    }

    private double getScore(int outsideCount) {
        return 1 - (double) outsideCount / pixels.length;
    }

    private boolean isInsideQuadrilateral(Pose2d pixel, Pose2d topLeft, Pose2d topRight, Pose2d bottomLeft, Pose2d bottomRight) {
        // Check if the pixel is inside the quadrilateral
        return isInsideTriangle(pixel, topLeft, topRight, bottomLeft)
                || isInsideTriangle(pixel, topRight, bottomRight, bottomLeft);
    }

    private boolean isInsideTriangle(Pose2d pixel, Pose2d a, Pose2d b, Pose2d c) {
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
                    ArrayList<Pose2d> pixels = floodFill(mask, r, c);
                    blobs.add(new Blob(pixels.toArray(new Pose2d[0])));
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

    private static ArrayList<Pose2d> floodFill(boolean[][] mask, int r, int c) {
        ArrayList<Pose2d> pixels = new ArrayList<>();
        ArrayList<Pose2d> toCheck = new ArrayList<>();
        toCheck.add(new Pose2d(r, c));

        while (!toCheck.isEmpty()) {
            Pose2d pixel = toCheck.remove(0);

            if (pixel.r < 0 || pixel.r >= mask.length || pixel.c < 0 || pixel.c >= mask[0].length) {
                continue;
            }

            if (mask[pixel.r][pixel.c]) {
                pixels.add(pixel);
                mask[pixel.r][pixel.c] = false;

                toCheck.add(new Pose2d(pixel.r - 1, pixel.c));
                toCheck.add(new Pose2d(pixel.r + 1, pixel.c));
                toCheck.add(new Pose2d(pixel.r, pixel.c - 1));
                toCheck.add(new Pose2d(pixel.r, pixel.c + 1));
            }
        }

        return pixels;
    }

    public Pose2d[] getPixels() {
        return pixels;
    }
}
