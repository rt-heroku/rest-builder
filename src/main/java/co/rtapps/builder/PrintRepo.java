package co.rtapps.builder;

import java.net.URISyntaxException;
import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.telosys.tools.commons.ConsoleLogger;
import org.telosys.tools.commons.TelosysToolsException;
import org.telosys.tools.commons.TelosysToolsLogger;
import org.telosys.tools.commons.dbcfg.DatabaseConfiguration;
import org.telosys.tools.commons.jdbc.ConnectionManager;
import org.telosys.tools.repository.RepositoryGenerator;
import org.telosys.tools.repository.model.RepositoryModel;
import org.telosys.tools.repository.persistence.FileInMemory;
import org.telosys.tools.repository.persistence.PersistenceManager;
import org.telosys.tools.repository.persistence.PersistenceManagerFactory;
import org.telosys.tools.repository.rules.RepositoryRules;

import co.rtapps.builder.model.DatabaseUrl;

public class PrintRepo {
	private static final String DEBUG = "debug";
	private static final String HELP = "help";
	private static final String DATABASE_URL = "database-url";
	private static final String DEFAULT_SCHEMA = "salesforce";
	private static final String SCHEMA = "schema";
	private static final String EXCLUDE_PATTERN = "exclude-pattern";
	private static final String INCLUDE_PATTERN = "include-pattern";
	private static final String TABLENAME_PATTERN = "tablename-pattern";
	private static final String TABLE_TYPE = "table-type";
	
	private static TelosysToolsLogger logger;
	private static boolean debug = false;

	private static String databaseUrl = "postgres://qfanqbpxwvtaot:438b7d87af678826f7fb959df71a11ecb1a605131df2e3b7531fd2454f0fbaa7@ec2-107-20-149-243.compute-1.amazonaws.com:5432/ddci0c3jj4j96m";
	private static String schemaName = "salesforce";
	private static String excludePattern = "^[_]\\w*";
	private static String includePattern = "";
	private static String tableNamePattern = "%";
	private static String tableType = "TABLE";
	
	public static void main(String[] args) {
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

		databaseUrl = getOptionOrDefaultValue(line, DATABASE_URL, "");

		debug = getOptionOrDefaultValue(line, DEBUG, "false").equals("true");
		schemaName = getOptionOrDefaultValue(line, SCHEMA, DEFAULT_SCHEMA);
		excludePattern = getOptionOrDefaultValue(line, EXCLUDE_PATTERN, excludePattern);
		includePattern = getOptionOrDefaultValue(line, INCLUDE_PATTERN, includePattern);
		tableNamePattern = getOptionOrDefaultValue(line, TABLENAME_PATTERN, tableNamePattern);
		tableType = getOptionOrDefaultValue(line, TABLE_TYPE, tableType);
		
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
			if (debug) System.out.println(option + line.getOptionValue(option));
			return line.getOptionValue(option);
		} else
			return defaultValue;
	}

	private static Options createCommandLineOptions() {
		// create the Options
		Options options = new Options();
		options.addOption("D", DATABASE_URL, true, "DATABASE_URL format");
		options.addOption("h", HELP, false, "display this help");
		options.addOption("d", DEBUG, true, "turn debug on");
		options.addOption("s", SCHEMA, true, "Schema where the salesforce tables are");
		options.addOption("x", EXCLUDE_PATTERN, true, "Regex Pattern used to EXCLUDE table while searching for names ... i.e everything that starts with UNDERSCORE _hc_err");
		options.addOption("i", INCLUDE_PATTERN, true, "Regex Pattern used to INCLUDE table while searching for names ... i.e everything that starts with UNDERSCORE _hc_err");
		options.addOption("t", TABLENAME_PATTERN, true, "Pattern used to search table names ... i.e %, acc%");
		options.addOption("T", TABLE_TYPE, true, "Type of object [TABLE,VIEW, MATERIALIZED VIEW] - Only one!");

		return options;
	}

	public static void run() throws SQLException, TelosysToolsException, Exception {

		ConnectionManager connectionManager = createConnectionManager();
		DatabaseConfiguration databaseConfiguration = getPostgresConfig(databaseUrl);

		printRepositoryXml(connectionManager, databaseConfiguration);
	}

	private static void printRepositoryXml(ConnectionManager connectionManager,
			DatabaseConfiguration databaseConfiguration) throws TelosysToolsException {

		FileInMemory fileInMemory = new FileInMemory();
		RepositoryModel repositoryModel = generateRepositoryModel(databaseConfiguration, connectionManager);
				
		PersistenceManager pm = PersistenceManagerFactory
				.createPersistenceManager(fileInMemory, getTelosysToolsLogger());
		
		pm.save(repositoryModel);
		System.out.println(fileInMemory.getContentAsString());
	}

	private static ConnectionManager createConnectionManager() throws TelosysToolsException {
		if (debug) System.out.println("Creating ConnectionManager ...");
		String libraries[] = { "/aaa/aaa", "/bbb/bbb" };
		return new ConnectionManager(libraries, new ConsoleLogger());
	}

	private static DatabaseConfiguration getPostgresConfig(String url) throws URISyntaxException {

		DatabaseUrl dbUrl = new DatabaseUrl(url);

		DatabaseConfiguration dbcfg = new DatabaseConfiguration();
		dbcfg.setDatabaseId(0);
		dbcfg.setDatabaseName("POSTGRESQL");
		dbcfg.setDriverClass("org.postgresql.Driver");
		dbcfg.setJdbcUrl(dbUrl.getJdbcUrl());
		dbcfg.setUser(dbUrl.getUsername());
		dbcfg.setPassword(dbUrl.getPassword());
		dbcfg.setMetadataTableTypes("TABLE");
		dbcfg.setMetadataSchema(schemaName);
		dbcfg.setMetadataTableNamePattern("%");
		dbcfg.setMetadataTableNameExclude(excludePattern);
		if (! includePattern.equals(""))
			dbcfg.setMetadataTableNameInclude(includePattern);
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

		if (debug) System.out.println("Repository generation... ");
		RepositoryGenerator repositoryGenerator = getRepositoryGenerator(connectionManager);
		RepositoryModel repositoryModel = repositoryGenerator.generate(dbcfg);

		return repositoryModel;
	}

}
