package Filters;

import Interfaces.PixelFilter;
import core.DImage;

public class ColorMaskFilter implements PixelFilter {
    @Override
    public DImage processImage(DImage img) {
        return processImage(img, 200);
    }

    public DImage processImage(DImage img, int threshold) {
        short[][] gray = img.getBWPixelGrid();

        for (int i = 0; i < gray.length; i++) {
            for (int j = 0; j < gray[i].length; j++) {
                if (gray[i][j] > threshold) {
                    gray[i][j] = 255;
                } else {
                    gray[i][j] = 0;
                }
            }
        }

        img.setPixels(gray);
        return img;
    }

    public static boolean[][] processImageBoolean(DImage img, int threshold) {
        short[][] gray = img.getBWPixelGrid();
        boolean[][] mask = new boolean[gray.length][gray[0].length];

        for (int i = 0; i < gray.length; i++) {
            for (int j = 0; j < gray[i].length; j++) {
                mask[i][j] = gray[i][j] > threshold;
            }
        }

        return mask;
    }
}

