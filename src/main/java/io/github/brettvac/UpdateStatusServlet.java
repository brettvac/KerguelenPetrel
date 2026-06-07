/**
 * @package    Kerguelen Petrel
 * @license    Apache License 2.0
 * @Copyright (c) 2017-2026 Brett
 *
 * UpdateStatusServlet - post a status update or "toot"
 */

package io.github.brettvac.kerguelenpetrel;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.net.URL;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import social.bigbone.MastodonClient;
import social.bigbone.api.entity.Status;
import social.bigbone.api.entity.data.Visibility;
import social.bigbone.api.exception.BigBoneRequestException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rometools.rome.io.FeedException;

public class UpdateStatusServlet extends HttpServlet 
  {
  
  private static final Logger log = Logger.getLogger(UpdateStatusServlet.class.getName());
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException 
    {   
    resp.setContentType("text/plain; charset=UTF-8");    
   
    // Our status update
    String toot = new String(); 

    try
       {
      
       // Set up Mastodon (BigBone)
       String instance = System.getenv("MASTODON_INSTANCE_URL");
       String token = System.getenv("MASTODON_ACCESS_TOKEN");
       
       MastodonClient client = new MastodonClient.Builder(instance).accessToken(token).build();
         
       //Get the status text from the service class
       String statusText = KerguelenpetrelService.createStatusUpdate();

       // Optional media
       List<String> mediaIds = null; 
       
       // Add an image from Flickr roughly 25% of the time
       if (System.nanoTime() % 4 == 0)
         {
         mediaIds = KerguelenpetrelService.addFlickrImg(client);    
         }
       
       //Post to Mastodon
       Status postedStatus = client.statuses()
           .postStatus
              (statusText,mediaIds,Visibility.PUBLIC, 
               null, // inReplyToId
               false, // sensitive
               null, // spoilerText
               null  // language
              ).execute();

       toot = postedStatus.getContent();
       } 
    
    // --- Specific exceptions ---
    catch (FeedException e) {
        log.log(Level.SEVERE, "Problem with RSS Feed", e);
    }

    catch (FileNotFoundException e) {
        log.log(Level.SEVERE, "Problem with static file", e);
    }

    catch (BigBoneRequestException e) {
        log.log(Level.SEVERE, "Problem with Mastodon request", e);
    }

    // And now for the general parent exceptions
    catch (IOException e) {
        log.log(Level.SEVERE, "Problem with Input/Output", e);
    }
    
    catch (Exception e) {
        log.log(Level.SEVERE, "Unexpected error in UpdateStatusServlet", e);
    }
        
    finally 
        {
        if(toot.length() > 0)
          {
          log.info("Toot posted successfully | content=" + toot);
          }
        resp.getWriter().println("OK");
        }         
    }   
  
  }