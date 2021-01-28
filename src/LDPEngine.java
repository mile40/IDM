import java.util.HashMap;

public class LDPEngine {
	String fn;
	Object bs;
	HashMap<String, Object> tags;
	public void engine(String fileName, Object business, HashMap<String, Object> tags) {
		this.tags = tags;
		this.fn = fileName;
		this.bs = business;
		
	}
}
