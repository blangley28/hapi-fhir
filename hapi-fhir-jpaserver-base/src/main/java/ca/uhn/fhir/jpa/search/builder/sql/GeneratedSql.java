package ca.uhn.fhir.jpa.search.builder.sql;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents the SQL generated by this query
 */
public class GeneratedSql {
	private final String mySql;
	private final List<Object> myBindVariables;
	private final boolean myMatchNothing;

	public GeneratedSql(boolean theMatchNothing, String theSql, List<Object> theBindVariables) {
		// FIXME: remove or make this only happen in unit tests
		assert Pattern.compile("=['0-9]").matcher(theSql.replace(" ", "")).find() == false : "Non-bound SQL parameter found: " + theSql;
		assert Pattern.compile("in\\(['0-9]").matcher(theSql.toLowerCase().replace(" ", "")).find() == false : "Non-bound SQL parameter found: " + theSql;

		myMatchNothing = theMatchNothing;
		mySql = theSql;
		myBindVariables = theBindVariables;
	}

	public boolean isMatchNothing() {
		return myMatchNothing;
	}

	public List<Object> getBindVariables() {
		return myBindVariables;
	}

	public String getSql() {
		return mySql;
	}
}
