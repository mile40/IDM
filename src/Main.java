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


import LDPparallel.LDPparallelPackage;
import LDPparallel.*;

public class Main {

	public static void main(String argv[]) {
		HashMap<String, Object> tags = new HashMap<String, Object>();
		tags.put("y1", 10);
		tags.put("x1", 10);
		tags.put("z1", 10);
		tags.put("x2", 4);
		tags.put("y2", 2);
	
		LDPParEngine engine = new LDPParEngine();
		Calcul calc = new Calcul();
		Resource res = engine.chargerModele("models/Calcul_par.xmi", LDPparallelPackage.eINSTANCE);
		Processus model = engine.getPorcessusModel("models/Calcul_par.xmi");
		engine.execute(model, calc , tags);
		System.out.println("Le résultat vaut:" + tags.get("res"));
	}

}
