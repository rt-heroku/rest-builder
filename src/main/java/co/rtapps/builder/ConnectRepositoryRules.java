package co.rtapps.builder;

import org.telosys.tools.repository.model.EntityInDbModel;
import org.telosys.tools.repository.rules.RepositoryRules;
import org.telosys.tools.repository.rules.RulesUtils;
import org.telosys.tools.repository.rules.StandardRepositoryRules;

public class ConnectRepositoryRules extends StandardRepositoryRules implements RepositoryRules{
	private final RulesUtils rulesUtils;

	public ConnectRepositoryRules() {
		this.rulesUtils = new ConnectRulesUtils();
	}

	@Override
	public String getEntityClassName(String sTableName) {
		String s = sTableName.replace("__c", "_");
		return this.rulesUtils.camelCase(s, "_");
	}
	
	@Override
	public String getAttributeName(String sColumnName) {
		String s = sColumnName.replace("__c", "_custom");
		String transformed = this.rulesUtils.camelCase(s, "_");

		return this.rulesUtils.uncapitalize(transformed);
	}
	
	public boolean attributeNameAlreadyUsed(String attributeName, EntityInDbModel entity ) {
		return this.rulesUtils.attributeNameAlreadyUsed(attributeName, entity) ;
	}

}
