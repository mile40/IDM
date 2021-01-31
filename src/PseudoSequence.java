import java.util.List;

import LDPparallel.ElementProcessus;
import LDPparallel.Sequence;

public class PseudoSequence {
	private Sequence seq;
	private List<ElementProcessus> pred;
	private List<ElementProcessus> succ;
	private boolean fini;
	
	public PseudoSequence(List<ElementProcessus> preds,  List<ElementProcessus> succs, Sequence s){
		pred = preds;
		succ = succs;
		seq = s;
		fini = false;
	}
	
	
	public boolean isFinished() {
		return fini;
	}
	
	List<ElementProcessus> getPreds(){
		return this.pred;
	}
	
	
	List<ElementProcessus> getsuccs(){
		return this.succ;
	}
	
}
