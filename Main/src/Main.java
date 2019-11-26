import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import javax.servlet.http.*;

import java.sql.*;


import com.mpatric.mp3agic.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.*;

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
		if (args.length != 1) {
			throw new IllegalArgumentException("Specify a valid directory.");
		}

		Path path = Paths.get(args[0]); // Create Path object
		//Having trouble running from commandline. 
		// Path path = Paths.get("C:\\Users\\asanchez\\Desktop\\mp3s");

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
		try (Connection conn = DriverManager.getConnection("jdbc:h2:~/mydatabase;AUTO_SERVER=TRUE;INIT=runscript from './create.sql'")) {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO Songs (artist, year, album, title) VALUES (?, ?, ?, ?)");

			for(Song s : songs) {
				ps.setString(1, s.getArtist());
				ps.setString(2, s.getYear());
				ps.setString(3, s.getAlbum());
				ps.setString(4, s.getTitle());
				ps.addBatch();
			}

			int[] rows = ps.executeBatch();
			System.out.println("Number of rows inserted: " + rows.length);			
		}

		//Step 5: Start HTTP Server - Part 1
		//This is pretty new for us.. Going to embed Jetty into Java program.
		Server server = new Server(8080);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.setResourceBase(System.getProperty("java.io.tmpdir"));
		server.setHandler(context);

		context.addServlet(SongServlet.class, "/songs");
		server.start();

		// Check if program is running on a desktop, and if so, open up a browser on localhost:8080/songs
		if (Desktop.isDesktopSupported()) {
			Desktop.getDesktop().browse(new URI("http://localhost:8080/songs"));
		}
	}

	//Step 5 : Write a servlet - Part 2

	public static class SongServlet extends HttpServlet {

		//Method which responds to HTTP GET calls (from the browser) on the path your servlet is registered to.
		protected void doGet(HttpServletRequest req, HttpServletResponse resp ) throws IOException {
			StringBuilder builder = new StringBuilder();

			try (Connection conn = DriverManager.getConnection("jdbc:h2:~/mydatabase")){
				Statement stat = conn.createStatement();
				ResultSet rs = stat.executeQuery("SELECT * FROM Songs");

				while(rs.next()) {
					builder.append("<tr class=\"table\">")
					.append("<td>").append(rs.getString("year")).append("</td>")
					.append("<td>").append(rs.getString("artist")).append("</td>")
					.append("<td>").append(rs.getString("album")).append("</td>")
					.append("<td>").append(rs.getString("title")).append("</td>")
					.append("</tr>");

				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String string = "<html><h1>Your Songs</h1><table><tr><th>Year</th><th>Artist</th><th>Album</th><th>Title</th></tr>" + builder.toString() + "</table></html>";
			resp.getWriter().write(string);
		}
	}


	public static Connection getConnection() {


		return null; // DriverManager.getConnection();
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