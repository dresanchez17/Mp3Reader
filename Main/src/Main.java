import java.io.*;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;
import java.util.stream.*;



import java.lang.*;
import java.sql.*;


import com.mpatric.mp3agic.*;
import org.eclipse.jetty.server.Server;

/**
 * This application takes in the directory path that holds mp3s
 * @author asanchez
 *
 */
public class Main {
	public static void main(String[] args) throws Exception	{

		/*Step 1.
		 * Validate input from user.
		 */
/*		if (args.length != 1) {
			throw new IllegalArgumentException("Specify a valid directory.");
		}
*/
		//Path path = Paths.get(args[0]); // Create Path object
		//Having trouble running from commandline. 
		Path path = Paths.get("C:\\Users\\asanchez\\Desktop\\mp3s");
		
		if (!Files.exists(path)) { // Verify path actually exists.
			throw new IllegalArgumentException("Specifed directory does not exist: " + path);
		}
		System.out.println("Path Exist!");

		/*Step 2.
		 * Read all files from directory
		 */
		List<Path> filePaths = new ArrayList<>();
		// will need to edit below stream to only pick up certain file types
		// Files.newDirectoryStream(path, "*.mp3")
		try(DirectoryStream<Path> paths = Files.newDirectoryStream(path, "*.mp3")) { // try + conditions; stream will need to be closed afterwards
			//Get the file names using lambda expressions.
			paths.forEach(p -> {
				System.out.println("Found: " + p.getFileName().toString());
				filePaths.add(p);
			});
		}
		
		//Step 3: Part 1. Using streams to construct list of Song objects created from MP3 data.
		List<Song> songs = filePaths.stream().map(p -> {
			try {
				Mp3File mp3 = new Mp3File(p.toString());
				ID3v2 id3 = mp3.getId3v2Tag();
				return new Song(id3.getArtist(), id3.getYear(), id3.getAlbum(), id3.getTitle());
			}
			catch (IOException | UnsupportedTagException | InvalidDataException e) {
				throw new IllegalStateException(e);
			}
		}).collect(Collectors.toList());
		
		//Step 4: Database. 
		// Can probably update this to Derby on home computer.
		try (Connection conn = DriverManager.getConnection("jdbc:h2:~/mydatabase;AUTO_SERVER=TRUE;INIT=runscript from './create.sql")) {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO Songs (artist, year, album, title) VALUES (?, ?, ?, ?)");
			
			for(Song s : songs) {
				ps.setString(1, s.getArtist());
				ps.setString(2, s.getYear());
				ps.setString(3, s.getAlbum());
				ps.setString(4, s.getTitle());
				ps.addBatch();
			}
			
			int[] rows = ps.executeBatch();
			System.out.println("Number of rows inserted: " + Arrays.toString(rows));			
		}
		
		//Step 5: Start HTTP Server - Part 1
		// This is pretty new for us.. Going to embed Jetty into Java program.
		Server server = new Server(8080);
		
	}
	/**
	 * Step 3: Part 2
	 * Class to represent a Song.
	 * @author asanchez
	 *
	 */
	public static class Song {
		private final String artist;
		private final String year;
		private final String album;
		private final String title;

		public Song (String artist, String year, String album, String title) {
			this.artist = artist;
			this.year = year;
			this.album = album;
			this.title = title;
		}

		public String getArtist() {
			return artist;
		}

		public String getYear() {
			return year;
		}

		public String getAlbum() {
			return album;
		}

		public String getTitle() {
			return title;
		}
	}
}
