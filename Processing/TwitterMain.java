import java.util.Date;
import java.util.Queue;

public class TwitterMain {
	public static void main(String[] args) throws InterruptedException {
		String seperator = (new String(new char[45])).replace('\0', '~');
		
		int initialDelay = 2 * 1000;
		int refreshInterval = 60 * 1000;
		boolean checkingFeed = true;
		boolean sendSampleReply = false;
		
		// only process API requests every 60 seconds,
		// we're rate limited @ 15 requests per 15 minutes so..
		TwitterCommunicator tc = new TwitterCommunicator(initialDelay, refreshInterval);
		
		if (sendSampleReply) {
			// sample reply to a real Tweet
			// note this will fail at the moment because I've already run it once!
			// delete the @reply and it will be demo-able again
			Tweetable.User sender = new Tweetable.User("AwerduinoTest", 
					"https://abs.twimg.com/sticky/default_profile_images/default_profile_2_200x200.png");
			
			Tweetable.Tweet source = new Tweetable.Tweet(sender, 
					"@Awerduino I really hope this works...", 
					new Date("Sun May 04 04:38:34 +0000 2014"), 
					"462813511484444672");
				
			tc.reply("...it works!", source);
		}
		
		while(checkingFeed) {
			Thread.sleep(10 * 1000);
			System.out.println("\n" + seperator);
			
			Queue<Tweetable.Tweet> tweets = tc.getTweets();
			Queue<Tweetable.Reply> replies = tc.getReplies();
			
			System.out.println("[Incoming]");
			System.out.println("Queued Tweets: " + (tweets == null ? 0 : tweets.size()));
			for (Tweetable.Tweet t : tweets) {
				System.out.println("\t->" + t.sender.handle + ": " + t.message);
				
				// EXAMPLE: parse incoming Tweets for keywords, @reply the sending users
				if (t.message.toLowerCase().indexOf("marco") > 0) {
					tc.reply("Polo!", t);
				} else {
					// do something else...
				}
			}
			
			System.out.println("[Outgoing]");
			System.out.println("Queued Replies: " + (replies == null ? 0 : replies.size()));
			
			for (Tweetable.Reply r : replies) {
				System.out.println("\t->Aweruino: " + r.message);
			}
			System.out.println(seperator);
		}
	}
}
