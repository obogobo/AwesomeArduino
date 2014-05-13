// This class provides Fake Tweets for testing the Processing sketch.

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.*;

public class FakeTwitter implements Tweetable {

	private static Random random = new Random();

	private static Dictionary<Charset, String> allLettersMap = new Hashtable<Charset, String>();

	private static List<String> tweetTemplates = new ArrayList<String>();
	private static Dictionary<String, List<String>> tweetGroups = new Hashtable<String, List<String>>();

	private static void addTweetGroup(String groupName, String... items) {
		List<String> groupItems = tweetGroups.get(groupName);
		if (groupItems == null) {
			groupItems = new ArrayList<String>(items.length);
			tweetGroups.put(groupName, groupItems);
		}
		Collections.addAll(groupItems, items);
	}

	static {

		tweetTemplates.add("Show me {drawable}");
		tweetTemplates.add("Show me the color {colorname}");
		tweetTemplates.add("Show me color {colorname}");
		tweetTemplates.add("Show me the color {colorpattern}");
		tweetTemplates.add("Show me {colorpattern}");
		tweetTemplates.add("How {temperature-adjective} is it?");
		tweetTemplates.add("Beeeees!");
		tweetTemplates.add("Buzzzzzzz!");

		addTweetGroup("drawable",
				"a heart",
				"an android",
				"the arduino logo",
				"a circle",
				"a square",
				"a triangle",
				"a shape"
		);

		addTweetGroup("temperature-adjective",
				"hot",
				"cold",
				"warm",
				"chilly",
				"cool"
		);

		addTweetGroup("colorname",
				"red",
				"Green",
				"BLUE",
				"orange",
				"YeLlOw",
				"MaRoOn"
		);

		addTweetGroup("colorpattern",
				"rgb(100,100,100)",
				"rgb(100%, 100%, 100%)",
				"rgb(255, 255, 0)",
				"hsl(360deg, 0, 10%)",
				"hsv(10deg, 0.5, 0.5)"
		);
	}

	private static String allLetters(Charset charset) {
		if (allLettersMap.get(charset) == null) {
			CharsetEncoder ce = charset.newEncoder();
			StringBuilder result = new StringBuilder();
			for (char c = 0; c < Character.MAX_VALUE; c++)
				if (ce.canEncode(c) && Character.isLetter(c))
					result.append(c);
			allLettersMap.put(charset, result.toString());
		}
		return allLettersMap.get(charset);
	}

	private String randomString(Charset charset, int length) {
		String source = allLetters(charset);
		return randomString(source, length);
	}

	private String randomString(String source, int length) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < length; ++i) {
			int position = random.nextInt(source.length());
			result.append(source.charAt(position));
		}
		return result.toString();
	}

	private String randomString(int length) {
		return randomString(Charset.forName("US-ASCII"), length);
	}

	private Tweetable.User fakeUser() {
		String handle = randomString(random.nextInt(10) + 5);
		String avatarURL = "http://www.gravatar.com/avatar/" + randomString("abcdef0123456789", 16) + "?d=retro";
		return new User(handle, avatarURL);
	}

	private Tweet randomTweet() {
		User sender = fakeUser();
		Date timestamp = new Date();

		String randTweet = tweetTemplates.get(random.nextInt(tweetTemplates.size()));
		Enumeration<String> groups = tweetGroups.keys();
		String group;
		do {
			group = groups.nextElement();
			List<String> elems = tweetGroups.get(group);
			String randElem = elems.get(random.nextInt(elems.size()));
			randTweet = randTweet.replace("{" + group + "}", randElem);
		} while (groups.hasMoreElements());

		randTweet = "@Awerduino " + randTweet + (random.nextBoolean() ? " #selfie" : "");
		return new Tweet(sender, randTweet, timestamp, random.nextInt() + "");
	}

	public Queue<Tweet> getTweets() {
		Queue<Tweet> randomTweets = new LinkedList<Tweet>();
		int howMany = random.nextInt(2);
		while (howMany-- > 0) {
			randomTweets.add(randomTweet());
		}
		return randomTweets;
	}

	@Override
	public void reply(String contents, Tweet source) {
		try {
			// Log tweets to file
			FileWriter twitterOut = new FileWriter("tweets.txt", true);

			// Format them nicely
			StringBuffer tweet = new StringBuffer();
			tweet.append("--- NEW TWEET ---\nSent: ");
			tweet.append(new Date().toString());
			tweet.append("\n@");
			tweet.append(source.sender.handle);
			tweet.append(" ");
			tweet.append(contents);
			tweet.append("\n--- IN REPLY TO ---\n");
			tweet.append(source.message);
			tweet.append("\n--- END TWEET ---\n\n");

			twitterOut.append(tweet);
			twitterOut.close();

		} catch (IOException e) {
			// Meh.
		}
	}

}
