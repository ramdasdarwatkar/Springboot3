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
        // Map initialized inside the method to avoid unnecessary memory usage
        Map<String, byte[]> fileSignatures = Map.of(
            "htm", "<!DOCTYPE html>".getBytes(),
            "doc", new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1}, // DOC Header
            "docx", new byte[]{'P', 'K', 0x03, 0x04}, // DOCX (ZIP format)
            "tiff", new byte[]{'M', 'M', 0x00, 0x2A}, // TIFF Header
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, // JPG Header
            "msg", new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1}, // MSG (same as DOC)
            "pdf", "%PDF-1.4\n%".getBytes(), // PDF Header
            "ppt", new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1}, // PPT Header
            "rtf", "{\\rtf1".getBytes(), // RTF Header
            "txt", new byte[0], // Empty for text files
            "xls", new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1}, // XLS Header
            "xlsx", new byte[]{'P', 'K', 0x03, 0x04}, // XLSX (ZIP format)
            "zip", new byte[]{'P', 'K', 0x03, 0x04}, // ZIP Header
            "mht", "<!DOCTYPE html>".getBytes() // MHT (HTML-like)
        );

        return fileSignatures.getOrDefault(extension, new byte[0]); // Default to empty content
    }

    public static void main(String[] args) {
        try {
            FileGenerator generator = new FileGenerator();
            generator.createEmptyFile("test.pdf");
            generator.createEmptyFile("test.doc");
            generator.createEmptyFile("test.xlsx");
            generator.createEmptyFile("test.msg");
            generator.createEmptyFile("test.unknown"); // Should create an empty file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
