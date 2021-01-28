import java.lang.reflect.Method;
import java.util.HashMap;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.XMLResource.XMLMap;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLMapImpl;
import LDP.LDPPackage;

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
		Resource res = this.chargerModele(fileName, LDPPackage.eINSTANCE);
		
	}
	
	
	public Resource chargerModele(String uri, EPackage pack) {
		   Resource resource = null;
		   try {
		      URI uriUri = URI.createURI(uri);
		      Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		      resource = (new ResourceSetImpl()).createResource(uriUri);
		      XMLResource.XMLMap xmlMap = new XMLMapImpl();
		      xmlMap.setNoNamespacePackage(pack);
		      java.util.Map options = new java.util.HashMap();
		      options.put(XMLResource.OPTION_XML_MAP, xmlMap);
		      resource.load(options);
		   }
		   catch(Exception e) {
		      System.err.println("ERREUR chargement du mod�le : "+e);
		      e.printStackTrace();
		   }
		   return resource;
		}
	
	public static void main() {
		HashMap<String, Object> tags = new HashMap<String, Object>();
		tags.put("n", 6);
		tags.put("puiss", 3);
		tags.put("x", 100);
		Calcul calc = new Calcul();
		LDPEngine engine = new LDPEngine();
		engine.execute("models/Calcul.xmi", calc, tags);
		System.out.println("Le r�sultat vaut:" + tags.get("resDiv"));
	}
}
