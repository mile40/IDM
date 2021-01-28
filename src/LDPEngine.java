import java.lang.reflect.Method;
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
	
	public Object dynamicInvoke(String methodName, Object target, Object params[]) throws Exception {
		   Class cl = target.getClass();
		   Class[] paramsClass = new Class[params.length];
		   for (int i=0; i < params.length; i++)
		      paramsClass[i] = params[i].getClass();
		   Method met = cl.getMethod(methodName, paramsClass);
		   Object result = met.invoke(target, params);
		   return result;
		}
	
	public void execute(String fileName, Calcul calc, HashMap<String, Object>  tags ) {
		//TODO: TAMER
	}
	
	public static void main() {
		
	}
	
	
}
