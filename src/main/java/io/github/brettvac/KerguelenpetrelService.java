/**
 * @package    Kerguelen Petrel
 * @license    Apache License 2.0
 * @Copyright (c) 2019-2026 Brett
 *
 * KerguelenpetrelService - utility service for Kerguelen Petrel Bot
 */

package io.github.brettvac.kerguelenpetrel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import com.google.appengine.api.urlfetch.*;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Arrays;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndLink;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import social.bigbone.MastodonClient;
import social.bigbone.api.entity.MediaAttachment;
import social.bigbone.api.method.FileAsMediaAttachment;
import social.bigbone.api.exception.BigBoneRequestException;
import social.bigbone.api.Pageable;
import social.bigbone.api.entity.Account;
import social.bigbone.api.entity.CredentialAccount;
import social.bigbone.api.entity.Status;
import social.bigbone.api.method.DirectoryMethods;

import org.jsoup.Jsoup;

public class KerguelenpetrelService
{
    private static final Random rand = new Random();
    private static final int MAX_TOOT_LENGTH = 500;

    private static final String FEEDSFILE = "WEB-INF/StaticFiles/feeds";
    private static final String FLICKRFEED = "WEB-INF/StaticFiles/flickrfeed";
    
    /**
     * Helper function to return a random non-empty line from a provided file containing URL feeds.
     */
    private static String getRandomLine(String feedsFile) throws FileNotFoundException, IOException
       {  
       int numlines = 1;
       String buf = "";
       String randline = null;

       File file = new File(feedsFile);
       Scanner input = new Scanner(file, "UTF-8");     

       while (input.hasNextLine())
         {
         buf = input.nextLine().trim();

         if (!buf.isEmpty()) 
           {
           if ((rand.nextInt() % numlines) == 0)
             {
             randline = buf;  //chance of saving line n 1/n
             }

           numlines++;
           }
         }

        if (randline == null || randline.isEmpty())
          {
          throw new IOException("File contains empty lines" + feedsFile);
          }
        
        input.close();
        return randline;
       }

    /**
     * Returns a random RSS feed entry from a random feed URL.
     */
    private static SyndEntry getRandomFeedEntry(String feedsFilePath)  throws IOException, FeedException
      {
      String selectedFeed = getRandomLine(feedsFilePath);

      URLFetchService fetchService = URLFetchServiceFactory.getURLFetchService();
      URL feedUrl = new URL(selectedFeed); 
      FetchOptions fetchOptions = FetchOptions.Builder.withDefaults().setDeadline(60.0); 
      HTTPRequest request = new HTTPRequest(feedUrl, HTTPMethod.GET, fetchOptions); 

      HTTPResponse response = fetchService.fetch(request);

      if (response.getResponseCode() != 200) 
        {
        throw new IOException("Failed to fetch feed: HTTP " + response.getResponseCode());
        }

      String xml = new String(response.getContent(), StandardCharsets.UTF_8);

      // Attempt to sanitize the feed
      xml = xml.replaceAll("(?s)<!DOCTYPE.*?>", "");

      InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
      XmlReader xmlReader = new XmlReader(input); 
      SyndFeed feed = new SyndFeedInput().build(xmlReader);
      
      List<SyndEntry> entries = feed.getEntries();

      if (entries == null || entries.isEmpty()) 
        {
        throw new FeedException("RSS feed contains no entries: " + selectedFeed);
        }

      return entries.get(rand.nextInt(entries.size()));
      }

    /**
     * Returns the title from a random feed entry.
     * @return Feed title
     *
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FeedException
     */
    private static String getFeedTitle() throws FileNotFoundException, IOException, FeedException
      {
      SyndEntry entry = getRandomFeedEntry(FEEDSFILE);

      if (entry.getTitle() == null)
        {
        return "";
        }

      // Extract raw HTML/text and clean it
      String rawTitle = entry.getTitle();
      
      return Jsoup.parse(rawTitle).text();
      }

    /**
     * Returns the description from a random feed entry.
     *
     * @return Feed description
     *
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FeedException
     */
    private static String getFeedDescription() throws FileNotFoundException, IOException, FeedException
      {
      SyndEntry entry = getRandomFeedEntry(FEEDSFILE);

      if (entry.getDescription() == null || entry.getDescription().getValue() == null)
        {
        return "";
        }

      // Extract raw HTML/text and clean it
      String rawDescription = entry.getDescription().getValue();
      return Jsoup.parse(rawDescription).text();
      }
    
    public static String createStatusUpdate() throws FileNotFoundException, IOException, FeedException
      {
      StringBuilder builder = new StringBuilder();

      // Append feed title
      builder.append(getFeedTitle());
        
      // Add separator at the end of the feed title
      String[] separator = new String[] { "?","!",",","-"," " };
      builder.append(separator[rand.nextInt(separator.length)]).append(" ");
        
      // Append feed description
      builder.append(getFeedDescription());
        
      /* Mastodon toots are maximum 500 characters, so trim appropriately */
      if (builder.length() > MAX_TOOT_LENGTH) 
        {
            if (builder.lastIndexOf(";", (MAX_TOOT_LENGTH-30)) > 0)
                builder.setLength(builder.lastIndexOf(";", (MAX_TOOT_LENGTH-30))); 
            else if (builder.lastIndexOf(":", (MAX_TOOT_LENGTH-30)) > 0)
                builder.setLength(builder.lastIndexOf(":", (MAX_TOOT_LENGTH-30)));  
            else if (builder.lastIndexOf(",", (MAX_TOOT_LENGTH-30)) > 0)
                builder.setLength(builder.lastIndexOf(",", (MAX_TOOT_LENGTH-30)));
            else
                builder.setLength((MAX_TOOT_LENGTH-30));
        }

      return builder.toString();
      }
    
    public static String getWordnikContent(String apiKey) throws IOException 
      {     
       String wordnikApiRandomWordsUrl =
                "https://api.wordnik.com/v4/words.json/randomWord"
                + "?hasDictionaryDef=true"
                + "&minLength=5"
                + "&api_key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        
        URL url = new URL(wordnikApiRandomWordsUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");

        int code = connection.getResponseCode();

        if (code != 200) 
          {
            throw new IOException("Failed to get random word. HTTP code: " + code);
          }

        String wordsResponse;
        
        try (InputStream is = connection.getInputStream()) {
           wordsResponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        
        // Gson's JsonParser and JsonObject
        JsonObject randomWordObj = JsonParser.parseString(wordsResponse).getAsJsonObject();
        
        String seedWord = randomWordObj.get("word").getAsString();

        String wordnikApiWordUrl =
                "https://api.wordnik.com/v4/word.json/"
                        + URLEncoder.encode(seedWord, StandardCharsets.UTF_8)
                        + "/topExample?useCanonical=false&api_key="
                        + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        url = new URL(wordnikApiWordUrl);
        connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");

        code = connection.getResponseCode();

        if (code == 401 || code == 403) {
            throw new IOException("Invalid Wordnik API key or unauthorized request.");
        }
        if (code == 404) {
           return seedWord; // Seed word does not have an example sentence
        }
        if (code != 200) {
            throw new IOException("HTTP error code: " + code);
        }

        String wordResponse;
        
        try (InputStream is = connection.getInputStream()) {
           wordResponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        JsonObject wordResponseObj = JsonParser.parseString(wordResponse).getAsJsonObject();

        if (!wordResponseObj.has("text") || wordResponseObj.get("text").isJsonNull()) {
            throw new IOException("No example sentence found.");
        }

        String sentence = wordResponseObj.get("text").getAsString().trim();
        
        String[] end = { ".", "?", "!", " :-)", "...", "…", "!!", "?!" };

        boolean hasValidEnding = Arrays.stream(end).anyMatch(sentence::endsWith);
         
        if (!hasValidEnding) {
             sentence += end[rand.nextInt(end.length)];
        }

        return sentence;
      }

    
    public static List<String> addFlickrImg(MastodonClient client) throws IOException, FeedException, BigBoneRequestException
      {
      // Call the helper method to get a random entry
      SyndEntry entry = getRandomFeedEntry(FLICKRFEED);

      // Extract the media URL
      List<SyndLink> links = entry.getLinks();
      // Ensure the index exists to avoid IndexOutOfBoundsException
      if (links.size() < 2) {
          throw new FeedException("Feed entry does not contain enough links for media.");
      }
      String mediaURL = links.get(1).getHref();

      File tempFile = File.createTempFile("flickr-", ".jpg");

      // Open input stream, copy file, and close stream
      URL url = new URL(mediaURL);
      
      try (InputStream in = url.openStream()) {
           Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

      FileAsMediaAttachment attachment = new FileAsMediaAttachment(tempFile, "image/jpeg");
      MediaAttachment uploadedFile = client.media().uploadMediaAsync(attachment).execute();

      return Collections.singletonList(uploadedFile.getId());
      }    
    
    /**
     * Business logic to pick a random follower, find one of their followers, 
     * and construct a targeted sentence using the Wordnik API.
     * * @param client The configured MastodonClient instance
     * @param apiKey The Wordnik API key string
     * @return Prepared status string ready for posting
     * @throws BigBoneRequestException if Mastodon API lookups fail
     * @throws IOException if network calls fail or no targets are found
     */
    public static String createBotherStatus(MastodonClient client, String apiKey) throws BigBoneRequestException, IOException 
      {
      // Verify credentials to get our own Account ID
      CredentialAccount me = client.accounts().verifyCredentials().execute();
      
      // Get our following (to find a friend)
      Pageable<Account> followingPage = client.accounts().getFollowing(me.getId()).execute();
      List<Account> myFollowing = followingPage.getPart();
      
      if (myFollowing == null || myFollowing.isEmpty()) 
        {
        throw new IOException("Cannot find any followers to bother");
        }
      
      // Pick a random account we are following
      Account randomAccount = myFollowing.get(rand.nextInt(myFollowing.size()));
      
      // Load the potential victim IDs (followers of our random account)
      Pageable<Account> victimPage = client.accounts().getFollowers(randomAccount.getId()).execute();
      List<Account> victims = victimPage.getPart();

      if (victims == null || victims.isEmpty()) 
        {
        throw new IOException("Cannot find any downstream followers to bother");
        }
      
      // Select a random victim from our follower's followers
      Account victim = victims.get(rand.nextInt(victims.size()));
      
      // Construct the message string
      StringBuilder builder = new StringBuilder();
      builder.append("@").append(victim.getAcct()).append(" ");
      builder.append(getWordnikContent(apiKey));

      // Guard rails for maximum length constraint
      if (builder.length() > MAX_TOOT_LENGTH) 
        {
        builder.setLength(MAX_TOOT_LENGTH); 
        }
      
      return builder.toString();
      }

    /**
     * Finds a random active account from the instance directory, follows them, 
     * and returns the target Account object.
     */
    public static Account findAndMakeNewFriend(MastodonClient client) throws BigBoneRequestException, IOException
       {
       List<Account> accounts = client.directories().listAccounts(
               true,
               DirectoryMethods.AccountOrder.ACTIVE,
               0,
               100
       ).execute();

       if (accounts == null || accounts.isEmpty()) 
          {
          throw new IOException("No active accounts found in the directory.");
          }

       Account account = accounts.get(rand.nextInt(accounts.size()));
       
       // Remove ourself from the candidate list
       CredentialAccount me = client.accounts().verifyCredentials().execute();

        accounts.removeIf(a -> a.getId().equals(me.getId()));

        if (accounts.isEmpty()) {
            throw new IOException("No valid accounts after filtering self.");
        }

        client.accounts().followAccount(account.getId()).execute();
       
       return account;
       }

    /**
     * Boosts (reblogs) a random status from the given account.
     * @return true if a post was boosted, false if the user had no statuses.
     */
    public static boolean reblogRandomStatus(MastodonClient client, Account account) throws BigBoneRequestException
       {
       Pageable<Status> pageableStatuses = client.accounts().getStatuses(account.getId()).execute();
       List<Status> statuses = pageableStatuses.getPart();

       if (statuses != null && !statuses.isEmpty()) 
          {
          Status randomStatus = statuses.get(rand.nextInt(statuses.size()));
          client.statuses().reblogStatus(randomStatus.getId()).execute();
          return true;
          }
          
       return false;
       }
       
       /**
     * Finds a random account that the bot currently follows, unfollows them,
     * and returns the target Account object.
     * @param client The configured MastodonClient instance
     * @return The Account that was unfollowed
     * @throws BigBoneRequestException if Mastodon API lookups fail
     * @throws IOException if the bot is not following anyone
     */
    public static Account findAndUnfollowFriend(MastodonClient client) throws BigBoneRequestException, IOException {
        // Verify credentials to get our own Account ID
        CredentialAccount me = client.accounts().verifyCredentials().execute();

        // Find a follower to unfriend
        Pageable<Account> followerPage = client.accounts().getFollowing(me.getId()).execute();
        List<Account> following = followerPage.getPart();
        
        if (following == null || following.isEmpty()) {
            throw new IOException("No friends to unfollow");
        } 
       
        // Pick a random account to unfollow
        Account unfriend = following.get(rand.nextInt(following.size()));
        
        // Unfollow them
        client.accounts().unfollowAccount(unfriend.getId()).execute();
        
        return unfriend;
    }

    /**
     * Helper to safely compare Mastodon Snowflake IDs (Strings).
     * Compares length first to avoid Long overflow, then lexicographically.
     */
    public static boolean isNewer(String incomingId, String storedId) {
        if (incomingId.length() != storedId.length()) {
            return incomingId.length() > storedId.length();
        }
        return incomingId.compareTo(storedId) > 0;
    }
    
    public static String createResponse(String handle, Status mention)
        throws IOException {

      StringBuilder builder = new StringBuilder();

      builder.append("@").append(handle).append(" ");

      if (mention.getContent().toLowerCase().contains("bye")) {
          builder.append("Bye");
      } else {
          String apiKey = System.getenv("WORDNIK_API_KEY");
          builder.append(getWordnikContent(apiKey));
      }

      if (builder.length() > MAX_TOOT_LENGTH) {
          builder.setLength(MAX_TOOT_LENGTH);
      }

      return builder.toString();
    }
}