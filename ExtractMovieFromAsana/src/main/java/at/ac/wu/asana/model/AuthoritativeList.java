package at.ac.wu.asana.model;

public final class AuthoritativeList {
	
	public static final String ALIGNMENT = "☺ Alignment Roles";
	public static final String ALIGNMENT_ID = "7963718816247";
	
	public static String[] authoritativeList = new String[]{
			"7746376637805",
			"7749914219827",
			"7963718816247",
			"11347525454570",
			"11348115733592",
			"11348115733601",
			"11350833325340",
			"11555199602299",
			"11626921109046",
			"12530878841888",
			"13169100426325",
			"29007443412107",
			"47872397062455",
			"61971534223290",
			"79667185218012",
			"163654573139013",
			"236886514207498",
			"388515769387194",
			"389549960603898",
			"404651189519209",
			"560994092069672",
			"561311958443380",
			"824769296181501",
	"1133031362168396"};

	public static String[] authoritativeListNames = new String[] {
			"☺ Sales Roles",
			"☺ Infrastructure Roles",
			"☺ Alignment Roles",
			"☺ Organisations Roles",
			"☺ Marketplace Roles",
			"☺ Demand Roles",
			"☺ Providers Roles",
			"☺ Smooth Operations Roles",
			"☺Business Intelligence Roles",
			"☺ Go Sales Roles",
			"☺ Rainmakers Roles",
			"☺ Go Customer Roles",
			"☺ Finance Roles",
			"☺ Product Roles",
			"☺ Marketing Roles",
			"☺ Evangelism Roles",
			"☺ Marketplace DE roles",
			"☺ Users Roles",
			"☺ Providers roles",
			"☺ Germany Roles",
			"☺ People Roles",
			"☺ Office Roles",
			"☺ Customer Success Roles",
			"☺ Springest Academy Roles"};
	
	public static int lookup(String currentCircleName) {
		boolean found = false;
		int i = 0;
		for (; i < authoritativeListNames.length; i++) {
			if(currentCircleName.equals(authoritativeListNames[i])) {
				found = true;
				break;
			}
		}
		if(currentCircleName.equals("☺ Smooth Ops Roles"))
			return 7;
		if(found)
			return i;

		return -1;
	}
	
}
