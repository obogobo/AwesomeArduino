import processing.core.PApplet;
import processing.serial.Serial;
import processing.video.Capture;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Queue;

/**
 * Sketch: The Processing Sketch.
 *
 * Since Processing's IDE is stupid beyond comprehension,
 * and their "compiling" process is a pain, I'm writing
 * the whole sketch in Java.
 *
 * Extending PApplet is also a big pain. So this hacky
 * solution with registerMethod and such.
 *
 * Then the PDE file is just:
 * setup() { Sketch.fromApplet(this); }
 *
 * Boom. Done.
 */
@SuppressWarnings("unused") // Processing's PDE file uses this.
public class Sketch {

    private static boolean FAKE_MODE = false;

    private final static String DROPBOX_PUBLIC_URL = "https://dl.dropboxusercontent.com/u/4434736/";
    private final static File DROPBOX_PATH;

    static {
        if (FAKE_MODE)
            DROPBOX_PATH = new File("/tmp");
        else
            DROPBOX_PATH = new File("/Users/jcw21/Dropbox/Public");
    }

    private final static Random random = new Random();
    private final static String[] methods = {
            "pre",
            "draw",
            "post",
            "pause",
            "resume",
            "dispose"
    };
    private final static String[] errorMessages = new String[]{
            "I don't know what you mean",
            "I'm sorry. Could you be a little less foggy?",
    };
    private final static String[] lookAtMe = new String[]{
            "Look at me!",
    };
    private final static String twitterHandle = "@awerduino";
    private final PApplet applet;
    private final Dictionary<String, ActionMatcher.MatchedAction<String>> actions = new Hashtable<String, ActionMatcher.MatchedAction<String>>();
    private final List<ActionMatcher<String, String>> matcherList = new LinkedList<ActionMatcher<String, String>>();
    private Capture webcam;
    private Serial port;
    private Tweetable twitter;
    private Queue<Tweetable.Tweet> queuedTweets;

    private Sketch(PApplet source) {
        applet = source;
        for (String method : methods)
            applet.registerMethod(method, this);
        setup();
    }

    public static String randomFormat(String[] templates, Object... args) {
        String template = templates[random.nextInt(templates.length)];
        return String.format(template, args);
    }

    @SuppressWarnings("unused") // Processing's PDE file uses this.
    public static Sketch fromApplet(PApplet applet) {
        return new Sketch(applet);
    }

    private void addTweetPattern(String pattern, String action) {
        matcherList.add(new ActionMatcher<String, String>(pattern, actions.get(action)));
    }

    @SuppressWarnings("unused") // Processing's PDE file uses this.
    public void draw() {
        if (queuedTweets == null || queuedTweets.size() == 0)
            queuedTweets = new LinkedList<Tweetable.Tweet>(twitter.getTweets());
        else
            processTweet(queuedTweets.remove());
    }

    public String arduinoCommand(String string) {
        if (FAKE_MODE) {
            System.out.println("[ARD] " + string);
            switch (string.charAt(0)) {
                case 'T':
                case 'H':
                    return random.nextFloat() + "";
                case 'F':
                    return random.nextBoolean() ? "Y" : "N";
                default:
                    return "**";
            }
        } else {
            port.write(string);
            while (port.available() <= 0) {
               try {
                 System.out.println(string); 
                 System.out.println("Waiting for response");
                 Thread.sleep(1 * 200);
               } catch (InterruptedException e) {
                ;
               } 
            }
            String res = port.readString();
            return (res == null ? "*NULL*" : res);
        }
    }

    public void processTweet(Tweetable.Tweet tweet) {

        System.out.println(tweet);

        boolean doPhoto = false;
        String reply = randomFormat(errorMessages);

        if (!tweet.message.toLowerCase().startsWith(twitterHandle))
            return;

        String tweetMessage = tweet.message.substring(twitterHandle.length() + 1);

        if (tweetMessage.contains(" #selfie")) {
            doPhoto = true;
            tweetMessage = tweetMessage.replace(" #selfie", "");
        }
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        arduinoCommand("@" + tweet.sender.handle + "\n" + sdf.format(tweet.timestamp));

        for (ActionMatcher<String, String> m : matcherList)
            if (m.doesMatch(tweetMessage)) {
                reply = m.doIfMatch(tweetMessage);
                break;
            }

        if (doPhoto) {
            File imageFile = getImage();
            if (imageFile != null) {
                if (reply == null) reply = randomFormat(lookAtMe);
                reply += " " + DROPBOX_PUBLIC_URL + getImage().getName();
            }
        }

        if (reply != null)
            twitter.reply(reply, tweet);

    }

    public File getImage() {
        // Can't save image if webcam has not been initialised.
        if (webcam == null) return null;

        try {
            File imageFile = File.createTempFile("awerduino", "photo.jpg", DROPBOX_PATH);
            if (webcam.available()) webcam.read();
            applet.image(webcam, 0, 0);
            webcam.save(imageFile.getAbsolutePath());
            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setup() {

        if (true) {
            port = new Serial(applet, Serial.list()[5], 9600);
            try {
              Thread.sleep(5 * 1000);
            } catch (Exception e) {;}
        }
        
        String s;
        if ((s = arduinoCommand("INIT")).charAt(0) != '*') {
            System.out.println("## " + s + " ##");
            throw new RuntimeException("No Arduino :(");
        }

        if (FAKE_MODE)
            twitter = new FakeTwitter();
        else
            twitter = new TwitterCommunicator(10, 60 * 1000);

        applet.size(1280, 720);

        webcam = new Capture(applet, 1280, 720);
        webcam.start();

        actions.put("temperature", new ActionMatcher.MatchedAction<String>() {

            private final String[] templates = {
                    "It is %1$.0f degrees fahrenheit",
                    "It is %2$.0f degrees celcius",
                    "It is %2$.0f degrees centigrade",
                    "My sensors inform me that it is %1$.2f F",
                    "My sensors inform me that it is %2$.2f C"
            };

            @Override
            public String matched(Dictionary<String, String> parameters) {
                try {
                    float temp = Float.parseFloat(arduinoCommand("T"));
                    return randomFormat(templates, temp * 1.8f + 32.0f, temp);
                } catch (NumberFormatException e) {
                    // Arduino response is not a float.
                    // Do nothing.
                    return null;
                }
            }
        });

        actions.put("color", new ActionMatcher.MatchedAction<String>() {

            private final String[] templates = {
                    "Are you sure %s is a color?",
                    "I don't know the color %s",
                    "Color %s is not in my standard library",
            };

            @Override
            public String matched(Dictionary<String, String> parameters) {

                Color color = null;
                if (parameters.get("colorname") != null) {
                    color = NamedColors.get(parameters.get("colorname"));
                    if (color == null)
                        return randomFormat(templates, parameters.get("colorname"));
                } else {

                    float[] cvals = new float[3];
                    for (int i = 0; i < 3; ++i) {
                        cvals[i] = Float.parseFloat(parameters.get("c" + i));
                        if (parameters.get("pc" + i) != null) cvals[i] = cvals[i] * 255 / 100;
                        else if (cvals[i] <= 1.0) cvals[i] *= 255;
                        cvals[i] /= 255;
                    }

                    String colorMode = parameters.get("colortype").toLowerCase();

                    if (colorMode.equals("hsb") || colorMode.equals("hsv")) {
                        color = new Color(Color.HSBtoRGB(cvals[0] * 255 / 360, cvals[1], cvals[2]));
                    } else if (colorMode.equals("hsl")) {
                        color = new Color(HSLtoRGB(cvals[0] * 255 / 360, cvals[1], cvals[2]));
                    } else if (colorMode.equals("rgb")) {
                        color = new Color(cvals[0], cvals[1], cvals[2]);
                    }
                }

                if (color != null)
                    arduinoCommand("C " + color.getRed() + " " + color.getGreen() + " " + color.getBlue());

                return null;
            }

            private int HSLtoRGB(float h, float s, float l) {
                float q;
                if (l < 0.5) q = l * (1 + s);
                else q = (l + s) - (s * l);
                float p = 2 * l - q;
                int r = (int)(255 * Math.max(0, HueToRGB(p, q, h + (1.0f / 3.0f))));
                int g = (int)(255 * Math.max(0, HueToRGB(p, q, h)));
                int b = (int)(255 * Math.max(0, HueToRGB(p, q, h - (1.0f / 3.0f))));
                return (255 << 24) + (r << 16) + (g << 8) + (b);
            }

            private float HueToRGB(float p, float q, float h) {
                if (h < 0) h += 1;
                if (h > 1) h -= 1;
                if (6 * h < 1) return p + ((q - p) * 6 * h);
                if (2 * h < 1) return q;
                if (3 * h < 2) return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
                return p;
            }
        });

        actions.put("buzz", new ActionMatcher.MatchedAction<String>() {


            @Override
            public String matched(Dictionary<String, String> parameters) {
                List<Integer> buzzIntervals = new LinkedList<Integer>();
                int count = 3 + random.nextInt(5);
                for (int i = 0; i < count; ++i)
                    buzzIntervals.add(3 + random.nextInt(5));

                StringBuilder arduino = new StringBuilder();
                arduino.append("B ");
                for (Integer buzz : buzzIntervals) {
                    arduino.append(buzz);
                    arduino.append(" ");
                }
                arduinoCommand(arduino.toString());

                StringBuilder tweet = new StringBuilder();
                tweet.append("B");
                for (Integer buzz : buzzIntervals) {
                    if (tweet.length() > 1)
                        tweet.append(" ");

                    for (int i = 0; i < buzz; ++i)
                        tweet.append("z");
                }
                return tweet.toString();
            }
        });

        final Map<String, String> drawables = Drawables.get();

        actions.put("draw", new ActionMatcher.MatchedAction<String>() {

            @Override
            public String matched(Dictionary<String, String> parameters) {
                String toDraw;

                if (parameters.get("raw") != null) {
                    toDraw = parameters.get("raw");
                } else if (parameters.get("named") != null) {
                    toDraw = drawables.get(parameters.get("named"));
                } else {
                    String randomKey = null;
                    Iterator<String> allKeys = drawables.keySet().iterator();
                    int howMany = random.nextInt(drawables.keySet().size());
                    for (int i = 0; i < howMany; ++i)
                        randomKey = allKeys.next();
                    toDraw = drawables.get(randomKey);
                }

                toDraw = toDraw.replace(".", "0").replace("#", "1");
                arduinoCommand(toDraw);

                return null;
            }
        });

        actions.put("humid", new ActionMatcher.MatchedAction<String>() {

            @Override
            public String matched(Dictionary<String, String> parameters) {
                try {
                    float humid = Float.parseFloat(arduinoCommand("H"));
                    String condition = "dry";
                    if(humid > .2)
                        condition = "mildly humid";
                    if(humid > .4)
                        condition = "very humid";
                    if(humid > .8)
                        condition = "Florida!";
                    return "It's " + condition;
                } catch (NumberFormatException e) {
                    // Arduino response is not a float.
                    // Do nothing.
                    return null;
                }
            }
        });

        addTweetPattern("How (warm|cold|chilly|hot|cool) is it\\??", "temperature");
        addTweetPattern("What('s| is) the temp(erature)?\\??", "temperature");

        addTweetPattern("Show me (the )?color (?<colorname>\\w+)[\\.\\!]?", "color");
        addTweetPattern("Show me ((the )?color )?(?<colortype>hsl|rgb|hsv)\\s*\\(\\s*(?<c0>[\\d\\.]+)\\s*(?<pc0>%)?\\s*,\\s*(?<c1>[\\d\\.]+)\\s*(?<pc1>%)?\\s*,\\s*(?<c2>[\\d\\.]+)\\s*(?<pc2>%)?\\s*\\)[\\.\\!]?", "color");
        addTweetPattern("Show me ((the )?color )?(?<colortype>hsl|hsv)\\s*\\(\\s*(?<c0>[\\d\\.]+?)\\s*deg\\s*,\\s*(?<c1>[\\d\\.]+)\\s*(?<pc1>%)?\\s*,\\s*(?<c2>[\\d\\.]+)\\s*(?<pc2>%)?\\s*\\)[\\.\\!]?", "color");

        addTweetPattern("Make some noise[\\.\\!+]?", "buzz");
        addTweetPattern("Buzz+[\\.\\!+]?", "buzz");
        addTweetPattern("What do bees sound like\\??", "buzz");
        addTweetPattern("(\\s*Bee+s[\\.\\!+]?)+", "buzz");

        addTweetPattern("How humid is it\\??", "humid");

        addTweetPattern("Show me (?<raw>[01\\.#]{64})[\\.\\!+]?", "draw");

        StringBuilder namedString = new StringBuilder();
        for (String key : drawables.keySet()) {
            if (namedString.length() > 0)
                namedString.append("|");
            namedString.append(key);
        }
        addTweetPattern("Show me ((an?|the) )?(?<named>" + namedString + ")[\\.\\!+]?", "draw");

        applet.loop();
    }

    @SuppressWarnings("unused") // Processing's PDE file uses this.
    public void pre() {

    }

    @SuppressWarnings("unused") // Processing's PDE file uses this.
    public void post() {

    }

    @SuppressWarnings("unused") // Processing's PDE file uses this.
    public void pause() {

    }

    @SuppressWarnings("unused") // Processing's PDE file uses this.
    public void resume() {

    }

    @SuppressWarnings("unused") // Processing's PDE file uses this.
    public void dispose() {

    }
}
