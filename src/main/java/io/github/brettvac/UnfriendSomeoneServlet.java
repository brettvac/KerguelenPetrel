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

@SuppressWarnings("serial")
public class UnfriendSomeoneServlet extends HttpServlet {

    private static final Logger log =
            Logger.getLogger(UnfriendSomeoneServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("text/plain; charset=UTF-8");

        Account exFriend = null;
        boolean isBoosted = false;

        try {

            String instance = System.getenv("MASTODON_INSTANCE_URL");
            String token = System.getenv("MASTODON_ACCESS_TOKEN");

            MastodonClient client =
                    new MastodonClient.Builder(instance)
                            .accessToken(token)
                            .build();

            exFriend = KerguelenpetrelService.findAndUnfollowFriend(client);
            isBoosted = KerguelenpetrelService.reblogRandomStatus(client, exFriend);

            // Single-line success log (core requirement)
            log.info(
                    "UnfriendSomeoneServlet success | unfollowed=@"
                            + (exFriend != null ? exFriend.getAcct() : "unknown")
                            + " | boosted=" + isBoosted
            );

            resp.getWriter().println("OK");

        } catch (BigBoneRequestException e) {

            log.log(Level.SEVERE, "Mastodon API error in UnfriendSomeoneServlet", e);
            resp.getWriter().println("OK");

        } catch (IOException e) {

            log.log(Level.SEVERE, "IO error in UnfriendSomeoneServlet", e);
            resp.getWriter().println("OK");

        } catch (Exception e) {

            log.log(Level.SEVERE, "Unexpected error in UnfriendSomeoneServlet", e);
            resp.getWriter().println("OK");
        }
    }
}