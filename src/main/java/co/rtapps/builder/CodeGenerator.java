package co.rtapps.builder;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.telosys.tools.commons.ConsoleLogger;
import org.telosys.tools.commons.TelosysToolsException;
import org.telosys.tools.commons.TelosysToolsLogger;
import org.telosys.tools.commons.bundles.BundleStatus;
import org.telosys.tools.commons.bundles.BundlesManager;
import org.telosys.tools.commons.cfg.TelosysToolsCfg;
import org.telosys.tools.commons.cfg.TelosysToolsCfgManager;
import org.telosys.tools.commons.dbcfg.DatabaseConfiguration;
import org.telosys.tools.commons.dbcfg.DatabasesConfigurations;
import org.telosys.tools.commons.dbcfg.DbConfigManager;
import org.telosys.tools.commons.env.EnvironmentManager;
import org.telosys.tools.commons.jdbc.ConnectionManager;
import org.telosys.tools.commons.variables.Variable;
import org.telosys.tools.generator.target.TargetDefinition;
import org.telosys.tools.generator.target.TargetsDefinitions;
import org.telosys.tools.generator.target.TargetsLoader;
import org.telosys.tools.generator.task.GenerationTaskResult;
import org.telosys.tools.generator.task.StandardGenerationTask;
import org.telosys.tools.generic.model.Entity;
import org.telosys.tools.repository.RepositoryGenerator;
import org.telosys.tools.repository.model.AttributeInDbModel;
import org.telosys.tools.repository.model.EntityInDbModel;
import org.telosys.tools.repository.model.LinkInDbModel;
import org.telosys.tools.repository.model.RepositoryModel;
import org.telosys.tools.repository.persistence.PersistenceManager;
import org.telosys.tools.repository.persistence.PersistenceManagerFactory;
import org.telosys.tools.repository.rules.RepositoryRules;

import co.rtapps.builder.model.DatabaseUrl;

public class CodeGenerator {
	private static final String DEBUG = "debug";
	private static final String DIR = "dir";
	private static final String HELP = "help";
	private static final String APP = "app";
	private static final String ENTITY_PACKAGE_NAME = "entity-package-name";
	private static final String DATABASE_URL = "database-url";
	private static final String GITHUB_USER = "github-user";
	private static final String TEMPLATE_NAME = "template-name";
	private static final String PACKAGE_NAME = "package-name";
	private static final String REPOSITORY_FILENAME = "/postgres.dbrep";
	private static final String DEFAULT_GITHUB_USER = "rt-heroku";
	private static TelosysToolsLogger logger;
	private static boolean debug = false;

	private static String appName = "app-name";
	private static String repoName = "template_secure_api_db";
	private static String rootPackage = "com.heroku";
	private static String entityPackage = "com.heroku.entities";
	private static String dir = "";
	private static String databaseUrl = "postgres://qfanqbpxwvtaot:438b7d87af678826f7fb959df71a11ecb1a605131df2e3b7531fd2454f0fbaa7@ec2-107-20-149-243.compute-1.amazonaws.com:5432/ddci0c3jj4j96m";

	public static void main(String[] args) {
		System.out.println("Starting Code Generator!!!");

		try {
			if (evaluateCommandLineArguments(args))
				run();
		} catch (ParseException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected static boolean evaluateCommandLineArguments(String[] args) throws ParseException, Exception {
		CommandLineParser parser = new DefaultParser();
		Options options = createCommandLineOptions();
		CommandLine line = parser.parse(options, args);

		showHelp(options, line);

		validateMandatoryOptionIsPresent(line, options, DATABASE_URL);
		validateMandatoryOptionIsPresent(line, options, APP);

		rootPackage = getOptionOrDefaultValue(line, PACKAGE_NAME, "com.heroku.generated");
		repoName = getOptionOrDefaultValue(line, TEMPLATE_NAME, "template_secure_api_db");
		entityPackage = getOptionOrDefaultValue(line, ENTITY_PACKAGE_NAME, rootPackage + "." + "entities");
		appName = getOptionOrDefaultValue(line, APP, "secure-app");
		dir = getOptionOrDefaultValue(line, DIR, System.getProperty("user.dir"));
		debug = getOptionOrDefaultValue(line, DEBUG, "false").equals("true");
		
		return true;
	}

	private static void showHelp(Options options, CommandLine line) throws ParseException {
		if (line.hasOption(HELP)) {
			printCommandLineHelp(options, HELP);
			throw new ParseException("");
		}
	}

	private static void validateMandatoryOptionIsPresent(CommandLine line, Options options, String option)
			throws ParseException {
		
		if (debug)
			System.out.println("");
			
		if (!line.hasOption(option)) {
			printCommandLineHelp(options, option);
			throw new ParseException(option + " is Mandatory, please refer to -h or --help for more information.");
		}
	}

	private static void printCommandLineHelp(Options options, String option) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("generate", options);
	}

	private static String getOptionOrDefaultValue(CommandLine line, String option, String defaultValue) {
		if (line.hasOption(option)) {
			System.out.println(option + line.getOptionValue(option));
			return line.getOptionValue(option);
		} else
			return defaultValue;
	}

	private static Options createCommandLineOptions() {
		// create the Options
		Options options = new Options();
		options.addOption("p", PACKAGE_NAME, true, "package name to use");
		options.addOption("r", TEMPLATE_NAME, true, "template name");
		options.addOption("g", GITHUB_USER, true, "github user to pull the template from: [rt-heroku is default]");
		options.addOption("D", DATABASE_URL, true, "DATABASE_URL format");
		options.addOption("e", ENTITY_PACKAGE_NAME, true,
				"package name where the entities will be generated [<ROOT_PACKAGE>.entities]");
		options.addOption("a", APP, true, "application name");
		options.addOption("h", HELP, false, "display this help");
		options.addOption("W", DIR, true, "directory to write all files [default .]");
		options.addOption("d", DEBUG, true, "turn debug on");

		return options;
	}

	public static void run() throws SQLException, TelosysToolsException, Exception {
		StringBuffer sb = new StringBuffer();

		System.out.println("Current dir: " + dir);

		EnvironmentManager environmentManager = new EnvironmentManager(dir);

		environmentManager.initEnvironment(sb, null);

		ConnectionManager connectionManager = createConnectionManager();
		DatabaseConfiguration databaseConfiguration = getPostgresConfig(databaseUrl);

		TelosysToolsCfg ttCfg = initializeAndSaveToolsCfg(appName, rootPackage, entityPackage, dir);

		saveDatabaseConfiguration(ttCfg, databaseConfiguration);

		downloadFromGithubAndInstallTemplate(repoName, ttCfg);

		RepositoryModel repositoryModel = generateRepository(ttCfg, connectionManager, databaseConfiguration);
		loadGenerationTargetsAndGenerateFiles(repoName, ttCfg, repositoryModel);

		System.out.println("Process Done!!!");

	}

	private static TelosysToolsCfg initializeAndSaveToolsCfg(String appName, String rootPackage, String entityPackage,
			String dir) throws TelosysToolsException {
		TelosysToolsCfgManager ttCfgManager = new TelosysToolsCfgManager(dir);
		TelosysToolsCfg ttCfg = ttCfgManager.loadTelosysToolsCfg();
		ttCfg.setSpecificVariables(getVariables(appName, rootPackage));
		ttCfg.setEntityPackage(entityPackage);
		ttCfg.setRootPackage(rootPackage);
		ttCfgManager.saveTelosysToolsCfg(ttCfg);
		return ttCfg;
	}

	private static RepositoryModel generateRepository(TelosysToolsCfg ttCfg, ConnectionManager connectionManager,
			DatabaseConfiguration databaseConfiguration) throws TelosysToolsException {
		// Generate repository
		RepositoryModel repositoryModel = generateRepositoryModel(databaseConfiguration, connectionManager);
		// TODO: Need to figure out duplicate names in each entity.

		// Very careful with this!!!! Prints everything!!!
		if (debug)
			printModel(repositoryModel);

		// Save repository
		PersistenceManager pm = PersistenceManagerFactory
				.createPersistenceManager(new File(ttCfg.getModelsFolder() + REPOSITORY_FILENAME));
		pm.save(repositoryModel);
		return repositoryModel;
	}

	private static void saveDatabaseConfiguration(TelosysToolsCfg ttCfg, DatabaseConfiguration databaseConfiguration)
			throws TelosysToolsException {
		DatabasesConfigurations databasesConfigurations = new DatabasesConfigurations();
		databasesConfigurations.storeDatabaseConfiguration(databaseConfiguration);

		DbConfigManager dbConfigManager = new DbConfigManager(new File(ttCfg.getDatabasesDbCfgFileAbsolutePath()));
		dbConfigManager.save(databasesConfigurations);
	}

	private static void loadGenerationTargetsAndGenerateFiles(String repoName, TelosysToolsCfg ttCfg,
			RepositoryModel repositoryModel) throws TelosysToolsException {
		List<String> selectedEntities = new LinkedList<String>();
		for (Entity e : repositoryModel.getEntities())
			selectedEntities.add(e.getClassName());

		// Load targets to execute from TEMPLATE/templates.cfg
		TargetsLoader targetLoader = new TargetsLoader(ttCfg);
		TargetsDefinitions targetDefinitions = targetLoader.loadTargetsDefinitions(repoName);
		List<TargetDefinition> selectedTargets = targetDefinitions.getTemplatesTargets();
		List<TargetDefinition> resourcesTargets = targetDefinitions.getResourcesTargets();

		// Generate code
		StandardGenerationTask generationTask = new StandardGenerationTask(repositoryModel, selectedEntities, repoName,
				selectedTargets, resourcesTargets, ttCfg, getTelosysToolsLogger());

		GenerationTaskResult generationTaskResult = generationTask.launch();

		System.out.println("Nb file(s) generated : " + generationTaskResult.getNumberOfFilesGenerated());
	}

	private static void downloadFromGithubAndInstallTemplate(String repoName, TelosysToolsCfg ttCfg) throws Exception {
		System.out.println("========== Download + Install ");
		BundlesManager bundlesManager = new BundlesManager(ttCfg);
		System.out.println("Downloading bundle '" + repoName + "'...");

		BundleStatus status = bundlesManager.downloadBundle(DEFAULT_GITHUB_USER, repoName);

		System.out.println("Satus message : " + status.getMessage());
		System.out.println("Satus is done ? : " + status.isDone());
		System.out.println("Zip file : " + status.getZipFile());
		if (status.getException() != null) {
			System.out.println("Exception : " + status.getException());
			throw new Exception(status.getException());
		}

		String zipFile = status.getZipFile();
		if (status.isDone() && status.getException() == null) {
			System.out.println("Installing bundle '" + repoName + "' from " + zipFile);
			BundleStatus status2 = bundlesManager.installBundle(zipFile, repoName);
			System.out.println("Satus message : " + status2.getMessage());
			System.out.println("Satus is done ? : " + status2.isDone());
			System.out.println("Exception : " + status2.getException());
			System.out.println("Satus log : ");
			System.out.println(status2.getLog());
		} else
			throw new Exception(status.getException());

	}

	private static List<Variable> getVariables(String appName, String rootPackage) {
		List<Variable> vars = new ArrayList<Variable>();
		Variable pname = new Variable("ProjectVariable.PROJECT_NAME", appName);
		Variable aname = new Variable("ProjectVariable.MAVEN_ARTIFACT_ID", appName);
		Variable gname = new Variable("ProjectVariable.MAVEN_GROUP_ID", rootPackage);

		vars.add(pname);
		vars.add(aname);
		vars.add(gname);
		return vars;
	}

	/**
	 * 
	 * @return
	 */
	protected static List<TargetDefinition> getSpecificTargets() {
		List<TargetDefinition> selectedTargets = new LinkedList<TargetDefinition>();
		selectedTargets.add(new TargetDefinition("Entity Java Bean", "${BEANNAME}.java", "${SRC}/${ENTITY_PKG}",
				"jpa_bean.vm", ""));
		return selectedTargets;
	}

	public static ConnectionManager createConnectionManager() throws TelosysToolsException {
		System.out.println("Creating ConnectionManager ...");
		String libraries[] = { "/aaa/aaa", "/bbb/bbb" };
		return new ConnectionManager(libraries, new ConsoleLogger());
	}

	public static void printDatabaseInfo(Connection conn) throws TelosysToolsException, SQLException {
		DatabaseMetaData dbmd = conn.getMetaData();
		System.out.println("DatabaseMetaData obtained : ");
		System.out.println(" product name    : " + dbmd.getDatabaseProductName());
		System.out.println(" product version : " + dbmd.getDatabaseProductVersion());
		System.out.println(" driver name     : " + dbmd.getDriverName());
		System.out.println(" driver version  : " + dbmd.getDriverVersion());
		conn.close();
	}

	public static DatabaseConfiguration getPostgresConfig(String url) throws URISyntaxException {

		DatabaseUrl dbUrl = new DatabaseUrl(url);

		DatabaseConfiguration dbcfg = new DatabaseConfiguration();
		dbcfg.setDatabaseId(0);
		dbcfg.setDatabaseName("POSTGRESQL");
		dbcfg.setDriverClass("org.postgresql.Driver");
		dbcfg.setJdbcUrl(dbUrl.getJdbcUrl());
		dbcfg.setUser(dbUrl.getUsername());
		dbcfg.setPassword(dbUrl.getPassword());
		dbcfg.setMetadataTableTypes("TABLE");
		dbcfg.setMetadataSchema("salesforce");
		dbcfg.setMetadataTableNamePattern("%");
		String excludePattern = "^[_]\\w*";
		dbcfg.setMetadataTableNameExclude(excludePattern);
		return dbcfg;
	}

	private static RepositoryGenerator getRepositoryGenerator(ConnectionManager connectionManager)
			throws TelosysToolsException {

		RepositoryRules rules = new ConnectRepositoryRules();

		return new RepositoryGenerator(connectionManager, rules, getTelosysToolsLogger());
	}

	private static TelosysToolsLogger getTelosysToolsLogger() {
		if (logger == null)
			logger = new ConsoleLogger();
		return logger;
	}

	private static RepositoryModel generateRepositoryModel(DatabaseConfiguration dbcfg,
			ConnectionManager connectionManager) throws TelosysToolsException {

		System.out.println("Database initialization... ");

		System.out.println("Repository generation... ");
		RepositoryGenerator repositoryGenerator = getRepositoryGenerator(connectionManager);
		RepositoryModel repositoryModel = repositoryGenerator.generate(dbcfg);

		return repositoryModel;
	}

	protected static void printModel(RepositoryModel model) {
		System.out.println("---------------------------------------------------");
		System.out.println("MODEL : ");
		System.out.println("Database ID = " + model.getDatabaseId());

		System.out.println("Number of entities = " + model.getNumberOfEntities());
		String[] entitiesNames = model.getEntitiesNames();
		for (String name : entitiesNames) {
			System.out.println("Entity name = '" + name + "'");
			EntityInDbModel entity = model.getEntityByTableName(name);
			AttributeInDbModel[] columns = entity.getAttributesArray();
			for (AttributeInDbModel c : columns) {
				System.out.println(" . Column : " + c);
			}
			LinkInDbModel[] links = entity.getLinksArray();
			for (LinkInDbModel link : links) {
				System.out.println(" . Link : " + link);
				System.out.println("     Field name = " + link.getFieldName());
				System.out.println("     Foreign Key name = " + link.getForeignKeyName());
				System.out.println("     Join Table name = " + link.getJoinTableName());
			}
		}
		System.out.println("---------------------------------------------------");

	}

	protected static List<String> getEntities(RepositoryModel model) {
		return Arrays.asList(model.getEntitiesNames());
	}

}
