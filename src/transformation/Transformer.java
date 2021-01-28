package transformation;



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

	public ModelBase getModelBase(String modelFile) {
		Resource resource = chargerModele(modelFile, ClassDiagramPackage.eINSTANCE);
		if (resource == null) {
			System.err.println(" Erreur de chargement du modèle");
			return null;
		}
		TreeIterator<EObject> it = resource.getAllContents();
		ModelBase base = null;
		while(it.hasNext()) {
			EObject obj = (EObject) it.next();
			if (obj instanceof ModelBase) {
				base = (ModelBase) obj;
				break;
			}
		}
		return base;
	}
	
	public void transform(String basename, String endname) {
		System.out.println(" Chargement du modèle");
		System.out.println(" ");
		ModelBase model = cdm.getModelBase(basename);
		cdm.display(model);
		
		
		
	
		
		System.out.println(" Sauvegarde du modèle");
		cdm.sauverModele(endname, model);
		
	}
	
	public static void main(String argV[]) {
		Transformer t = new Transformer();
		t.transform("models/Processus-seq.xmi", "models/Processus-par.xmi");
	}
	
}
