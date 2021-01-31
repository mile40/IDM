import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;


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


import LDPparallel.*;

public class LDPParEngine {
	private Processus process = null;
	private ConcurrentHashMap<Integer, Thread> threadMap = new ConcurrentHashMap<>();
	private List<Integer> jonctions = new CopyOnWriteArrayList<>();
	private Object lock = new Object();
	private AtomicBoolean fini = new AtomicBoolean(false);
	
	public void execute(Processus model, Object target, HashMap<String, Object> tags) {
		this.process = model;
		ElementProcessus firstElem = model.getDebut().getReference();
		
		try {
			handleElt(firstElem, tags, target);
			
			while(!fini.get()) {
				Thread.sleep(100);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void handleElt(ElementProcessus elt, HashMap<String, Object> tags, Object target) throws Exception {
		if(elt instanceof Fin) {
			fini.set(true);
			return;
		}
		if(elt instanceof Sequence || elt instanceof Porte) {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						//si notre élément est une Fourche
						if(elt instanceof Fourche) {
							Fourche f = (Fourche) elt;
							
							for(ElementProcessus e: f.getSucc()) {
								handleElt(e, tags, target);
							}
							//si c'est une jonction
						}else if(elt instanceof Jonction) {
							Jonction j = (Jonction) elt;
							
							synchronized(lock) {
								if(!jonctions.stream().anyMatch(e -> e == j.hashCode())) {
									jonctions.add(j.hashCode());
								}else {
									return;
								}
								
								for(ElementProcessus e: j.getPred()) {
									threadMap.get(e.hashCode()).join();
									handleElt(j.getSucc(), tags, target);
								}
							}
							//dans tous les autres cas
						}else if (elt instanceof Sequence) {
							Sequence s = (Sequence) elt;
							Activite a = s.getPremiereActivite();
							s.setPremiereActivite(a);
							execSeq(s, target, tags);
							//on détermine quelle porte suit
							for(Porte p: process.getPortes()) {
								//si la porte suivante est une jonction
								if(p instanceof Jonction && ((Jonction)p).getPred().stream().anyMatch(e->e==s) ) {
									handleElt(p, tags, target);
								}else if(p instanceof Fourche && ((Fourche)p).getPred() == s) {
									handleElt(p, tags, target);
								}
									
							}
							//on appelle récursivement sur le successeur 
							 if(process.getFin().getReference() == s){
	                        		handleElt(process.getFin(), tags, target); 
	                         }
						}
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
				
			});
			threadMap.put(elt.hashCode(), t);
			t.start();
		}

		
	}
	
	public void execSeq(Sequence s, Object calc, HashMap<String, Object>  tags ) {
		
		Activite a = s.getPremiereActivite();
		boolean fini = false;	
		//tant qu'on est pas a la fin 
		while(!fini) {
				System.out.println("ecexution: "+a.getAction().toString());
	
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
	


}
