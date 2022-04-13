package imageprocessing.editor;

import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImageEditor {
    final private Path inputDirectory;
    final private Path outputDirectory;
    final private static int FORK_POOL_LIMIT = 7;

    public ImageEditor(Path inputDirectory, Path outputDirectory) {
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
    }

    private Pair<Path, BufferedImage> getImageFromPath(Path imagePath) {
        try {
            return Pair.of(imagePath.getFileName(), ImageIO.read(imagePath.toFile()));
        } catch (IOException ignore) {
            return null;
        }
    }

    public void startEditing() {
        List<Path> inputFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(inputDirectory)) {
            inputFiles = stream.collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (int i = 1; i <= FORK_POOL_LIMIT; i++) {
            ForkJoinPool forkJoinPool = new ForkJoinPool(i);
            long time = System.currentTimeMillis();

            List<Path> finalInputFiles = inputFiles;
            try {
                forkJoinPool.submit(() -> {
                    parallelEditingStream(finalInputFiles);
                }).get();
                System.out.printf("Size of fork pool %d, time: %d\n", i, System.currentTimeMillis() - time);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                break;
            } finally {
                forkJoinPool.shutdown();
            }
        }
    }

    private void parallelEditingStream(List<Path> inputFiles) {
        Stream<Path> inputPathStream = inputFiles.stream().parallel();

        Stream<Pair<Path, BufferedImage>> newImagesStream = inputPathStream
                .map(this::getImageFromPath)
                .filter(Objects::nonNull)
                .parallel();

        Stream<Pair<Path, BufferedImage>> newImageAfterEditingStream = newImagesStream.map(pair -> {
            BufferedImage original = pair.getRight();
            BufferedImage newImage = new BufferedImage(original.getWidth(),
                    original.getHeight(),
                    original.getType());

            for (int x = 0; x < original.getWidth(); x++) {
                for (int y = 0; y < original.getHeight(); y++) {
                    int rgb = original.getRGB(x, y);
                    Color color = new Color(rgb);
                    int red = color.getRed();
                    int blue = color.getBlue();
                    int green = color.getGreen();
                    Color outColor = new Color(red, blue, green);
                    int outRgb = outColor.getRGB();
                    newImage.setRGB(x, y, outRgb);
                }
            }

            return Pair.of(pair.getLeft(), newImage);
        }).parallel();

        newImageAfterEditingStream.forEach(pair -> {
            File outputFile = outputDirectory.resolve(pair.getLeft()).toFile();
            try {
                ImageIO.write(pair.getRight(), "jpg", outputFile);
            } catch (IOException ignore) {}
        });
    }
}
