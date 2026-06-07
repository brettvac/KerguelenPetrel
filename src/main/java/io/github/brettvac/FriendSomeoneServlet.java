package io.github.brettvac.kerguelenpetrel;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import social.bigbone.MastodonClient;
import social.bigbone.api.entity.Account;
import social.bigbone.api.exception.BigBoneRequestException;

public class FriendSomeoneServlet extends HttpServlet {

    private static final Logger log =
            Logger.getLogger(FriendSomeoneServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("text/plain; charset=UTF-8");

        Account newFriend = null;
        boolean isBoosted = false;

        try {

            // Set up Mastodon
            String instance = System.getenv("MASTODON_INSTANCE_URL");
            String token = System.getenv("MASTODON_ACCESS_TOKEN");

            MastodonClient client =
                    new MastodonClient.Builder(instance)
                            .accessToken(token)
                            .build();

            newFriend = KerguelenpetrelService.findAndMakeNewFriend(client);
            isBoosted = KerguelenpetrelService.reblogRandomStatus(client, newFriend);

            // Single success log line (important requirement)
            log.info(
                    "FriendSomeoneServlet success | newFriend=@"
                            + (newFriend != null ? newFriend.getAcct() : "unknown")
                            + " | boosted=" + isBoosted
            );

            resp.getWriter().println("OK");

        } catch (BigBoneRequestException e) {

            log.log(Level.SEVERE, "Mastodon error in FriendSomeoneServlet", e);
            resp.getWriter().println("OK");

        } catch (IOException e) {

            log.log(Level.SEVERE, "IO error in FriendSomeoneServlet", e);
            resp.getWriter().println("OK");

        } catch (Exception e) {

            log.log(Level.SEVERE, "Unexpected error in FriendSomeoneServlet", e);
            resp.getWriter().println("OK");
        }
    }
}