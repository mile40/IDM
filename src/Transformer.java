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
import LDPparallel.LDPparallelPackage;

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
        Resource resource = chargerModele(modelFile, LDPparallelPackage.eINSTANCE);
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
	
	
	public void transform(String basename, String endname) {
		System.out.println(" Chargement du modèle");
		System.out.println(" ");
		
		LDP.Processus proc = this.getProcessusInModel(basename);
		boolean seq_activity = false;
		
		LDPparallel.Processus new_proc = LDPparallel.LDPparallelFactory.eINSTANCE.createProcessus();
		
		LDPparallel.Sequence new_seq;
		LDPparallel.Activite new_act;
		
		List<LDPparallel.Sequence> new_list_seq = new ArrayList();
		
		EList<LDP.Activite> list_act = proc.getActivites();
		
		for(int act = 0; act<list_act.size(); act++) {
			
			seq_activity = false;
			
			for(int aux = 0; aux< list_act.size(); aux++) {
				
				EList<String> aux_params = list_act.get(aux).getAction().getParamsTag();
				
				for(int aux_params_index = 0 ; aux_params_index < aux_params.size(); aux_params_index++) {
					
					if( !list_act.get(act).getAction().getReturnTag().equals(aux_params.get(aux_params_index)) ) {
							
						seq_activity = true;
						
					}
					else seq_activity = false;
					
				}
				
				if(seq_activity) {
					
					new_seq = LDPparallel.LDPparallelFactory.eINSTANCE.createSequence();
					List<LDPparallel.Activite> new_list_act = new ArrayList();
					new_act = LDPparallel.LDPparallelFactory.eINSTANCE.createActivite();
					new_act = this.copyActivite(list_act.get(act));
					new_list_act.add(new_act);
					new_seq.getActivites().addAll(new_list_act);
					new_seq.setPremiereActivite(new_act);
					new_list_seq.add(new_seq);
					new_proc.getSequences().addAll(new_list_seq);
					//parallel_activities.add( copyActivity(list_act.get(act)) );
					
				}
				
			}
			
		}
	
		
		
		System.out.println(" Sauvegarde du modèle");
		this.sauverModele(endname, proc);
		
	}
	
	public static void main(String argV[]) {
		Transformer t = new Transformer();
		t.transform("models/Processus-seq.xmi", "models/Processus-par.xmi");
	}
	
}
