package co.rtapps.builder;

public class TestCLIParameters {
	public static void main(String[] args) {

		TestCLIParameters test = new TestCLIParameters();
		
//		test.testHelp();
		
		test.testMandatory();
		
//		test.testAll();
	}
	
	
	void testMandatory(){
		String[] args = new String[]{ "--app=appName --package-name=com.heroku --database-url=url" };
		
		System.out.println("testMandatory -> All args: " + args);
		
		try{
			CodeGenerator.evaluateCommandLineArguments(args);
		}catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
		
	}
	
	void testHelp(){
		String[] args = new String[]{ "--app=myApp " };

		System.out.println("testHelp -> All args: " + args);
		
		try{
			CodeGenerator.evaluateCommandLineArguments(args);
		}catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
		
	}

	void testAll(){
		String[] args = new String[]{ "--app=myApp " };

		System.out.println("testAll -> All args: " + args);
		
		try{
			CodeGenerator.evaluateCommandLineArguments(args);
		}catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
		
	}
}
