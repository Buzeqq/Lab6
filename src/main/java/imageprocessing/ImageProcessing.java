package imageprocessing;

import imageprocessing.editor.ImageEditor;

import java.nio.file.Path;

public class ImageProcessing {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Please specify input and output directory!");
            return;
        }

        String inputStringPath = args[0];
        String outputStringPath = args[1];

        ImageEditor imageEditor = new ImageEditor(Path.of(inputStringPath), Path.of(outputStringPath));
        imageEditor.startEditing();
    }
}
