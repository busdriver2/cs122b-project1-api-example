import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;


// Declaring a WebServlet called StarsServlet, which maps to url "/api/stars"
@WebServlet(name = "StarsServlet", urlPatterns = "/api/stars")
public class StarsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {

            // Declare our statement
            Statement statement = conn.createStatement();

            String query = "SELECT\n" +
                    "\tm.id,\n" +
                    "    m.title,\n" +
                    "    m.year,\n" +
                    "    m.director,\n" +
                    "    substring_index(group_concat(DISTINCT g.name ORDER BY g.id SEPARATOR ', '), ',', 3) as genres,\n" +
                    "\tsubstring_index(group_concat(DISTINCT s.name ORDER BY s.id SEPARATOR ', '), ',', 3) as stars,\n" +
                    "\tsubstring_index(GROUP_CONCAT(DISTINCT s.id ORDER BY s.id SEPARATOR ', '), ',', 3) AS star_ids,\n" +
                    "    r.rating\n" +
                    "FROM\n" +
                    "    movies m\n" +
                    "LEFT JOIN\n" +
                    "    genres_in_movies gm ON m.id = gm.movieId\n" +
                    "LEFT JOIN\n" +
                    "    genres g ON gm.genreId = g.id\n" +
                    "LEFT JOIN\n" +
                    "    stars_in_movies sm ON m.id = sm.movieId\n" +
                    "LEFT JOIN\n" +
                    "    stars s ON sm.starId = s.id\n" +
                    "LEFT JOIN\n" +
                    "    ratings r ON m.id = r.movieId\n" +
                    "GROUP BY\n" +
                    "    m.id;\n";

            // Perform the query
            ResultSet rs = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {
                String movie_id = rs.getString("id");
                String movie_title = rs.getString("title");
                String movie_year = rs.getString("year");
                String movie_director = rs.getString("director");
                String movie_genres = rs.getString("genres");
                String[] movie_stars = rs.getString("stars").split(", ");
                String[] movie_star_ids = rs.getString("star_ids").split(", ");

                String movie_rating = rs.getString("rating");



                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movie_id);
                jsonObject.addProperty("movie_title", movie_title);
                jsonObject.addProperty("movie_year", movie_year);
                jsonObject.addProperty("movie_director", movie_director);
                jsonObject.addProperty("movie_genres", movie_genres);
                jsonObject.addProperty("movie_star_1", movie_stars[0]);
                if (movie_stars.length > 1) {
                    jsonObject.addProperty("movie_star_2", movie_stars[1]);
                } else {
                    jsonObject.addProperty("movie_star_2", "None");
                }
                if (movie_stars.length > 2) {
                    jsonObject.addProperty("movie_star_3", movie_stars[2]);
                } else {
                    jsonObject.addProperty("movie_star_3", "None");
                }
                jsonObject.addProperty("movie_star_id_1", movie_star_ids[0]);
                if (movie_star_ids.length > 1) {
                    jsonObject.addProperty("movie_star_id_2", movie_star_ids[1]);
                } else {
                    jsonObject.addProperty("movie_star_id_2", "None");
                }
                if (movie_star_ids.length > 2) {
                    jsonObject.addProperty("movie_star_id_3", movie_star_ids[2]);
                } else {
                    jsonObject.addProperty("movie_star_id_3", "None");
                }
                //jsonObject.addProperty("movie_star_2", movie_stars[1]);
                //jsonObject.addProperty("movie_star_3", movie_stars[2]);

                jsonObject.addProperty("movie_rating", movie_rating);



                jsonArray.add(jsonObject);
            }
            rs.close();
            statement.close();

            // Log to localhost log
            request.getServletContext().log("getting " + jsonArray.size() + " results");

            // Write JSON string to output
            out.write(jsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }
}
