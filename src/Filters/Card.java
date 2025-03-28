package Filters;

import core.DImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Card {
    public static class DataBaseCard {
        public String number;
        public String shape;
        public String color;
        public String shading;
        public DImage image;
        public int score;

        public DataBaseCard(String number, String shape, String color, String shading, DImage image, int score) {
            this.number = number;
            this.shape = shape;
            this.color = color;
            this.shading = shading;
            this.image = image;
            this.score = score;
        }
    }

    public static short normalizeR = 245;
    public static short normalizeG = 245;
    public static short normalizeB = 245;

    public String[] numbers = {"one", "two", "three"};
    public String[] shapes = {"diamond", "squiggle", "oval"};
    public String[] colors = {"red", "green", "blue"};
    public String[] shadings = {"empty", "full", "partial"};

    private final Blob blob;
    private final DImage projected;

    private ArrayList<DataBaseCard> dataBaseCards;

    public Card(Blob blob, DImage img) {
        this.blob = blob;
        projected = blurImage(project(img, 75, 50), 2);
        analyze();
    }

    private void analyze() {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        AtomicReference<ArrayList<DataBaseCard>> dataBaseCards = new AtomicReference<>(new ArrayList<>());
        for (String number : numbers) {
            for (String shape : shapes) {
                for (String color : colors) {
                    for (String shading : shadings) {
                        executor.submit(() -> {
                            File dir = new File("images/cards/" + number + "/" + color + "/" + shape + "/" + shading);
                            File[] files = dir.listFiles();

                            if (files == null || files.length == 0) {
                                System.out.println("No images found for " + number + " " + shape + " " + color + " " + shading);
                                return;
                            }

                            int sum = 0, count = 0;
                            for (int i = 0; i < Math.min(10, files.length); i++) {
                                sum += (int) getScore(normalizeTo(new DImage(files[i].getPath()), new short[] { normalizeR, normalizeG, normalizeB }));
                                count++;
                            }

                            int score = (count > 0) ? sum / count : Integer.MAX_VALUE;

                            System.out.println("Score for " + number + " " + shape + " " + color + " " + shading + ": " + score);

                            synchronized (dataBaseCards) {
                                dataBaseCards.get().add(new DataBaseCard(shape, color, shading, number, new DImage(files[0].getPath()), score));
                            }
                        });
                    }
                }
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        dataBaseCards.get().sort(Comparator.comparingInt(a -> a.score));

        this.dataBaseCards = dataBaseCards.get();
    }

    private double getScore(DImage img) {
        DImage scaled = scaleImage(img, projected.getWidth(), projected.getHeight());

        // Use K-Neighbor distance to compare images
        double distScore = kNeighborDist(scaled);

        // trace the left edge of the both images and compare how far they are from each other
        int[] projectedLeftEdge = new int[projected.getHeight()];
        int[] scaledLeftEdge = new int[projected.getHeight()];

        int sum = 0;

        for (int r = 0; r < projected.getHeight(); r++) {
            for (int c = 0; c < projected.getWidth(); c++) {
                if (projected.getRedChannel()[r][c] < 200) {
                    projectedLeftEdge[r] = c;
                    sum += c;
                    break;
                }
            }
        }

        int avg = sum / projected.getHeight();

        for (int r = 0; r < projected.getHeight(); r++) {
            projectedLeftEdge[r] -= avg;
        }

        sum = 0;

        for (int r = 0; r < scaled.getHeight(); r++) {
            for (int c = 0; c < scaled.getWidth(); c++) {
                if (scaled.getRedChannel()[r][c] < 200) {
                    scaledLeftEdge[r] = c;
                    sum += c;
                    break;
                }
            }
        }

        avg = sum / projected.getHeight();

        for (int r = 0; r < projected.getHeight(); r++) {
            scaledLeftEdge[r] -= avg;
        }

        for (int r = 0; r < projected.getHeight(); r++) {
            distScore += Math.abs(projectedLeftEdge[r] - scaledLeftEdge[r]) * 3;
        }

        return distScore;
    }

    private double kNeighborDist(DImage scaled) {
        short[][] red = scaled.getRedChannel();
        short[][] green = scaled.getGreenChannel();
        short[][] blue = scaled.getBlueChannel();

        int sum = 0;
        for (int r = 0; r < projected.getHeight(); r++) {
            for (int c = 0; c < projected.getWidth(); c++) {
                double distR = Math.abs(projected.getRedChannel()[r][c] - red[r][c]);
                double distG = Math.abs(projected.getGreenChannel()[r][c] - green[r][c]);
                double distB = Math.abs(projected.getBlueChannel()[r][c] - blue[r][c]);

                double avgDist = (distR + distG + distB) / 3.0;

                sum += (int) (avgDist * avgDist);
            }
        }

        return Math.sqrt(sum);
    }

    private DImage blurImage(DImage img, int n) {
        // Use convolution to blur the image
        short[][] red = img.getRedChannel();
        short[][] green = img.getGreenChannel();
        short[][] blue = img.getBlueChannel();

        short[][] newRed = new short[red.length][red[0].length];
        short[][] newGreen = new short[green.length][green[0].length];
        short[][] newBlue = new short[blue.length][blue[0].length];

        for (int r = 0; r < red.length; r++) {
            for (int c = 0; c < red[r].length; c++) {
                int sumR = 0, sumG = 0, sumB = 0;
                int count = 0;

                for (int dr = -n; dr <= n; dr++) {
                    for (int dc = -n; dc <= n; dc++) {
                        int newR = r + dr;
                        int newC = c + dc;

                        if (newR >= 0 && newR < red.length && newC >= 0 && newC < red[r].length) {
                            sumR += red[newR][newC];
                            sumG += green[newR][newC];
                            sumB += blue[newR][newC];
                            count++;
                        }
                    }
                }

                newRed[r][c] = (short) (sumR / count);
                newGreen[r][c] = (short) (sumG / count);
                newBlue[r][c] = (short) (sumB / count);
            }
        }

        DImage blurred = new DImage(img.getWidth(), img.getHeight());
        blurred.setRedChannel(newRed);
        blurred.setGreenChannel(newGreen);
        blurred.setBlueChannel(newBlue);
        return blurred;
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
            }
        }

        scaled.setRedChannel(red);
        scaled.setGreenChannel(green);
        scaled.setBlueChannel(blue);
        return scaled;
    }

    public DImage project(DImage original, int w, int h) {
        DImage projected = new DImage(w, h);
        short[][] red = new short[h][w];
        short[][] green = new short[h][w];
        short[][] blue = new short[h][w];

        double avgHeight = (blob.bottomLeft.r - blob.topLeft.r + blob.bottomRight.r - blob.topRight.r) / 2.0;
        double avgWidth = (blob.topRight.c - blob.topLeft.c + blob.bottomRight.c - blob.bottomLeft.c) / 2.0;

        if (avgHeight > avgWidth) {
            // Rotate the blob 90 degrees
            Pixel[] temp = new Pixel[4];
            temp[0] = blob.topLeft;
            temp[1] = blob.topRight;
            temp[2] = blob.bottomRight;
            temp[3] = blob.bottomLeft;
            blob.topLeft = temp[3];
            blob.topRight = temp[0];
            blob.bottomRight = temp[1];
            blob.bottomLeft = temp[2];
        }

        Pixel[] corners = blob.getQuadrilateralCorners();

        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                double u = (double) c / (w - 1);
                double v = (double) r / (h - 1);

                int srcC = (int) ((1 - u) * (1 - v) * corners[0].c + u * (1 - v) * corners[1].c + (1 - u) * v * corners[2].c + u * v * corners[3].c);
                int srcR = (int) ((1 - u) * (1 - v) * corners[0].r + u * (1 - v) * corners[1].r + (1 - u) * v * corners[2].r + u * v * corners[3].r);

                srcR = Math.max(0, Math.min(srcR, original.getHeight() - 1));
                srcC = Math.max(0, Math.min(srcC, original.getWidth() - 1));

                red[r][c] = original.getRedChannel()[srcR][srcC];
                green[r][c] = original.getGreenChannel()[srcR][srcC];
                blue[r][c] = original.getBlueChannel()[srcR][srcC];
            }
        }

        projected.setRedChannel(red);
        projected.setGreenChannel(green);
        projected.setBlueChannel(blue);
        return projected;
    }

    public DImage getProjected () {
        return projected;
    }

    public ArrayList<DImage> getMatchedImages(int n) {
        ArrayList<DImage> images = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i >= dataBaseCards.size()) {
                break;
            }
            images.add(normalizeTo(scaleImage(dataBaseCards.get(i).image, projected.getWidth(), projected.getHeight()), new short[] { normalizeR, normalizeG, normalizeB }));
        }

        return images;
    }

    public static short[] computeAverageColor(DImage img) {
        short[][] red = img.getRedChannel();
        short[][] green = img.getGreenChannel();
        short[][] blue = img.getBlueChannel();

        int width = img.getWidth();
        int height = img.getHeight();
        long sumR = 0, sumG = 0, sumB = 0;
        int count = width * height;

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                sumR += red[r][c];
                sumG += green[r][c];
                sumB += blue[r][c];
            }
        }

        return new short[]{
                (short) (sumR / count),
                (short) (sumG / count),
                (short) (sumB / count)
        };
    }

    public static DImage normalizeTo(DImage img, short[] projectedAvg) {
        short[][] red = img.getRedChannel();
        short[][] green = img.getGreenChannel();
        short[][] blue = img.getBlueChannel();

        int width = img.getWidth();
        int height = img.getHeight();

        // Compute average color of database image
        short[] dbAvg = computeAverageColor(img);

        // Compute scale factors to match projected image's average color
        double scaleR = projectedAvg[0] / (double) dbAvg[0];
        double scaleG = projectedAvg[1] / (double) dbAvg[1];
        double scaleB = projectedAvg[2] / (double) dbAvg[2];

        // Normalize colors
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                red[r][c] = (short) Math.min(255, Math.max(0, red[r][c] * scaleR));
                green[r][c] = (short) Math.min(255, Math.max(0, green[r][c] * scaleG));
                blue[r][c] = (short) Math.min(255, Math.max(0, blue[r][c] * scaleB));
            }
        }

        DImage normalized = new DImage(width, height);
        normalized.setRedChannel(red);
        normalized.setGreenChannel(green);
        normalized.setBlueChannel(blue);
        return normalized;
    }

}
