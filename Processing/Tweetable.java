import java.util.Date;
import java.util.Queue;

public interface Tweetable {

	/**
	 * getTweets() dequeues and receives all @mentions, since last checking.
	 *
	 * @return a generic List of Tweet objects.
	 */
	public Queue<Tweet> getTweets();

	/**
	 * reply() enqueues an @reply in response to a user's query.
	 *
	 * @param message
	 * 		- the 140 character response message.
	 * @param source
	 * 		- the Tweet object invoking this response.
	 */
	public void reply(String message, Tweet source);

	// Metadata about the sender of a Tweet.
	public class User {
		public String handle;
		public String avatarURL;

		public User(String handle, String avatarURL) {
			this.handle = handle;
			this.avatarURL = avatarURL;
		}
	}

	// Incoming messages from Twitter.
	public class Tweet {
		public User sender;
		public String message;
		public Date timestamp;
		public String tweetId;

		public Tweet(User sender, String message, Date timestamp, String tweetId) {
			this.sender = sender;
			this.message = message;
			this.timestamp = timestamp;
			this.tweetId = tweetId;
		}

		/**
		 * toString() prepares a textual representation of the Tweet.
		 *
		 * @return a pretty String.
		 */
		public String toString() {
			String tweetTime = Long.toString(timestamp.getTime());
			String tweetBody = sender.handle + ": " + message;
			String border = new String(new char[tweetBody.length()]).replace('\0', '#');

			return border + "\n" +
					tweetTime + "\n" +
					tweetBody + "\n" +
					border;
		}
	}

	// Outgoing messages to Twitter.
	public class Reply {
		public User recipient;
		public Tweet source;
		public String message;
		public String imageURL;

		public Reply(User recipient, Tweet source, String message, String imageURL) {
			this.recipient = recipient;
			this.source = source;
			this.message = message;
			this.imageURL = imageURL;
		}
	}
}