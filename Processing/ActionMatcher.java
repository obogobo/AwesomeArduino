import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionMatcher<E, V extends String> {

	private static Pattern groupPattern = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");
	private Pattern pattern;
	private Set<String> namedGroups;
	private MatchedAction<E> action;
	private Matcher matcher;

	public ActionMatcher(String pattern, MatchedAction<E> action) {
		this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		this.action = action;
		namedGroups = new TreeSet<String>();
		Matcher m = groupPattern.matcher(pattern);
		while (m.find())
			namedGroups.add(m.group(1));
	}

	public boolean doesMatch(V input) {
		matcher = pattern.matcher(input);
		return matcher.matches();
	}

	public E doIfMatch(V input) {
		if (!doesMatch(input)) return null;
		Dictionary<String, String> params = new Hashtable<String, String>();
		for (String name : namedGroups)
			if (matcher.group(name) != null)
				params.put(name, matcher.group(name));
		return this.action.matched(params);
	}

	public static abstract class MatchedAction<E> {
		public abstract E matched(Dictionary<String, String> parameters);
	}
}
