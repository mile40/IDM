import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import LDPparallel.*;

public class LDPParEngine {

	String fn;
	Object bs;
	public static HashMap<String, Object> tags;
	public static int i;
	private Calcul calc;
	
	public Object dynamicInvoke(String methodName, Object target, Object params[]) throws Exception {
		   Class cl = target.getClass();
		   Class[] paramsClass = new Class[params.length];
		   for (int i=0; i < params.length; i++)
		      paramsClass[i] = params[i].getClass();
		   Method met = cl.getMethod(methodName, paramsClass);
		   Object result = met.invoke(target, params);
		   return result;
		}
	
	public HashMap<String, Object>execSeq(Sequence s, HashMap<String, Object> tags){
		
		LDPparallel.Activite a = s.getPremiereActivite();
		boolean fini = false;
		//tant qu'on est pas a la fin 
		while(!fini) {
				System.out.println("ecexution: "+a.getAction().toString());
				//récupération des paramsTags de la fonctions a executer
				EList<String> pt = a.getAction().getParamsTag();
				//initialisation des paramètres aux entrées de la HashMap correspondante
				Object params[] = new Object[pt.size()];
				for(int j = 0; j<pt.size(); j++) {
					params[j] = tags.get(pt.get(j));
				}
				try {
					//on exécute la méthode 
					Object o = this.dynamicInvoke(a.getAction().getMethodName(), calc, params);
					//on rajoute le returnTag à la HashMap
					tags.put(a.getAction().getReturnTag(), o);		
					if (a.getSuivante() == null) {
						//si c'est la dernière activité on termine le calcul 
						fini = true;
					}else {
						//sinon on passe a  l'activité suivante et on recommence
						a = a.getSuivante();
					}
				} catch (Exception e) {
					e.printStackTrace();
					fini = true;
				}
		}	
		return tags;
	}
	
	public boolean checkEntries(List<String> tags, HashMap<String, Object> map) {
		for(int i = 0; i < tags.size(); i++) {
			if(!map.containsKey(tags.get(i))) {
				return false;
			}
		
		}
		return true;
	}
	public HashMap<String, Object> execute(String fileName, Calcul calc, HashMap<String, Object>  tags ) {
		HashMap<String, Object>  temp = tags;
		//on charge le modèle
		Processus model = this.getPorcessusModel(fileName);
		EList<Sequence> seqList = model.getSequences();
		for(int i = 0; i < seqList.size(); i++) {
			this.i = i;
			System.out.println("je commence le thread");
			Thread t = new Thread() {
				public void run() {
					while(!checkEntries(seqList.get(LDPParEngine.i).getPremiereActivite().getAction().getParamsTag(), LDPParEngine.tags)) {
						System.out.println("j'attent ntm");
					}
					LDPParEngine.tags = execSeq(seqList.get(LDPParEngine.i), temp);
					System.out.println("je finis le thread");
				}
			};
			t.start();
		}
		
		
		tags = temp;
		return tags;
		
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
	
	public void setCalcul(Calcul calc) {
		this.calc = calc;
	}
	
	public Calcul getCalcul() {
		return this.calc;
	}
	public static void main(String argv[]) {
		HashMap<String, Object> tags = new HashMap<String, Object>();
		tags.put("n", 7);
		tags.put("x1", 1);
		tags.put("x2", 7);
		tags.put("x3", 6);
		tags.put("x4", 3);
	
		LDPParEngine engine = new LDPParEngine();
		engine.setCalcul(new Calcul());
		engine.execute("models/CalculPar.xmi", engine.getCalcul(), tags);
		System.out.println("Le résultat vaut:" + tags.get("resAdd2"));
		System.out.println("j'ai fini");
	}
}
