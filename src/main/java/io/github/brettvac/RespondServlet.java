package io.github.brettvac.kerguelenpetrel;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;

import social.bigbone.MastodonClient;
import social.bigbone.api.Pageable;
import social.bigbone.api.entity.CredentialAccount;
import social.bigbone.api.entity.Notification;
import social.bigbone.api.entity.Status;
import social.bigbone.api.entity.data.Visibility;
import social.bigbone.api.exception.BigBoneRequestException;

@SuppressWarnings("serial")
public class RespondServlet extends HttpServlet {

    private static final Logger log =
            Logger.getLogger(RespondServlet.class.getName());

    private static Entity lastPostIdEntity;
    private static String lastPostId = "0";

    private static final int MAX_TOOT_LENGTH = 500;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("text/plain; charset=UTF-8");

        try {

            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

            String instance = System.getenv("MASTODON_INSTANCE_URL");
            String token = System.getenv("MASTODON_ACCESS_TOKEN");

            MastodonClient client =
                    new MastodonClient.Builder(instance)
                            .accessToken(token)
                            .build();

            CredentialAccount me =
                    client.accounts().verifyCredentials().execute();

            Pageable<Notification> notificationsPage =
                    client.notifications().getAllNotifications().execute();

            List<Notification> notifications = notificationsPage.getPart();

            lastPostIdEntity =
                    datastore.get(KeyFactory.createKey("lastPostIDEntity", "ID"));

            lastPostId =
                    lastPostIdEntity.getProperty("lastPostID").toString();

            if (notifications == null || notifications.isEmpty()) {
                log.info("No mentions so far");
                resp.getWriter().println("OK");
                return;
            }

            Notification targetNotification = null;

            for (Notification notification : notifications) {

                if (!"mention".equals(notification.getType())) {
                    continue;
                }

                if (!KerguelenpetrelService.isNewer(notification.getId(), lastPostId)) {
                    continue;
                }

                targetNotification = notification;
                break;
            }

            if (targetNotification == null) {
                log.info("No new mentions");
                resp.getWriter().println("OK");
                return;
            }

            Status mention = targetNotification.getStatus();

            if (mention.getAccount().getId().equals(me.getId())) {

                lastPostIdEntity.setProperty("lastPostID", targetNotification.getId());
                datastore.put(lastPostIdEntity);

                log.info("Skipped self-mention");
                resp.getWriter().println("OK");
                return;
            }

            if (mention.isReblogged()) {

                client.statuses()
                        .favouriteStatus(mention.getId())
                        .execute();

                lastPostIdEntity.setProperty("lastPostID", targetNotification.getId());
                datastore.put(lastPostIdEntity);

                log.info("Favourited reblogged mention");
                resp.getWriter().println("OK");
                return;
            }

            String response = KerguelenpetrelService.createResponse(targetNotification.getAccount().getAcct(),mention);

            Status postedStatus = client.statuses().postStatus(
                    response,
                    null,
                    Visibility.PUBLIC,
                    mention.getId(),
                    false,
                    null,
                    null
            ).execute();

            lastPostIdEntity.setProperty("lastPostID", targetNotification.getId());
            datastore.put(lastPostIdEntity);

            log.info("Toot posted successfully | content=" + postedStatus.getContent());

            resp.getWriter().println("OK");

        } catch (EntityNotFoundException e) {

            DatastoreService datastore =
                    DatastoreServiceFactory.getDatastoreService();

            lastPostIdEntity = new Entity("lastPostIDEntity", "ID");
            lastPostIdEntity.setProperty("lastPostID", "0");

            datastore.put(lastPostIdEntity);

            log.warning("Created missing lastPostID entity");

            resp.getWriter().println("OK");

        } catch (BigBoneRequestException e) {

            log.log(Level.SEVERE, "Mastodon API error", e);

            resp.getWriter().println("OK");

        } catch (Exception e) {

            log.log(Level.SEVERE, "Unexpected error in RespondServlet", e);

            resp.getWriter().println("OK");
        }
    }
}