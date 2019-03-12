package nationGen.misc;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/**
 * Utility for uniformly interacting with the filesystem.  All files are taken relative to the user directory, which is
 * where the program was launched from.
 */
public final class FileUtil {
	
	private static final String DIRECTORY = System.getProperty("user.dir");
	
	private FileUtil() {
		throw new UnsupportedOperationException("Do not instantiate.");
	}
	
	/**
	 * Reads an image from a file path.
	 * @param filepath The path to an existing image file
	 * @return The buffered image object
	 * @throws IllegalArgumentException if the file can't be read into an image
	 */
	public static BufferedImage readImage(String filepath) {
		BufferedImage image;
		try {
			image = ImageIO.read(getExistingPath(filepath).toFile());
		} catch (IOException ioe) {
			throw new IllegalArgumentException("File '" + filepath + "' can't be read.", ioe);
		}
		if (image == null) {
			throw new IllegalArgumentException("File '" + filepath + "' can't be read into an image.");
		}
		return image;
	}
	
	/**
	 * Reads all the lines of a file into a List.
	 * @param filepath The path to an existing file
	 * @return The lines of the file
	 * @throws IllegalArgumentException if the file can't be read
	 */
	public static List<String> readLines(String filepath) {
		try {
			return Files.readAllLines(getExistingPath(filepath));
		} catch (IOException e) {
			throw new IllegalArgumentException("File '" + filepath + "' can't be read.", e);
		}
	}
	
	/**
	 * Writes a file using a list of lines.
	 * @param filepath The path to a file
	 * @param lines The lines of the file to write
	 * @throws IllegalArgumentException if the file couldn't be written
	 */
	public static void writeLines(String filepath, List<String> lines) {
		try {
			Files.write(getPath(filepath), lines);
		} catch (IOException e) {
			throw new IllegalArgumentException("Couldn't write to file '" + filepath + "'.", e);
		}
	}
	
	/**
	 * Creates a directory at the given path.
	 * @param path The path to create
	 * @throws IllegalStateException If the directory couldn't be created or already exists
	 */
	public static void createDirectory(String path) {
		final File file = getPath(path).toFile();
		if (!file.mkdirs()) {
			if (file.exists()) {
				throw new IllegalStateException("Directory '" + path + "' already exists.");
			}
			throw new IllegalStateException("Directory '" + path + "' could not be created.");
		}
	}
	
	/**
	 * Checks if a file at the specified filepath is missing.
	 * @param filename The path to the file to check
	 * @return {@code true} if the file does not exist
	 */
	public static boolean isMissing(String filename) {
		return !getPath(filename).toFile().exists();
	}
	
	/**
	 * Gets a {@link Path} of an existing file from the specified filepath.  The
	 * @param filepath The path to the file
	 * @return The {@link Path}
	 * @throws IllegalArgumentException if the path can't be found
	 */
	private static Path getExistingPath(String filepath) {
		Path path = getPath(filepath);
		if (!path.toFile().exists()) {
			throw new IllegalArgumentException("File '" + filepath + "' can't be found.");
		}
		return path;
	}
	
	/**
	 * Creates a {@link Path} from a filepath string.  Uses the user directory, aka where the program was launched from.
	 * @param filepath The path to the file
	 * @return The {@link Path}
	 */
	private static Path getPath(String filepath) {
		return Path.of(DIRECTORY, filepath);
	}
	
	/**
	 * Writes a PNG image file based on the given image and the path to where it should be saved.
	 * @param image The image to save
	 * @param filepath The path to the file where it should be saved
	 * @throws IllegalStateException if the image couldn't be written
	 */
	public static void writePng(BufferedImage image, String filepath) {
		try {
			ImageIO.write(image, "png", getPath(filepath).toFile());
		} catch (IOException e) {
			throw new IllegalStateException("Couldn't write PNG file to '" + filepath + "'.");
		}
	}
	
	/**
	 * Writes a TGA image file based on the given image and the path to where it should be saved.
	 * @param image The image to save
	 * @param filepath The path to the file where it should be saved
	 * @throws IllegalStateException if the image couldn't be written
	 */
	public static void writeTGA(BufferedImage image, String filepath) {
		try (DataOutputStream out =
				new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getPath(filepath).toFile())))) {
			
			// ID Length
			out.writeByte((byte) 0);
			
			// Color Map
			out.writeByte((byte) 0);
			
			// Image Type
			out.writeByte((byte) 2);
			
			// Color Map - Ignored
			out.writeShort(flipEndian((short) 0));
			out.writeShort(flipEndian((short) 0));
			out.writeByte((byte) 0);
			
			// X, Y Offset
			out.writeShort(flipEndian((short) 0));
			out.writeShort(flipEndian((short) 0));
			
			// Width, Height, Depth
			out.writeShort(flipEndian((short) image.getWidth()));
			out.writeShort(flipEndian((short) image.getHeight()));
			
			out.writeByte((byte) 24);
			out.writeByte((byte) 0);
			
			// Write out the image data
			for (int y = image.getHeight() - 1; y >= 0; y--) {
				
				for (int x = 0; x < image.getWidth(); x++) {
					
					Color c = new Color(image.getRGB(x, y));
					out.writeByte((byte) (c.getBlue()));
					out.writeByte((byte) (c.getGreen()));
					out.writeByte((byte) (c.getRed()));
				}
			}
			
		} catch (IOException e) {
			throw new IllegalStateException("Couldn't write TGA file to '" + filepath + "'.");
		}
	}
	
	/**
	 * Reverses the bytes of a short integer.
	 * @param signedShort The short data
	 * @return The short data with reversed bytes
	 */
	private static short flipEndian(short signedShort) {
		int input = signedShort & 0xFFFF;
		return (short) (input << 8 | (input & 0xFF00) >>> 8);
	}
}
