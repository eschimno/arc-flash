/* ArcFlash.java

Author: Erick Schimnowski

 */

import java.lang.Object;
import java.io.*;
import java.util.*;

public class ArcFlash extends ArcFunctions{
	
	public static void main(String[] args) {

		//Read in Arc Pro data from file and create lookup tables
		CaseTable[] cases= createLookups(args[1]);
	
		//Read in fault clearing data from Oneliner output file
		String[][] lines= parseInput(args[0]);

		//get user input for how many percents the fault was run at
		int percents= getNumPercents();

		//Create an entry for each line in input file and update the inicident energy for each case
		Line [] entries = updateEnergies(lines, percents, cases);
		
		//Format the results and output to file
		fileOutput(entries);
	}
}