# KerguelenPetrel
This bot runs on Google App Engine. 

List of servlets:

  - **BotherSomeoneServlet** This servlet finds a top poster and toots a sentence from Wordnik to this user
  - **FriendSomeoneServlet** - Chooses a follower to friend and reposts somethoing from that user
  - **RespondServlet** - Checks for a response and responds accordingly with a sentence from Wordnik
  - **UnfriendSomeoneServlet** - Chooses a follower to unfriend and reblog one of their posts before unfollowing
  - **UpdateStatusServlet** - Updates the status with a title from an RSS feed. The servlet may also add a picture from Flickr to the toot.

## Moving From Twitter To Mastodon

The original Kerguelen Petrel bot posted to Twitter and required API keys created through an app, a somewhat annoying process that required phone verification. 

Since Twitter became X, it’s much easier to create an App in the developer console, but any API request now requires payment. 

Since this is just a fun bot, this iteration of Kerguelen Petrel retains almost the same functionality as the original bot, but it posts on Mastodon instead of Twitter.

It's quite straightforward (and free) to get KP set up and running on Mastodon.

Additionnaly, I have restored the functionality to use Wordnik as the API still works and it was relatively straightforward to make API calls directly from the service file.
 
## Setting up the bot
1. Create a Google App Engine application for Kerguelen Petrel
 1. Go to https://console.cloud.google.com/appengine and select "New project"
 2. Name the project "Kerguelen Petrel" 
 3. Click “Create Project” and take note of the project ID—it's generated automatically.
2. Add your Mastodon Instance URL, Auth Token and Wordnik API key to the block in appengine-web.xml
3. Test Kerguelen Petrel on your local server.
 1. From the project root, run `mvn package appengine:run` 
 2. Visit http://localhost:8080/ to test the Kerguelen Petrel functionality.
4. Deploy Kerguelen Petrel to Google App Engine.
 1. Run `gcloud services enable cloudscheduler.googleapis.com pubsub.googleapis.com` 
    3. Run `gcloud app create --region=us-central --project [YOUR_PROJECT_ID] replacing the placeholder with the Google App Engine project ID you noted earlie.
    4. Deploy to GAE using `mvn package appengine:deploy`.

---

You can modify the frequency of the bot's performance of each of those Servlets in the `cron.yaml` file.

The provided `cron.yaml` file has servlet run times that mimic a regular user.
