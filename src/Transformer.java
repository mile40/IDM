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

import LDP.*;
import LDP.LDPPackage;

import LDPparallel.LDPparallelPackage;
import LDPparallel.*;

public class Transformer {
	
	public void sauverModele(String uri, EObject root) {
		Resource resource = null;
		try {
			URI uriUri = URI.createURI(uri);
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
			resource = (new ResourceSetImpl()).createResource(uriUri);
			resource.getContents().add(root);
			resource.save(null);
		} catch (Exception e) {
			System.err.println("ERREUR sauvegarde du modèle : "+e);
			e.printStackTrace();
		}
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

	public LDP.Processus getProcessusInModel(String modelFile) {
        Resource resource = chargerModele(modelFile, LDPPackage.eINSTANCE);
        if (resource == null) {
            System.err.println(" Erreur de chargement du modèle");
            return null;
        }

        TreeIterator it = resource.getAllContents();

        LDP.Processus proc = null;
        while(it.hasNext()) {
           EObject obj = (EObject) it.next();
           if (obj instanceof LDP.Processus) {
              proc = (LDP.Processus) obj;
              break;
           }
        }
        return proc;
    }
	
	public LDPparallel.Activite copyActivite(LDP.Activite act){
		LDPparallel.Activite new_act = LDPparallel.LDPparallelFactory.eINSTANCE.createActivite();
		LDPparallel.Operation new_op = LDPparallel.LDPparallelFactory.eINSTANCE.createOperation();
		new_op.setMethodName(act.getAction().getMethodName());
		new_op.setReturnTag(act.getAction().getReturnTag());
		new_op.getParamsTag().addAll(act.getAction().getParamsTag());
		new_act.setAction(new_op);
		return new_act;
	}
	
	public LDP.Activite getPremiereActivite(LDP.Processus p){
		
		return p.getDebut().getReference();
		
	}
	
	public void transform(String basename, String endname) {
		
		// (x + y) * (z - a)
		
		System.out.println(" Chargement du modèle");
		System.out.println(" ");
		
		LDP.Processus proc = this.getProcessusInModel(basename);

		//LDPparallel.Processus new_proc = LDPparallel.LDPparallelFactory.eINSTANCE.createProcessus();
		//EList<LDP.Activite> list_act = proc.getActivites();
		
		List<LDP.Activite> ordered_list = new ArrayList<LDP.Activite>();
		
		LDPparallel.Activite new_act = LDPparallel.LDPparallelFactory.eINSTANCE.createActivite();
		new_act = copyActivite( getPremiereActivite(proc) );
		
		// Ranger les activités dans l'ordre d'execution
		LDP.Activite a = getPremiereActivite(proc);
		boolean fini = false;
		while(!fini) {
			ordered_list.add(a);
			if(a.getSuivante() == null) {
				fini = true;
			}
			else {
				a = a.getSuivante();
			}
		}
		
		// Mettre les activités dans des sequences differentes
		List<LDPparallel.Sequence> seq_list = new ArrayList<LDPparallel.Sequence>();
		LDPparallel.Sequence new_seq; 
		for(int i=0; i<ordered_list.size(); i++) {
			
			for(int j=0; j<seq_list.size(); j++) {
				
				if(seq_list.size() == 0) {
					new_seq = LDPparallel.LDPparallelFactory.eINSTANCE.createSequence();
					new_seq.getActivites().add( copyActivite(ordered_list.get(i)) );
					seq_list.add(new_seq);
				}
				else {
					for(int k=0; k<seq_list.get(j).getActivites().size(); k++) {
						
						for(int l=0; l<seq_list.get(j).getActivites().get(k).getAction().getParamsTag().size(); l++) {
							
							if( !seq_list.get(j).getActivites().get(k).getAction().getParamsTag().get(l).equals( ordered_list.get(i).getAction().getReturnTag() ) ) {
								new_seq = LDPparallel.LDPparallelFactory.eINSTANCE.createSequence();
								new_seq.getActivites().add( copyActivite(ordered_list.get(i)) );
								seq_list.add(new_seq);
							}
							else {
								seq_list.get(j).getActivites().add( copyActivite(ordered_list.get(i)) );
							}
						}
					}
				}			
			}
		}
		
		
		// Le test
		for(int i=0; i<seq_list.size(); i++) {
			System.out.print("Sequence" + i +" -> ");
			for(int j=0; j<seq_list.get(i).getActivites().size(); j++) {
				System.out.print("A" + j +", ");
			}
			System.out.println(" ");
		}
		
		
		//System.out.println(" Sauvegarde du modèle");
		//this.sauverModele(endname, proc);
		
	}
	
	public static void main(String argV[]) {
		Transformer t = new Transformer();
		t.transform("models/Calcul.xmi", "models/Calcul-par.xmi");
	}
	
}
