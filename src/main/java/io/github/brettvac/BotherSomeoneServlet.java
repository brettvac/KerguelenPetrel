/**
 * @package    Kerguelen Petrel
 * @license    Apache License 2.0
 * @Copyright (c) 2017-2026 Brett
 *
 * BotherSomeoneServlet - write something to a friend of a follower on Mastodon
 */

package io.github.brettvac.kerguelenpetrel;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import social.bigbone.MastodonClient;
import social.bigbone.api.entity.Status;
import social.bigbone.api.entity.data.Visibility;
import social.bigbone.api.exception.BigBoneRequestException;

public class BotherSomeoneServlet extends HttpServlet {

    private static final Logger log =
            Logger.getLogger(BotherSomeoneServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("text/plain; charset=UTF-8");
        String toot = "";

        try {
            // Set up Mastodon
            String instance = System.getenv("MASTODON_INSTANCE_URL");
            String token = System.getenv("MASTODON_ACCESS_TOKEN");
            MastodonClient client = new MastodonClient.Builder(instance).accessToken(token).build();

            String apiKey = System.getenv("WORDNIK_API_KEY");

            // Get the compiled status text from the service class
            String statusText = KerguelenpetrelService.createBotherStatus(client, apiKey);

            // Post the status to Mastodon
            Status postedStatus = client.statuses().postStatus(
                    statusText,
                    null,              // mediaIds
                    Visibility.PUBLIC, // visibility
                    null,              // inReplyToId
                    false,             // sensitive
                    null,              // spoilerText
                    null               // language
            ).execute();

            toot = postedStatus.getContent();

            // Single success log line
            log.info("BotherSomeoneServlet success | tootPosted=true | content=" + toot);

        } catch (BigBoneRequestException e) {

            log.log(Level.SEVERE, "Problem with Mastodon in BotherSomeoneServlet", e);

        } catch (IOException e) {

            // Captures both network/Wordnik errors and explicit validation failures (e.g. no followers found)
            log.log(Level.SEVERE, "Problem with Input/Output or Business Logic in BotherSomeoneServlet", e);

        } catch (Exception e) {

            log.log(Level.SEVERE, "Unexpected problem in BotherSomeoneServlet", e);

        } finally {

            if (toot.length() > 0) {
                log.info("BotherSomeoneServlet completed | tootPosted=true | content=" + toot);
            }

            resp.getWriter().println("OK");
        }
    }
}