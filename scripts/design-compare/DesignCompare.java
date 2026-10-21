// Compares approved design images with the app's screenshot test images (ADR-0020). Not a gate: it produces
// side-by-side images and a summary for the independent review and for the person who approves the screen.
//
// Usage: java DesignCompare.java <design dir> <screenshot dir> <output dir>
// A design image <name>.png is compared with the screenshot <name>.png. Design images without a screenshot are
// listed as missing. The design image is scaled to the screenshot's size before comparing.

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

public class DesignCompare {
    // A pixel counts as different when one channel differs by more than this (0-255), so anti-aliasing and
    // small font rendering differences are ignored.
    static final int CHANNEL_TOLERANCE = 40;
    static final int GAP = 16;

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Cách dùng: java DesignCompare.java <thư mục thiết kế> <thư mục ảnh chuẩn> <thư mục kết quả>");
            System.exit(2);
        }
        File designDir = new File(args[0]);
        File screenshotDir = new File(args[1]);
        File outputDir = new File(args[2]);
        File[] designs = designDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (designs == null || designs.length == 0) {
            System.err.println("Không có ảnh thiết kế .png trong " + designDir);
            System.exit(1);
        }
        Arrays.sort(designs);
        Files.createDirectories(outputDir.toPath());

        List<String> rows = new ArrayList<>();
        for (File design : designs) {
            File screenshot = new File(screenshotDir, design.getName());
            String name = design.getName().replaceFirst("\\.png$", "");
            if (!screenshot.isFile()) {
                rows.add("| " + name + " | thiếu ảnh chuẩn screenshot | |");
                continue;
            }
            BufferedImage actual = read(screenshot);
            BufferedImage expected = scale(read(design), actual.getWidth(), actual.getHeight());
            BufferedImage diff = new BufferedImage(actual.getWidth(), actual.getHeight(), BufferedImage.TYPE_INT_RGB);
            long different = 0;
            for (int y = 0; y < actual.getHeight(); y++) {
                for (int x = 0; x < actual.getWidth(); x++) {
                    int a = actual.getRGB(x, y);
                    int e = expected.getRGB(x, y);
                    if (differs(a, e)) {
                        different++;
                        diff.setRGB(x, y, 0xFF0000);
                    } else {
                        diff.setRGB(x, y, faded(a));
                    }
                }
            }
            double percent = 100.0 * different / ((long) actual.getWidth() * actual.getHeight());
            String output = name + "_compare.png";
            ImageIO.write(sideBySide(expected, actual, diff), "png", new File(outputDir, output));
            rows.add(String.format(Locale.ROOT, "| %s | %.1f%% | `%s` |", name, percent, output));
        }

        StringBuilder summary = new StringBuilder();
        summary.append("| Màn hình | Điểm ảnh khác | Ảnh so sánh (thiết kế, thật, khác biệt) |\n");
        summary.append("| --- | --- | --- |\n");
        rows.forEach(row -> summary.append(row).append('\n'));
        Files.writeString(new File(outputDir, "summary.md").toPath(), summary.toString());
        System.out.print(summary);
    }

    static BufferedImage read(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) throw new IOException("Không đọc được ảnh " + file);
        return image;
    }

    static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    static boolean differs(int a, int b) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs(((a >> shift) & 0xFF) - ((b >> shift) & 0xFF)) > CHANNEL_TOLERANCE) return true;
        }
        return false;
    }

    // Same pixel, lighter, so the red differences stand out.
    static int faded(int rgb) {
        int r = 255 - (255 - ((rgb >> 16) & 0xFF)) / 4;
        int g = 255 - (255 - ((rgb >> 8) & 0xFF)) / 4;
        int b = 255 - (255 - (rgb & 0xFF)) / 4;
        return (r << 16) | (g << 8) | b;
    }

    static BufferedImage sideBySide(BufferedImage... images) {
        int width = images[0].getWidth();
        int height = images[0].getHeight();
        BufferedImage out = new BufferedImage(width * images.length + GAP * (images.length - 1), height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        for (int i = 0; i < images.length; i++) g.drawImage(images[i], i * (width + GAP), 0, null);
        g.dispose();
        return out;
    }
}
