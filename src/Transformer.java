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
		new_act.setDescription(act.getDescription());
		return new_act;
	}
	
	public LDP.Activite getPremiereActivite(LDP.Processus p){
		
		return p.getDebut().getReference();
		
	}
	
	public boolean memeActivite(LDPparallel.Activite a1, LDPparallel.Activite a2) {
		if(a1.getDescription().equals(a2.getDescription())) {
			return true;
		}
		else return false;
	}
	
	public void transform(String basename, String endname) {
		
		// Operation de base (x1 + y1 + z1) * (x2 - y2)
		
		System.out.println(" Chargement du modèle");
		System.out.println(" ");
		
		LDP.Processus proc = LDP.LDPFactory.eINSTANCE.createProcessus();
		proc = this.getProcessusInModel(basename);

		//LDPparallel.Processus new_proc = LDPparallel.LDPparallelFactory.eINSTANCE.createProcessus();
		//EList<LDP.Activite> list_act = proc.getActivites();
		
		System.out.println("Liste Activités :");
		System.out.println(proc.getActivites().toString());
		
		List<LDP.Activite> ordered_list = new ArrayList<LDP.Activite>();
		
		LDPparallel.Activite new_act = LDPparallel.LDPparallelFactory.eINSTANCE.createActivite();
		new_act = copyActivite( getPremiereActivite(proc) );
		
		System.out.println(" ");
		System.out.println("Premiere Activité :");
		System.out.println(new_act.toString());
		
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
		
		System.out.println(" ");
		System.out.println("Liste ordonnée :");
		System.out.println(ordered_list.toString());
		
		List<LDPparallel.Sequence> seq_list = new ArrayList<LDPparallel.Sequence>();
		LDPparallel.Sequence new_seq; 
		
		System.out.println(" ");
		System.out.println("Creation Liste Sequence vide:");
		for(int i=0; i<ordered_list.size(); i++) {
			new_seq = LDPparallel.LDPparallelFactory.eINSTANCE.createSequence();
			seq_list.add(new_seq);
		}
		System.out.println("seq_list size : " + seq_list.size());
		
		
		System.out.println(" ");
		System.out.println("Rangement en sequence :");
		// Mettre la 1ere activité dans la 1ere sequence
		//seq_list.get(0).getActivites().add( copyActivite(ordered_list.get(0)) );
		
		// Mettre les activités dans des sequences differentes
		List<Integer> index_precedents = new ArrayList<Integer>();
		for(int i=0; i<ordered_list.size(); i++) { // Parcours des activités MIN = 0,  MAX = nombre d'activités
			
			System.out.println(ordered_list.get(i).getDescription());
			
			int nb_precedents = 0;
			index_precedents.clear();
			
			for(int j=0; j<i; j++) { // Parcours des activités déja traitées MIN = 0,  MAX = nombre d'activités
					
				for(int k=0; k<ordered_list.get(i).getAction().getParamsTag().size(); k++) {
						
					// Si le tag retour de l'activité k de la sequence j egal au tag param l de l'activité en cours
					if( ordered_list.get(j).getAction().getReturnTag().equals( ordered_list.get(i).getAction().getParamsTag().get(k) ) ) {
						nb_precedents++;
						index_precedents.add(j);
					}

				}		
			}
			
			if(nb_precedents == 1) { // Inserer dans la bonne sequence
				for(int s=0; s<i; s++) { // Parcours de toutes les sequences potentiellement non vides
					for(int t=0; t<seq_list.get(s).getActivites().size(); t++) {
						if( memeActivite(seq_list.get(s).getActivites().get(t), copyActivite(ordered_list.get(index_precedents.get(0))) ) ) { // Si l'activité correspond a l'activité precedente
							seq_list.get(s).getActivites().add( copyActivite(ordered_list.get(i)) ); // Ajout de la nouvelle activité dans la sequence
						}
					}
				}
			}
			else { // Inserer dans la 1ere sequence vide
				for(int s=0; s<(i+1); s++) {
					if(seq_list.get(s).getActivites().isEmpty()) {
						seq_list.get(s).getActivites().add( copyActivite(ordered_list.get(i)) ); // Insertion dans la sequence vide
						break;
					}
				}
			}
		}
		
	
		// Le test pour voir si les activités sont biens rangées
		System.out.println(" ");
		for(int i=0; i<seq_list.size(); i++) {
			System.out.print("Sequence" + i +" -> ");
			for(int j=0; j<seq_list.get(i).getActivites().size(); j++) {
				System.out.print(seq_list.get(i).getActivites().get(j).getDescription() + ", ");
			}
			System.out.println(" ");
		}
		
		
		//System.out.println(" Sauvegarde du modèle");
		//this.sauverModele(endname, proc);
		
	}
	
	public static void main(String argV[]) {
		Transformer t = new Transformer();
		t.transform("models/Calcul_seq.xmi", "models/Calcul_par.xmi");
	}
	
}
