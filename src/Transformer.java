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
			System.err.println("ERREUR sauvegarde du mod�le : "+e);
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
			System.err.println("ERREUR chargement du mod�le : "+e);
			e.printStackTrace();
		}
		return resource;
	}

	public LDP.Processus getProcessusInModel(String modelFile) {
        Resource resource = chargerModele(modelFile, LDPPackage.eINSTANCE);
        if (resource == null) {
            System.err.println(" Erreur de chargement du mod�le");
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
	
	public boolean isPorteCreated(LDPparallel.Sequence s, List<LDPparallel.Sequence> list_suiv, List<LDPparallel.Porte> list_porte) {
		for(int i=0; i<list_porte.size(); i++) {
			if(list_porte.get(i) instanceof Fourche) {
				Fourche f = (Fourche) list_porte.get(i);
				if(list_suiv.equals( f.getSucc() )) {
					return true;
				}		
			}
			else {
				Jonction j = (Jonction) list_porte.get(i);
				if( j.getSucc().equals( list_suiv.get(0) ) ) {
					return true;
				}
			}
		}
		return false;
	}
	
	public int getPorteIndex(LDPparallel.Sequence s, List<LDPparallel.Sequence> list_suiv, List<LDPparallel.Porte> list_porte){
		for(int i=0; i<list_porte.size(); i++) {
			if(list_porte.get(i) instanceof Fourche) {
				Fourche f = (Fourche) list_porte.get(i);
				if(list_suiv.equals( f.getSucc() )) {
					return i;
				}		
			}
			else {
				Jonction j = (Jonction) list_porte.get(i);
				if( j.getSucc().equals( list_suiv.get(0) ) ) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public void transform(String basename, String endname) {
		
		// Operation de base (x1 + y1 + z1) * (x2 - y2)
		
		System.out.println(" Chargement du mod�le");
		System.out.println(" ");
		
		LDP.Processus proc = LDP.LDPFactory.eINSTANCE.createProcessus();
		proc = this.getProcessusInModel(basename);

		LDPparallel.Processus new_proc = LDPparallel.LDPparallelFactory.eINSTANCE.createProcessus();
		
		List<LDP.Activite> ordered_list = new ArrayList<LDP.Activite>();
		
		// Ranger les activit�s dans l'ordre d'execution
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
			new_seq.setName("Sequence " + i);
			seq_list.add(new_seq);
		}
		
		
		// Mettre les activit�s dans des sequences differentes
		System.out.println(" ");
		System.out.println("Rangement dans les sequences");
		List<Integer> index_precedents = new ArrayList<Integer>();
		for(int i=0; i<ordered_list.size(); i++) { // Parcours des activit�s MIN = 0,  MAX = nombre d'activit�s
			
			int nb_precedents = 0;
			index_precedents.clear();
			
			for(int j=0; j<i; j++) { // Parcours des activit�s d�ja trait�es MIN = 0,  MAX = nombre d'activit�s
					
				for(int k=0; k<ordered_list.get(i).getAction().getParamsTag().size(); k++) {
						
					// Si le tag retour de l'activit� k de la sequence j egal au tag param l de l'activit� en cours
					if( ordered_list.get(j).getAction().getReturnTag().equals( ordered_list.get(i).getAction().getParamsTag().get(k) ) ) {
						nb_precedents++;
						index_precedents.add(j);
					}

				}		
			}
			
			if(nb_precedents == 1) { // Inserer dans la bonne sequence
				for(int s=0; s<i; s++) { // Parcours de toutes les sequences potentiellement non vides
					for(int t=0; t<seq_list.get(s).getActivites().size(); t++) {
						if( memeActivite(seq_list.get(s).getActivites().get(t), copyActivite(ordered_list.get(index_precedents.get(0))) ) ) { // Si l'activit� correspond a l'activit� precedente
							seq_list.get(s).getActivites().add( copyActivite(ordered_list.get(i)) ); // Ajout de la nouvelle activit� dans la sequence
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
		
	
		// Le test pour voir si les activit�s sont biens rang�es
		System.out.println(" ");
		for(int i=0; i<seq_list.size(); i++) {
			if(seq_list.get(i).getActivites().isEmpty()) { // Detruit les sequences vides
				seq_list.remove(i);
			}
			/*else {
				System.out.print("Sequence" + i +" -> ");
				for(int j=0; j<seq_list.get(i).getActivites().size(); j++) {
					System.out.print(seq_list.get(i).getActivites().get(j).getDescription() + ", ");
				}
				System.out.println(" ");
			}*/
			
		}
		
		
		// On fixe la 1ere activit� et on fixe les suivants de chaque sequence
		for(int i=0; i<seq_list.size(); i++) {
			for(int j=0; j<seq_list.get(i).getActivites().size(); j++) {
				if( j == 0 ) { // 1ere activit� d'une sequence
					seq_list.get(i).setPremiereActivite(seq_list.get(i).getActivites().get(j));
					if( seq_list.get(i).getActivites().size() > 1 ) { // Si l'activit� n'est pas seule dans la liste
						seq_list.get(i).getActivites().get(j).setSuivante(seq_list.get(i).getActivites().get(j+1));
						seq_list.get(i).getActivites().get(j).setPrecedente(null); // TODO les jonctions ou Fin
					}
					else { // Si elle est seule dans la liste
						seq_list.get(i).getActivites().get(j).setSuivante(null); // TODO les jonctions ou Fin ( Derniere activit� de la sequence )
						seq_list.get(i).getActivites().get(j).setPrecedente(null); // TODO les fourches ou Fin
					}
				}
				else { // Pas 1ere activit� de la sequence
					if( seq_list.get(i).getActivites().size() > j+1 ) { // Si la taille de la sequence est plus grande que la position de l'activit� dans la sequence
						seq_list.get(i).getActivites().get(j).setSuivante(seq_list.get(i).getActivites().get(j+1)); // Suivante de l'activit� J de la sequence I
					}
					else {
						seq_list.get(i).getActivites().get(j).setSuivante(null); // TODO les jonctions ou Fin ( Derniere activit� de la sequence )
					}
					seq_list.get(i).getActivites().get(j).setPrecedente(seq_list.get(i).getActivites().get(j-1)); // Precedente de l'activit� J de la sequence I 
				}			
			}
		}
		
		
		// Le test pour voir si les activit�s se suivent et se precedent bien
		/*System.out.println(" ");
		for(int i=0; i<seq_list.size(); i++) {
			System.out.println(" <-------- Sequence " + i + " --------> ");
			for(int j=0; j<seq_list.get(i).getActivites().size(); j++) {
				System.out.println(" **** Activit� " + seq_list.get(i).getActivites().get(j).getDescription() +" **** ");
				if( seq_list.get(i).getActivites().get(j).getPrecedente() != null ) {
					System.out.println(" Precedente : " + seq_list.get(i).getActivites().get(j).getPrecedente().getDescription());
				}
				if( seq_list.get(i).getActivites().get(j).getSuivante() != null ) {
					System.out.println(" Suivante : " + seq_list.get(i).getActivites().get(j).getSuivante().getDescription());
				}		
			}
			System.out.println(" ");			
		}*/
		
		
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
			LDPparallel.Activite i_act = seq_list.get(i).getPremiereActivite(); // Premiere activit� de la sequence I
			for(int j=0; j<seq_list.size(); j++) {
				LDPparallel.Activite j_act = seq_list.get(j).getActivites().get( seq_list.get(j).getActivites().size() - 1); // Derniere activit� des sequences J
				for(int k=0; k<i_act.getAction().getParamsTag().size(); k++) {
					// Si l'un des parametres de la 1ere Activite de la sequence I = Retour de la derniere Activit� de la sequence J
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
		
		// Construction de la liste de SeqSuiv
		List<SeqSuiv> seq_suiv = new ArrayList<SeqSuiv>();
		for(int i=0; i<seq_list.size(); i++) {
			seq_suiv.add( new SeqSuiv(seq_list.get(i)) );
			for(int j=0; j<seq_list.size(); j++) {
				if( j != i ) { 
					for(int k=0; k<seq_list.get(j).getPremiereActivite().getAction().getParamsTag().size(); k++) {
						if( seq_list.get(i).getActivites().get( seq_list.get(i).getActivites().size() - 1).getAction().getReturnTag().equals(seq_list.get(j).getPremiereActivite().getAction().getParamsTag().get(k)) ) {
							// Si le tag de retour de la dernire activit� de la Sequence I = l'un des tag parametre de la prmeiere activit� de la sequence J
							seq_suiv.get(i).list_suiv.add( seq_list.get(j) );
						}
					}				
				}
			}
		}
		
		//Affichage SeqSuiv
		for(int i=0; i<seq_suiv.size(); i++) {
			System.out.println("<----- Sequence " + i + " -----> ");
			System.out.println(" *** Activit�s *** ");
			for(int j=0; j<seq_suiv.get(i).seq.getActivites().size(); j++) {
				System.out.println( seq_suiv.get(i).seq.getActivites().get(j).getDescription() );
			}
			for(int j=0; j<seq_suiv.get(i).list_suiv.size(); j++) {
				System.out.println(" *** Sequence suivante *** ");
				for(int k=0; k<seq_suiv.get(i).list_suiv.get(j).getActivites().size(); k++) {
					System.out.println( seq_suiv.get(i).list_suiv.get(j).getActivites().get(k).getDescription() );
				}
			}
			System.out.println(" ");
		}
		
		
		
		//Transfo
		for(int i=0; i<seq_suiv.size(); i++) {
			if( seq_suiv.get(i).list_suiv.size() == 0 ) { // Sequence de fin
				new_fin.setReference(seq_suiv.get(i).seq);
			}
			else if( !isPorteCreated( seq_suiv.get(i).seq, seq_suiv.get(i).list_suiv, porte_list ) ) { // Si la porte n'est pas d�ja cr�e
				if( seq_suiv.get(i).list_suiv.size() > 1) { // Creation Fourche
					LDPparallel.Fourche new_f = LDPparallel.LDPparallelFactory.eINSTANCE.createFourche(); 
					new_f.setPred(seq_suiv.get(i).seq);
					new_f.getSucc().addAll(seq_suiv.get(i).list_suiv);
					porte_list.add(new_f);
				}
				else { // Creation Jonction
					LDPparallel.Jonction new_j = LDPparallel.LDPparallelFactory.eINSTANCE.createJonction();
					new_j.getPred().add(seq_suiv.get(i).seq);
					new_j.setSucc(seq_suiv.get(i).list_suiv.get(0));
					porte_list.add(new_j);
				}
			}
			else { // Modification Porte ( Jonction )
				int index_porte = getPorteIndex( seq_suiv.get(i).seq, seq_suiv.get(i).list_suiv, porte_list );
				Jonction j = ( Jonction ) porte_list.get(index_porte);
				j.getPred().add( seq_suiv.get(i).seq );
				porte_list.set(index_porte, j);
			}
		}
		
		// Test transfo
		System.out.println(" ");
		for(int i=0; i<porte_list.size(); i++) {
			System.out.println("<----- " + porte_list.get(i).getClass().getName() + " ----->");
			if(porte_list.get(i) instanceof Fourche) { // Si c'est une Fourche
				System.out.println("*** Successeurs ***");
				for(int j=0; j< ((Fourche) porte_list.get(i)).getSucc().size(); j++) { // Recuperation des Successeurs
					System.out.println("- Sequence -");
					for(int k=0; k< ((Sequence) ((Fourche) porte_list.get(i)).getSucc().get(j) ).getActivites().size(); k++) {
						System.out.println( ((LDPparallel.Activite) ((Sequence) ((Fourche) porte_list.get(i)).getSucc().get(j) ).getActivites().get(k)).getDescription() );
					}
				}
				System.out.println("*** Predecesseurs ***");
				if( ((Fourche) porte_list.get(i)).getPred() instanceof LDPparallel.Debut ) {
					System.out.println("Debut");
				}
				else {
					System.out.println("- Sequence -");
					for(int k=0; k< ((Sequence) ((Fourche) porte_list.get(i)).getSucc().get(0) ).getActivites().size(); k++) {
						System.out.println( ((LDPparallel.Activite) ((Sequence) ((Fourche) porte_list.get(i)).getSucc().get(0) ).getActivites().get(k)).getDescription() );
					}
				}
			}
			
			else { // Si c'est une Jonction
				System.out.println("*** Successeurs ***");
				System.out.println("- Sequence -");
				for(int k=0; k< ((Sequence) ((Jonction) porte_list.get(i)).getSucc() ).getActivites().size(); k++) {
					System.out.println( ((LDPparallel.Activite) ((Sequence) ((Jonction) porte_list.get(i)).getSucc()).getActivites().get(k)).getDescription() );
				}
				System.out.println("*** Predecesseurs ***");
				for(int j=0; j< ((Jonction) porte_list.get(i)).getPred().size(); j++) { // Recuperation des Successeurs
					System.out.println("- Sequence -");
					for(int k=0; k< ((Sequence) ((Jonction) porte_list.get(i)).getPred().get(j) ).getActivites().size(); k++) {
						System.out.println( ((LDPparallel.Activite) ((Sequence) ((Jonction) porte_list.get(i)).getPred().get(j) ).getActivites().get(k)).getDescription() );
					}
				}
			}
			System.out.println(" ");
		}
		
		new_proc.setDebut(new_debut);
		new_proc.setFin(new_fin);
		new_proc.getSequences().addAll(seq_list);
		new_proc.getPortes().addAll(porte_list);
		
		System.out.println(" Sauvegarde du mod�le");
		this.sauverModele(endname, new_proc);
		
	}
	
	private class SeqSuiv{
		LDPparallel.Sequence seq;
		List<LDPparallel.Sequence> list_suiv = new ArrayList();
		public SeqSuiv(LDPparallel.Sequence s) {
			seq = s;
		}
	}
	
	public static void main(String argv[]) {
		Transformer t = new Transformer();
		t.transform("models/Calcul_seq.xmi", "models/Calcul_par.xmi");
	}
	
}
