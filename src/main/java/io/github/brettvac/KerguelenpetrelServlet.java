/**
 * @package    Kerguelen Petrel
 * @license    Apache License 2.0
 * @Copyright (c) 2017-2026 Brett
 *
 * KerguelenpetrelServlet - Testing interface for service class functionalities
 */

package io.github.brettvac.kerguelenpetrel;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.util.List;

import social.bigbone.MastodonClient;
import social.bigbone.api.entity.Account;
import social.bigbone.api.entity.Status;
import social.bigbone.api.entity.data.Visibility;
import social.bigbone.api.exception.BigBoneRequestException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rometools.rome.io.FeedException;

public class KerguelenpetrelServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Using HTML so we can render a clickable menu for easy testing
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        
        String action = req.getParameter("action");

        // If no action is provided, render the testing menu
        if (action == null || action.isEmpty()) {
            out.println("<!DOCTYPE html><html><head><title>Test Menu</title></head><body>");
            out.println("<h2>Kerguelen Petrel - Manual Testing Interface</h2>");
            out.println("<ul>");
            out.println("<li><a href=\"?action=post\">1. Post a Status</a></li>");
            out.println("<li><a href=\"?action=post_image\">2. Post a Status with Image</a></li>");
            out.println("<li><a href=\"?action=bother\">3. Bother Someone</a></li>");
            out.println("<li><a href=\"?action=friend\">4. Friend Someone</a></li>");
            out.println("<li><a href=\"?action=unfriend\">5. Unfriend Someone</a></li>");
            out.println("<li><a href=\"?action=unfriend_boost\">6. Unfriend and Boost</a></li>");
            out.println("</ul>");
            out.println("</body></html>");
            out.close();
            return;
        }

        out.println("<!DOCTYPE html><html><body><pre>");
        out.println("Executing action: " + action + "...\n");

        try {
            // Set up Mastodon (BigBone) and API Keys
            String instance = System.getenv("MASTODON_INSTANCE_URL");
            String token = System.getenv("MASTODON_ACCESS_TOKEN");
            String wordnikKey = System.getenv("WORDNIK_API_KEY"); 
            
            MastodonClient client = new MastodonClient.Builder(instance).accessToken(token).build();

            switch (action) {
                case "post":
                    String text1 = KerguelenpetrelService.createStatusUpdate();
                    Status s1 = client.statuses().postStatus(
                            text1, null, Visibility.PUBLIC, null, false, null, null).execute();
                    out.println("Success! Toot posted: " + s1.getContent());
                    break;

                case "post_image":
                    String text2 = KerguelenpetrelService.createStatusUpdate();
                    List<String> mediaIds = KerguelenpetrelService.addFlickrImg(client);
                    Status s2 = client.statuses().postStatus(
                            text2, mediaIds, Visibility.PUBLIC, null, false, null, null).execute();
                    out.println("Success! Toot posted with Image: " + s2.getContent());
                    break;

                case "bother":
                    if (wordnikKey == null || wordnikKey.isEmpty()) {
                        out.println("Error: WORDNIK_API_KEY environment variable is not set.");
                        break;
                    }
                    String botherText = KerguelenpetrelService.createBotherStatus(client, wordnikKey);
                    Status s3 = client.statuses().postStatus(
                            botherText, null, Visibility.PUBLIC, null, false, null, null).execute();
                    out.println("Success! Bothered a user: " + s3.getContent());
                    break;

                case "friend":
                    Account newFriend = KerguelenpetrelService.findAndMakeNewFriend(client);
                    out.println("Success! Friended new account: @" + newFriend.getAcct());
                    break;

                case "unfriend":
                    Account exFriend = KerguelenpetrelService.findAndUnfollowFriend(client);
                    out.println("Success! Unfriended account: @" + exFriend.getAcct());
                    break;
                    
               case "unfriend_boost":
                  Account exFriend2 = KerguelenpetrelService.findAndUnfollowFriend(client);

                  boolean boosted = KerguelenpetrelService.reblogRandomStatus(client, exFriend2);

                  out.println("Success! Unfriended account: @" + exFriend2.getAcct());

                  if (boosted) {
                      out.println("Boosted a random post from them one last time!");
                  } else {
                      out.println("This user hasn't posted anything to boost.");
                  }
                  break;

                default:
                    out.println("Unknown action requested.");
                    break;
            }

        } catch (FeedException e) {
            out.println("Problem with RSS Feed: ");
            e.printStackTrace(out);
        } catch (FileNotFoundException e) {
            out.println("Problem with static file: ");
            e.printStackTrace(out);
        } catch (BigBoneRequestException e) {
            out.println("Problem with Mastodon API: ");
            e.printStackTrace(out);
        } catch (IOException e) {
            out.println("Problem with Input/Output: ");
            e.printStackTrace(out);
        } catch (Exception e) {
            out.println("An unexpected problem occurred: ");
            e.printStackTrace(out);
        } finally {
            out.println("\n</pre>");
            out.println("<a href=\"?\">Return to Testing Menu</a>");
            out.println("</body></html>");
            out.close();
        }
    }
}