import java.lang.reflect.Method;
import java.util.HashMap;

import org.eclipse.emf.common.util.EList;
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

import LDP.Activite;
import LDP.Debut;
import LDP.LDPPackage;
import LDP.Processus;
import LDPparallel.Activite.*;
import LDPparallel.ElementProcessus;
import LDPparallel.LDPparallelPackage;

public class LDPParEngine {

	String fn;
	Object bs;
	HashMap<String, Object> tags;
	
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
		//on cgarge le modèle
		Processus model = this.getPorcessusModel(fileName);
		//récupération du debut 
		Debut deb = model.getDebut();
		//recuperation de la première activité
		ElementProcessus a = (ElementProcessus) deb.getReference();
		System.out.println(""+  a.getClass().getName());
		
		boolean fini = false;
		
	}
	
	public Processus getPorcessusModel(String file) {
		Resource res = this.chargerModele(file, LDPparallelPackage.eINSTANCE);
		if(res == null) {
			System.err.println("ERREUR chargement du modèle: ");
		}
		
		TreeIterator it = res.getAllContents();

		Processus proc = null;
		while(it.hasNext()) {
		   EObject obj = (EObject) it.next();
		   if (obj instanceof Processus) {
		      proc = (Processus)obj;
		      break;
		   }
		}
		
		return proc;
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
		      System.err.println("ERREUR chargement du modèle : "+e);
		      e.printStackTrace();
		   }
		   return resource;
		}
	
	public static void main(String argv[]) {
		HashMap<String, Object> tags = new HashMap<String, Object>();
		tags.put("n", 6);
		tags.put("x1", 4);
		tags.put("x2", 3);
		tags.put("x3", 7);
		tags.put("x4", 4);
		Calcul calc = new Calcul();
		LDPParEngine engine = new LDPParEngine();
		engine.execute("models/CalculPar.xmi", calc, tags);
		System.out.println("Le résultat vaut:" + tags.get("resAdd2"));
		System.out.println("j'ai fini");
	}
}
