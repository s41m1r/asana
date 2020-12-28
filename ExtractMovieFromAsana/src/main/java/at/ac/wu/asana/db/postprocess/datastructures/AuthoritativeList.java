package at.ac.wu.asana.db.postprocess.datastructures;

public final class AuthoritativeList {
	public static final String[] authoritativeList = new String[]{
			"1183127380297106",
			"7963718816247",
			"824769296181501",
			"11348115733601",
			"163654573139013",
			"47872397062455",
			"404651189519209",
			"29007443412107",
			"12530878841888",
			"1160716728801873",
			"7749914219827",
			"388515769387194",
			"1160287303922155",
			"236886514207498",
			"11348115733592",
			"1162475423722019",
			"561311958443380",
			"11347525454570",
			"560994092069672",
			"61971534223290",
			"11350833325340",
			"389549960603898",
			"13169100426325",
			"7746376637805",
			"11555199602299",
			"1133031362168396",
			"1181577597127617",
			"11626921109046",
			"1158107169298919",
			"79667185218012"
			};

	public static final String[] authoritativeListNames = new String[] {
			"☯ Suite roles",
			"☺ Alignment Roles",
			"☺ Customer Success Roles",
			"☺ Demand Roles",
			"☺ Evangelism Roles",
			"☺ Finance Roles",
			"☺ Germany Roles",
			"☺ Go Customer Roles",
			"☺ Go Sales Roles",
			"☺ Go Users Roles",
			"☺ Infrastructure Roles",
			"☺ Learning Advice Roles",
			"☺ Marketplace (NL/BE/UK/SE/COM) Roles",
			"☺ Marketplace DE roles",
			"☺ Marketplace Roles",
			"☺ Non-Catalogue Bookings roles",
			"☺ Office Roles",
			"☺ Organisations Roles",
			"☺ People Roles",
			"☺ Product Roles",
			"☺ Providers Roles",
			"☺ Providers roles",
			"☺ Rainmakers Roles",
			"☺ Sales Roles",
			"☺ Smooth Operations Roles",
			"☺ Springest Academy Roles",
			"☺ Springest Startups roles",
			"☺Business Intelligence Roles",
			"😎 Shop Roles",
			"🙂 Marketing Roles"
			};

	public static String lookupId(String currentCircleId) {
		boolean found = false;
		int i = 0;
		for (; i < authoritativeList.length; i++) {
			if(currentCircleId.equals(authoritativeList[i])) {
				found = true;
				break;
			}
		}
		
		if(found)
			return authoritativeListNames[i];

		return null;
	}
}
