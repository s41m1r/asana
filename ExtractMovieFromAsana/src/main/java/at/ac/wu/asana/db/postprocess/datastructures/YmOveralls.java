package at.ac.wu.asana.db.postprocess.datastructures;

public class YmOveralls {

	public String ym;
	public int births;
	public int deaths;
	public int modifications;

	public String getYm() {
		return ym;
	}

	public void setYm(String ym) {
		this.ym = ym;
	}

	public int getBirths() {
		return births;
	}
	public void setBirths(int births) {
		this.births = births;
	}

	public int getDeaths() {
		return deaths;
	}

	public void setDeaths(int deaths) {
		this.deaths = deaths;
	}

	public int getModifications() {
		return modifications;
	}

	public void setModifications(int modifications) {
		this.modifications = modifications;
	}


	@Override
	public String toString() {
		return "YmOveralls [ym=" + ym + ", births=" + births + ", deaths=" + deaths + ", modifications=" + modifications
				+ "]";
	}
	
	public String[] toCSVRow(String ym) {
		return new String[] {ym, ""+births,""+deaths , ""+modifications};
	}
	
	public static String[] csvHeader() {
		return new String[] {"ym", "births","deaths" , "modifications"};
	}

}
