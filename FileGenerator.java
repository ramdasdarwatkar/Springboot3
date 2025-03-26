import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class FileGenerator {

    public void createEmptyFile(String filename) throws IOException {
        String extension = getFileExtension(filename).toLowerCase();
        byte[] content = getFileSignature(extension); // Get file-specific byte content

        File file = new File(filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }

        System.out.println("Created file: " + filename);
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1 || lastDot == filename.length() - 1) ? "" : filename.substring(lastDot + 1);
    }

    private byte[] getFileSignature(String extension) {
        // Initializing the map inside the method
        Map<String, byte[]> fileSignatures = Map.of(
            "png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "gif", new byte[]{'G', 'I', 'F', '8'},
            "pdf", "%PDF-1.4\n%".getBytes(),
            "zip", new byte[]{'P', 'K', 0x03, 0x04},
            "mp3", new byte[]{'I', 'D', '3'},
            "exe", new byte[]{'M', 'Z'}
        );

        return fileSignatures.getOrDefault(extension, new byte[0]); // Default to empty content
    }

    public static void main(String[] args) {
        try {
            FileGenerator generator = new FileGenerator();
            generator.createEmptyFile("example.pdf");
            generator.createEmptyFile("example.png");
            generator.createEmptyFile("example.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
