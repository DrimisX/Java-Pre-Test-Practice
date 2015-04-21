/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package services;

import credentials.Credentials;
import static credentials.Credentials.getConnection;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.RequestScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Dylan Huculak - c0630163
 */
@Path("/blog")
@RequestScoped
public class BlogService {
    
    /**
     * doGet that returns all rows from table when no id is given
     * @return Response from SELECT statement
     */
    @GET
    @Produces("application/json")
    public Response doGet() {
        return Response.ok(getResults("SELECT * FROM blogs"),
                MediaType.APPLICATION_JSON).build();
    }
    
    /**
     * @doGet that returns row of id given
     * @param id
     * @return Response from SELECT FROM WHERE statement
     */
    @GET
    @Path("{id}")
    @Produces("application/json")
    public Response doGet(@PathParam("id") int id) {
        return Response.ok(getResults("SELECT * FROM blogs WHERE blogid = " 
                + String.valueOf(id)),
                    MediaType.APPLICATION_JSON).build();   
    }
    
    /** 
     * doPost that inserts row with data given
     * @param insert
     * @return Response 500 if method completes (should not complete if INSERT successful)
     */
    @POST
    @Consumes("application/json")
    public Response doPost(JsonObject json) {
        Response postResponse;
        int maxId = 0;
        try (Connection conn = getConnection()) {
            PreparedStatement pstmt = conn
                .prepareStatement("INSERT INTO blogs (title,content,tags,userid) VALUES ("
                + "'" + json.getString("title") + "',"
                + "'" + json.getString("content") + "',"
                + "'" + json.getJsonArray("tags").toString() + "',"
                + String.valueOf(json.getInt("userid")) + ")",
                    Statement.RETURN_GENERATED_KEYS);

                pstmt.executeUpdate();
            
                // Get highest id (autoincremented id of last row)
                Statement checkId = conn.createStatement();
                checkId.execute("SELECT MAX(blogid) FROM blogs WHERE title = '" + json.getString("title") 
                        + "' AND content = '" + json.getString("content") 
                        + "' AND userid = " + json.getInt("userid"));
                ResultSet checkIdResults = checkId.getResultSet();
                if ( checkIdResults.next() ) {
                    maxId = checkIdResults.getInt(1);
                } 
                
                postResponse = Response.ok("http://localhost:8080/blog/" + String.valueOf(maxId) ).build();
        } catch (SQLException ex) {
            Logger.getLogger(BlogService.class.getName())
                    .log(Level.SEVERE, null, ex);
            postResponse = Response.status(500).build();
        }
        
        return postResponse;
    }
    
    /**
     * doPut method that updates row of id given with data given
     * @param id
     * @param update
     * @return Response 500 if method completes (should not complete if INSERT successful)
     */
    @PUT
    @Path("{id}")
    @Consumes("application/json")
    public Response doPut(@PathParam("id") int id, JsonObject json) {
        Response putResponse;
        try (Connection conn = getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("UPDATE blogs SET title='" 
                + json.getString("title") + "', content='" + json.getString("content")
                + "', tags='" + json.getJsonArray("tags").toString() 
                + "'," + " userid=" + String.valueOf(json.getInt("userid")) + " WHERE blogid = " 
                + id , Statement.RETURN_GENERATED_KEYS);
            
            pstmt.executeUpdate();
            
            putResponse = Response.ok("http://localhost:8080/blog/" + id ).build();
        } catch (SQLException ex) {
            Logger.getLogger(BlogService.class.getName())
                    .log(Level.SEVERE, null, ex);
            putResponse = Response.status(500).build();
        }
        
        return putResponse;
    }
    
    /**
     * doDelete method that deletes row of id given
     * @param id
     * @return empty string for a successful deletion
     */
    @DELETE
    @Path("{id}")
    @Consumes("application/json")
    public Response doDelete(@PathParam("id") int id) {
        Response delResponse;
        try (Connection conn = getConnection()) {
            PreparedStatement pstmt = conn
                .prepareStatement("DELETE FROM blogs WHERE blogid = " 
                    + String.valueOf(id)); 
            pstmt.executeUpdate();
            delResponse = Response.status(Response.Status.OK).entity("").build();
        } catch (SQLException ex) {
            Logger.getLogger(BlogService.class.getName())
                .log(Level.SEVERE, null, ex);
            delResponse = Response.status(500).build();
        }
        
        return delResponse;
    }
    
    private String getResults(String query, String... params) {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = Credentials.getConnection()){
            PreparedStatement pstmt = conn.prepareStatement(query);
            for (int i = 1; i <= params.length; i++) {
                pstmt.setString(i, params[i - 1]);
            }
            ResultSet rs = pstmt.executeQuery();
            sb.append("[ ");
            while (rs.next()) {
                sb.append(String.format("{ \"blogid\" : %s, \"title\" : %s, \"content\" : %s, \"tags\" : %s, \"datetime\" : %s }, \"userid\" : %s },\n", 
                        rs.getInt("productId"), rs.getString("title"), rs.getString("content"), rs.getArray("tags").toString() , rs.getTimestamp("datetime"), rs.getInt("userid")));
            }
            sb.setLength(Math.max(sb.length() - 2, 0));
            sb.append("]");
        } catch (SQLException ex) {
            Logger.getLogger(BlogService.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }
}
