package ca.uhn.fhir.jpa.dao.search.sql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class SqlBuilderFactory {

	@Autowired
	private ApplicationContext myApplicationContext;

	public CoordsIndexTable coordsIndexTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(CoordsIndexTable.class, theSearchSqlBuilder);
	}

	public DateIndexTable dateIndexTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(DateIndexTable.class, theSearchSqlBuilder);
	}

	public NumberIndexTable numberIndexTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(NumberIndexTable.class, theSearchSqlBuilder);
	}

	public QuantityIndexTable quantityIndexTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(QuantityIndexTable.class, theSearchSqlBuilder);
	}

	public ReferenceIndexTable referenceIndexTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(ReferenceIndexTable.class, theSearchSqlBuilder);
	}

	public ResourceSqlTable resourceTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(ResourceSqlTable.class, theSearchSqlBuilder);
	}

	public SearchParamPresenceTable searchParamPresenceTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(SearchParamPresenceTable.class, theSearchSqlBuilder);
	}

	public StringIndexTable stringIndexTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(StringIndexTable.class, theSearchSqlBuilder);
	}

	public TokenIndexTable tokenIndexTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(TokenIndexTable.class, theSearchSqlBuilder);
	}

	public UriIndexTable uriIndexTable(SearchSqlBuilder theSearchSqlBuilder) {
		return myApplicationContext.getBean(UriIndexTable.class, theSearchSqlBuilder);
	}

}