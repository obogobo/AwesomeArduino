import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TwitterCommunicator implements Tweetable {
    // Java 6 workaround for lack of switch(String) support
    private final int CHECK_FEED = 1;
    private final int SEND_REPLY = 2;
    private ConcurrentLinkedQueue<Tweet> tweetQueue;
    private ConcurrentLinkedQueue<Reply> replyQueue;
    private String lastTweetId;

    public TwitterCommunicator(int initialDelay, int refreshInterval) {
        this.tweetQueue = new ConcurrentLinkedQueue<Tweet>();
        this.replyQueue = new ConcurrentLinkedQueue<Reply>();
        Timer timer = new Timer();
        this.lastTweetId = "1";

        // periodically interact with Twitter
        timer.schedule(new TimerTask() {
            public void run() {
                System.out.println(String.format("[%s] Refreshing feed...", rightNow()));
                HashMap<String, String> bindings;

                // check feed for new @mentions
                bindings = new HashMap<String, String>();
                bindings.put("requestMethod", "GET");
                bindings.put("requestURL", "https://api.twitter.com/1.1/statuses/mentions_timeline.json");
                bindings.put("since_id", lastTweetId);
                bindings.put("queryArgs", "since_id=" + lastTweetId);
                bindings.put("verbose", "false");

                fireRequest(CHECK_FEED, bindings);
            }
        }, initialDelay, refreshInterval);

        // periodically interact with Twitter (+30 seconds)
        timer.schedule(new TimerTask() {
            public void run() {
                System.out.println(String.format("[%s] Batch processing @replies...", rightNow()));
                HashMap<String, String> bindings;

                // send out all pending @replies
                Reply reply;
                while ((reply = replyQueue.poll()) != null) {
                    String status = "@" + reply.recipient.handle + " " + reply.message;
                    String responseId = reply.source.tweetId;

                    bindings = new HashMap<String, String>();
                    bindings.put("requestMethod", "POST");
                    bindings.put("requestURL", "https://api.twitter.com/1.1/statuses/update.json");
                    bindings.put("status", status);
                    bindings.put("responseId", responseId);
                    bindings.put("queryArgs", null);
                    bindings.put("verbose", "false");

                    fireRequest(SEND_REPLY, bindings);
                }
            }
        }, initialDelay + (30 * 1000), refreshInterval);
    }

    private void fireRequest(int task, HashMap<String, String> bindings) {
        String requestMethod = bindings.get("requestMethod");
        String requestURL = bindings.get("requestURL");
        String queryArgs = bindings.get("queryArgs");

        // each request must be authenticated using OAuth
        // ...just run the already working JavaScript implementation!
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine jsEngine = factory.getEngineByName("JavaScript");
        String basePath = "/Users/Tanner/Documents/Arduino/AwesomeArduino/Processing/";
        String[] scripts = {
                "js/hmac-sha1.js",
                "js/md5.js",
                "js/enc-base64-min.js",
                "js/authenticateRequest.js"};
        File scriptFile;
        Reader scriptReader;

        // pass through all necessary params (Java -> JavaScript)
        for (String key : bindings.keySet()) {
            jsEngine.put(key, bindings.get(key));
        }

        try {
            // execute the scripts above, sequentially (ordered by definition),
            // they'll share a globally common namespace within the engine instance.
            for (String script : scripts) {
                scriptFile = new File(basePath + script);
                scriptReader = new FileReader(scriptFile);
                jsEngine.eval(scriptReader);
            }

            // build the authenticated request header
            String authString = "OAuth oauth_consumer_key=\""
                    + jsEngine.get("oauth_consumer_key") + "\", oauth_nonce=\""
                    + jsEngine.get("nonce") + "\", oauth_signature=\""
                    + jsEngine.get("hash") + "\", oauth_signature_method=\""
                    + "HMAC-SHA1" + "\", oauth_timestamp=\""
                    + jsEngine.get("timestamp") + "\", oauth_token=\""
                    + jsEngine.get("oauth_token") + "\", oauth_version=\"" + "1.0"
                    + "\"";

            //System.out.println("OAuth header generated at " + rightNow());
            //System.out.println("\t" + authString);

            // send off the (now) authenticated API request
            // GETs pass data through URL parameters
            // POSTs write data directly over the wire, after the headers
            URL url = new URL(requestURL + (requestMethod.equals("GET") ? "?" + queryArgs : ""));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(requestMethod);
            conn.setRequestProperty("Authorization", authString);

            if (requestMethod.equals("GET")) {
                conn.connect();
                System.out.println(String.format("[%s] GET request fired at %s", rightNow(), conn.getURL()));
            } else if (requestMethod.equals("POST")) {
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);
                OutputStreamWriter post = new OutputStreamWriter(conn.getOutputStream());
                String postData = (String) jsEngine.get("postData");
                post.write(postData);
                post.close();
                System.out.println(String.format("[%s] POST request fired at %s", rightNow(), conn.getURL()));
                //System.out.println("\t" + postData);
            }

            // did it work?
            if (conn.getResponseCode() == 200) {
                System.out.println(String.format("[%s] 200 OK, response recieved!", rightNow()));

                // expecting 1 line of JSON in response
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String res = br.readLine();
                System.out.println("\t" + res);

                br.close();
                conn.disconnect();

                // per task additional logic
                switch (task) {
                    case CHECK_FEED:
                        // parse JSON into an array of @mention Tweet objects
                        JSONArray rawTweets = new JSONArray(res);
                        for (int i = 0; i < rawTweets.length(); i++) {
                            JSONObject jsobj = rawTweets.getJSONObject(i);

                            // pluck out some useful information...
                            String messageText = jsobj.getString("text");
                            String tweetTime = jsobj.getString("created_at");
                            String thisTweetId = jsobj.getString("id_str");
                            //System.out.println("**************" + thisTweetId);

                            // VERY convenient that new Date(String) is still around
                            Date timestamp = new Date(tweetTime);

                            // keep track of the most recently received Tweet
                            if (Long.parseLong(thisTweetId) > Long.parseLong(lastTweetId)) {
                                lastTweetId = thisTweetId;
                            }

                            // pluck out some more useful information...
                            JSONObject usr = jsobj.getJSONObject("user");
                            String handle = usr.getString("screen_name");
                            String avatarURL = usr.getString("profile_image_url_https");

                            // finally, package together metadata and enqueue the new Tweet!
                            Tweet tweet = new Tweet(new User(handle, avatarURL), messageText, timestamp, thisTweetId);
                            tweetQueue.offer(tweet);
                        }
                        break;
                    case SEND_REPLY:
                        // Eh.. HTTP Status will let us know if something went wrong
                        break;
                }
            } else {
                // why did the request fail?
                System.out.println(String.format("[%s] %s %s! (%s) ",
                        rightNow(),
                        conn.getResponseCode(),
                        conn.getResponseMessage(),
                        "..." + (task == CHECK_FEED ? "feed check anomaly" : "reply sending anomaly")));
            }
        } catch (Exception e) {
            // we have a --wide-- variety of possible exceptions!!
            e.printStackTrace();
        }
    }

    // millis since the epoch
    public long rightNow() {
        return (new Date()).getTime();
    }

    @Override
    public Queue<Tweet> getTweets() {
        Queue<Tweet> tweets = new LinkedList<Tweet>();

        while (!tweetQueue.isEmpty()) {
            tweets.add(tweetQueue.poll());
        }
        return tweets;
    }

    public Queue<Reply> getReplies() {
        return replyQueue;
    }

    @Override
    public void reply(String message, Tweet source) {
        Reply reply = new Reply(source.sender, source, message, null);
        replyQueue.offer(reply);
    }
}
