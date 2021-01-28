
public class Calcul {
	
	public Integer addition(Integer val1, Integer val2) {
		return val1+val2;
	}
	
	public Integer multiplication(Integer val1, Integer val2) {
		return val1*val2;
	}
	
	public Integer substraction(Integer val1, Integer val2) {
		return val1-val2;
	}
	
	public Integer division(Integer val1, Integer val2) {
		return val1/val2;
	}
	
	public Integer square(Integer val) {
		return val * val;
	}
	
	public Integer sqrt(Integer val) {
		return (new Double(Math.sqrt(val))).intValue();
	}

	public Integer factorial(Integer val) {
		int res = 1;
		for (int i=1;i<=val;i++) {
			res = res*i;
		}
		return res;
	}
	
	public Integer power(Integer val, Integer puiss) {
		int res = val;
		for (int i=1;i<puiss;i++) {
			res = res*val;
		}
		return res;
	}

	/*public static void main(String argv[]) {
		Calcul calc = new Calcul();
		int n = 6;
		int puiss = 3;
		int x = 100;
		int resFact = calc.factorial(n);
		int resSqrt = calc.sqrt(resFact);
		int res = calc.division(calc.power(resSqrt, puiss), x);
		System.out.println("Résultat : "+res);
		
		
	}*/
	
}
