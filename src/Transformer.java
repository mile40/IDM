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
	
	public boolean memeSequence(LDPparallel.Sequence s1, LDPparallel.Sequence s2) {
		if(s1.getActivites().get(0).getDescription().equals(s2.getActivites().get(0).getDescription())) {
			return true;
		}
		else return false;
	}
	
	public boolean memeActivite(LDPparallel.Activite a1, LDPparallel.Activite a2) {
		if(a1.getDescription().equals(a2.getDescription())) {
			return true;
		}
		else return false;
	}
	
	public LDPparallel.Porte getPortePrecFromSequence(LDPparallel.Sequence to_test, LDPparallel.Sequence s, List<LDPparallel.Porte> lp){
		for(LDPparallel.Porte new_p : lp) {
			if(new_p.getClass() == LDPparallel.Fourche.class) {
				
			}
		}
		return null;
	}
	
	public LDPparallel.Porte getPorteSuivFromSequence( LDPparallel.Sequence s, List<LDPparallel.Porte> lp){
		for(LDPparallel.Porte new_p : lp) {
			if(new_p.getClass() == LDPparallel.Fourche.class) {
				
			}
		}
		return null;
	}
	
	public void transform(String basename, String endname) {
		
		// Operation de base (x1 + y1 + z1) * (x2 - y2)
		
		System.out.println(" Chargement du modèle");
		System.out.println(" ");
		
		LDP.Processus proc = LDP.LDPFactory.eINSTANCE.createProcessus();
		proc = this.getProcessusInModel(basename);

		LDPparallel.Processus new_proc = LDPparallel.LDPparallelFactory.eINSTANCE.createProcessus();
		
		List<LDP.Activite> ordered_list = new ArrayList<LDP.Activite>();
		
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
		
		List<LDPparallel.Sequence> seq_list = new ArrayList<LDPparallel.Sequence>();
		LDPparallel.Sequence new_seq; 
		
		System.out.println(" ");
		System.out.println("Creation Liste Sequence vide");
		for(int i=0; i<ordered_list.size(); i++) {
			new_seq = LDPparallel.LDPparallelFactory.eINSTANCE.createSequence();
			seq_list.add(new_seq);
		}
		
		
		// Mettre les activités dans des sequences differentes
		System.out.println(" ");
		System.out.println("Rangement dans les sequences");
		List<Integer> index_precedents = new ArrayList<Integer>();
		for(int i=0; i<ordered_list.size(); i++) { // Parcours des activités MIN = 0,  MAX = nombre d'activités
			
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
			if(seq_list.get(i).getActivites().isEmpty()) { // Detruit les sequences vides
				seq_list.remove(i);
			}
			else {
				System.out.print("Sequence" + i +" -> ");
				for(int j=0; j<seq_list.get(i).getActivites().size(); j++) {
					System.out.print(seq_list.get(i).getActivites().get(j).getDescription() + ", ");
				}
				System.out.println(" ");
			}
			
		}
		
		
		// On fixe la 1ere activité et on fixe les suivants de chaque sequence
		for(int i=0; i<seq_list.size(); i++) {
			for(int j=0; j<seq_list.get(i).getActivites().size(); j++) {
				if( j == 0 ) { // 1ere activité d'une sequence
					seq_list.get(i).setPremiereActivite(seq_list.get(i).getActivites().get(j));
					if( seq_list.get(i).getActivites().size() > 1 ) { // Si l'activité n'est pas seule dans la liste
						seq_list.get(i).getActivites().get(j).setSuivante(seq_list.get(i).getActivites().get(j+1));
						seq_list.get(i).getActivites().get(j).setPrecedente(null); // TODO les jonctions ou Fin
					}
					else { // Si elle est seule dans la liste
						seq_list.get(i).getActivites().get(j).setSuivante(null); // TODO les jonctions ou Fin ( Derniere activité de la sequence )
						seq_list.get(i).getActivites().get(j).setPrecedente(null); // TODO les fourches ou Fin
					}
				}
				else { // Pas 1ere activité de la sequence
					if( seq_list.get(i).getActivites().size() > j+1 ) { // Si la taille de la sequence est plus grande que la position de l'activité dans la sequence
						seq_list.get(i).getActivites().get(j).setSuivante(seq_list.get(i).getActivites().get(j+1)); // Suivante de l'activité J de la sequence I
					}
					else {
						seq_list.get(i).getActivites().get(j).setSuivante(null); // TODO les jonctions ou Fin ( Derniere activité de la sequence )
					}
					seq_list.get(i).getActivites().get(j).setPrecedente(seq_list.get(i).getActivites().get(j-1)); // Precedente de l'activité J de la sequence I 
				}			
			}
		}
		
		
		// Le test pour voir si les activités se suivent et se precedent bien
		System.out.println(" ");
		for(int i=0; i<seq_list.size(); i++) {
			System.out.println(" <-------- Sequence " + i + " --------> ");
			for(int j=0; j<seq_list.get(i).getActivites().size(); j++) {
				System.out.println(" **** Activité " + seq_list.get(i).getActivites().get(j).getDescription() +" **** ");
				if( seq_list.get(i).getActivites().get(j).getPrecedente() != null ) {
					System.out.println(" Precedente : " + seq_list.get(i).getActivites().get(j).getPrecedente().getDescription());
				}
				if( seq_list.get(i).getActivites().get(j).getSuivante() != null ) {
					System.out.println(" Suivante : " + seq_list.get(i).getActivites().get(j).getSuivante().getDescription());
				}		
			}
			System.out.println(" ");			
		}
		
		
		// Creation des fourches + jonctions
		
		List<LDPparallel.Porte> porte_list = new ArrayList<LDPparallel.Porte>();
		List<LDPparallel.Sequence> seq_traitees = new ArrayList<LDPparallel.Sequence>();
		List<Integer> index_seq_traitees = new ArrayList<Integer>();
		
		LDPparallel.Debut new_debut = LDPparallel.LDPparallelFactory.eINSTANCE.createDebut();
		LDPparallel.Fin new_fin = LDPparallel.LDPparallelFactory.eINSTANCE.createFin();
		
		
		// Verifier combien de sequences n'ont pas de predecesseurs
		// Pour commencer soit par une fourche soit par une sequence simple
		List<Integer> premieres_seq = new ArrayList<Integer>();
		int nb_seq_pred = 0;
		for(int i=0; i<seq_list.size(); i++) {
			nb_seq_pred = 0;
			LDPparallel.Activite i_act = seq_list.get(i).getPremiereActivite(); // Premiere activité de la sequence I
			for(int j=0; j<seq_list.size(); j++) {
				LDPparallel.Activite j_act = seq_list.get(j).getActivites().get( seq_list.get(j).getActivites().size() - 1); // Derniere activité des sequences J
				for(int k=0; k<i_act.getAction().getParamsTag().size(); k++) {
					// Si l'un des parametres de la 1ere Activite de la sequence I = Retour de la derniere Activité de la sequence J
					if( i_act.getAction().getParamsTag().get(k).equals(j_act.getAction().getReturnTag()) ) { 
						nb_seq_pred++;
						break; // On sort direct si on en trouve un
					}
				}
			}
			if( nb_seq_pred == 0 ) { // Si il n'y a pas de sequences qui precedent 
				premieres_seq.add(i);
			}
		}
		LDPparallel.Fourche new_fourche = LDPparallel.LDPparallelFactory.eINSTANCE.createFourche();
		
		if(premieres_seq.size() > 1) { // On commence par une fourche
			new_fourche.setPred(new_debut);
			for(int i = 0; i<premieres_seq.size(); i++) { 
				new_fourche.getSucc().add( seq_list.get( premieres_seq.get(i) ) );
			}
			new_debut.setReference(new_fourche);
			porte_list.add(new_fourche);
		}
		
		else { // On commence par une sequence
			new_debut.setReference( seq_list.get( premieres_seq.get(0)) );
			new_fourche.setPred( seq_list.get( premieres_seq.get(0)) );
			porte_list.add(new_fourche);
			
		}
		
		
		//System.out.println(" Sauvegarde du modèle");
		//this.sauverModele(endname, proc);
		
	}
	
	public static void main(String argv[]) {
		Transformer t = new Transformer();
		t.transform("models/Calcul_seq.xmi", "models/Calcul_par.xmi");
	}
	
}
