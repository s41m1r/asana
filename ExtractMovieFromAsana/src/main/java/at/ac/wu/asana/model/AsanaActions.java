package at.ac.wu.asana.model;

/**
 * 
 * @author Saimir Bala
 *
 */

public class AsanaActions {
	
	public static final String ADD = "add";
	public static final String ASSIGN = "assign";
	public static final String ATTACH = "attach";
	public static final String CHANGE = "change";
	public static final String MAKE_COMMENT = "comment";
	public static final String COMPLETE = "complete";
	public static final String CREATE = "create";
	public static final String DUPLICATE = "duplicate";
	public static final String INCOMPLETE = "incomplete";
	public static final String LASTMODIFY = "modifylast";
	public static final String LIKE = "like";
	public static final String MOVE = "move";
	public static final String REMOVE = "remove";
	public static final String SET = "set";
	public static final String UNASSIGN = "unassign";
	
	public static final int CHANGE_ACCOUNTABILITY_PURPOSE = 1;
	public static final int ADD_SUB_ROLE = 11;
	public static final int CHANGE_SUB_ROLE = 111;
	public static final int ASSIGN_TO_ACTOR = 2;
	public static final int UNASSIGN_FROM_ACTOR = 3;
	public static final int ADD_TO_CIRCLE = 4;
	public static final int REMOVE_FROM_CIRCLE = 5;
	public static final int COMMENT = 6;
	public static final int DETELE_OR_MARK_COMPLETE = 7;
	public static final int REVIVE_OR_MARK_INCOMPLETE =8;
	public static final int CHANGE_NAME_OF_ROLE = 9;
	public static final int IGNORE_OR_DELETE = 0;
	public static final int UNCLEAR_OR_CONFLICT_WITH_CODEBOOK = 99;
	public static final int DESIGN_ROLE = 12;
	public static final int LAST_MODIFY_ROLE = 13;
	public static final int COMPLETE_ROLE = 14;
	public static final int CREATE_ROLE = 15;
	public static final int CIRCLE_CHANGE = 16;
	
	
	public static String codeToString(int code){
		String res = "NOT_FOUND";
		switch (code) {
		case CHANGE_ACCOUNTABILITY_PURPOSE:
			res = "CHANGE_ACCOUNTABILITY_PURPOSE";
			break;
		case ADD_SUB_ROLE:
			res = "ADD_SUB_ROLE";
			break;
		case CHANGE_SUB_ROLE:
			res = "CHANGE_SUB_ROLE";
			break;
		case ASSIGN_TO_ACTOR:
			res = "ASSIGN_TO_ACTOR";
			break;
		case UNASSIGN_FROM_ACTOR:
			res = "UNASSIGN_FROM_ACTOR";
			break;
		case ADD_TO_CIRCLE:
			res = "ADD_TO_CIRCLE";
			break;
		case REMOVE_FROM_CIRCLE:
			res = "REMOVE_FROM_CIRCLE";
			break;
		case COMMENT:
			res = "COMMENT";
			break;
		case DETELE_OR_MARK_COMPLETE:
			res = "DELETE_OR_MARK_COMPLETE";
			break;
		case REVIVE_OR_MARK_INCOMPLETE:
			res = "REVIVE_OR_MARK_INCOMPLETE";
			break;
		case CHANGE_NAME_OF_ROLE:
			res = "CHANGE_NAME_OF_ROLE";
			break;
		case IGNORE_OR_DELETE:
			res = "IGNORE_OR_DELETE";
			break;
		case CREATE_ROLE:
			res = "CREATE_ROLE";
			break;
		case DESIGN_ROLE:
			res = "DESIGN_ROLE";
			break;
		case COMPLETE_ROLE:
			res = "COMPLETE_ROLE";
			break;
		case LAST_MODIFY_ROLE:
			res = "LAST_MODIFY_ROLE";
			break;		
		case UNCLEAR_OR_CONFLICT_WITH_CODEBOOK:
			res = "UNCLEAR_OR_CONFLICT_WITH_CODEBOOK";
			break;
		case CIRCLE_CHANGE:
			res = "CIRCLE_CHANGE";
			break;
		default:
			break;
		}
		
		return res;
	}

}
