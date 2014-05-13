public class Driver {
    public static void main(String[] args) {
        Tweetable src = new FakeTwitter();
        for(Tweetable.Tweet t : src.getTweets()) {
            System.out.println("@" + t.sender.handle + ": " + t.message);
        }
    }
}
