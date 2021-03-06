/* ArcFunctions.java

Author: Erick Schimnowski

 */

import java.lang.Object;
import java.io.*;
import java.util.*;

class CONSTANT{
	//Constant: the number of cases to run
	public static final int NUM_CASES = 6;
	//Constant : the number of voltage classes
	public static final int NUM_VOLTAGES = 8;
	//Constant: the maximum number of transmission lines to run faults on
	public static final int MAX_LINES = 5000;
}

class Case{
	private double[] energies;
	private int caseID=0;

	public Case(int percents, int caseID){
		this.caseID= caseID;
		energies= new double[percents];
	}
	public void updateEnergy(int voltage, double current, double duration, int index, CaseTable[] cases){
		double flux = cases[caseID].getFlux(voltage, current);
		if(flux == -1 || energies[index] == -1){
			energies[index] = -1;
		}
		else if(flux == -2 || energies[index] == -2){
			energies[index] = -2;
		}
		else{
			energies[index] += (flux * duration);
		}
	}
	//Scans through array of incident energies at each line percent fault was run at and returns the worst case (maximum incident energy for the line)
	public double getMaxEnergy(){
		double max=0;
		for (int i=0; i<energies.length; i++){
			if(max < energies[i]) max=energies[i];
			if(energies[i]==-1) return -1;
			if(energies[i]==-2) return -2;
		}
		return max;
	}
}

class Line{
	private String bus1;
	private String bus2;
	private String brname;
	private int voltage;
	private String cktid;
	private Case[] cases;

	public Line(String[] line, int numcases, int percents){
		bus1= line[0];
		bus2= line[1];
		cktid= line[2];

		System.out.println(line[0]);
		String voltageclass = line[0].substring(line[0].lastIndexOf(" ") + 1, line[0].lastIndexOf("k"));
		System.out.println(voltageclass);
		voltage= (int) (1000 * Double.parseDouble(voltageclass));
		brname= line[3];
		cases= new Case[numcases];
		for(int i=0; i<numcases; i++){
			cases[i]= new Case(percents, i);
		}
	}

	public void printLine()
	{
		System.out.println();
		System.out.println(bus1);
		System.out.println(bus2);
		System.out.println(voltage);
		System.out.println(brname);
		for(int i=0; i<cases.length; i++){
			System.out.println("Incident Energy for case #"+ i + " = " + cases[i].getMaxEnergy());
		}
		System.out.println();
	}

	public String outputLine(){
		String output;
		double energy;

		output = bus1+ "," + bus2+ "," + brname + ",";
		for(int i=0; i<cases.length; i++){
			energy = cases[i].getMaxEnergy();
			if (energy == -1){
				output += "N/A for Voltage,";
			}
			else if (energy == -2){
				output += "Use ArcPro,";
			}
			else{
				output += Double.toString(energy) + ",";
			}
		}
		return output;
	}

	//checks if input line is the same line as object
	public int isSame(String[] line){
		if(bus1.equals(line[0]) && bus2.equals(line[1]) && cktid.equals(line[2])){
			return 1;
		}
		return 0;
	}
	public void updateEnergy(double current, double duration, int index, CaseTable[] cases){
		for(int i=0; i<cases.length; i++){
			this.cases[i].updateEnergy(voltage, current, duration, index, cases);
		}
	}
}

class LookupTable{
	private int voltage;
	public double[] values;
	private int rounding;

	public LookupTable(int voltage, double[] values, int rounding)
	{
		this.voltage=voltage;

		this.values= new double[values.length];
		for (int i=0; i<this.values.length; i++) 
		{
			this.values[i]=values[i];
		}
		this.rounding = rounding;
	}
	public int getVoltage()
	{
		return voltage;
	}
	public int getRounding()
	{
		return rounding;
	}
	public double getFlux(double current)
	{
		System.out.println("Current is= "+current + " amps");
		int rounded;
		if (rounding==1)
		{
			//round current to next kA
			rounded= (int)Math.ceil(current/1000.0);
			// System.out.println("rounded current is= "+rounded*1000 + " amps");
			// System.out.println("voltage is= "+voltage);
			//System.out.println(Arrays.toString(values));

			//Check if above imported ArcPro range
			if(current > 40000) return -2;

			return values[rounded-1];
		}
		else
		{
			//round current to next .5kA
			rounded= (int)Math.ceil(current/1000.0)*2;
			//Check if above imported ArcPro range
			if (current > 20000) return -2;
			return values[rounded-1];
		}
	}
	public void updateTable(LookupTable lookup)
	{
		this.voltage=lookup.voltage;
		this.values= lookup.values;
		this.rounding= lookup.rounding;
	}
	public void printTables()
	{
		System.out.println("Array of voltage=" +voltage);
		System.out.println(Arrays.toString(values));

	}
}

class CaseTable{
	private LookupTable[] lookups;
	private int caseID;
	private int kVs;

	public CaseTable(int caseID){
		lookups = new LookupTable [CONSTANT.NUM_VOLTAGES];
		this.caseID = caseID;
		kVs=0;
	}
	public void addLookupTable(LookupTable lookup){
		//System.out.println("kV added: "+ lookup.getkV());
		//System.out.println(Arrays.toString(lookup.values));
		lookups[kVs] = new LookupTable(lookup.getVoltage(),lookup.values, lookup.getRounding());
		//System.out.println("Updated kv" + kVs);
		//System.out.println(Arrays.toString(lookups[kVs].values));
		//if(kVs==1) System.out.println("previous array: "+ Arrays.toString(lookups[kVs-1].values));
		kVs++;
	}
	public double getFlux(int voltage, double current){
		
		//System.out.println("Searching for KV= "+ kV);
		for(int i=0; i<kVs; i++){
			//System.out.println("KV accessed= "+ lookups[i].getkV());
			if (voltage == lookups[i].getVoltage()){
				// System.out.println("KV Found. Energy= "+ lookups[i].getEnergy(current));
				return lookups[i].getFlux(current);
			}
		}
		System.out.println("voltage does not exist, energy invalid");
		return -1;
	}
	public void printTables(){
		System.out.println("Printing Lookup Tables for case " + caseID);
		for(int i=0; i<kVs; i++){
			lookups[i].printTables();
		}
	}
}


public class ArcFunctions{
	public static int inputlines=0;
	
	//function to read in a table of ArcPro values that give arc energy flux for currents at given voltages and distances
	public static CaseTable[] createLookups(String file){
		double[][] lines=  new double[100][100];
		int count = 0;
		String nextline;

		try {
			Scanner input=  new Scanner(new File(file));
			while(input.hasNextLine()){
				nextline= input.nextLine();
				String[] split= nextline.split(",");
				for(int i=0; i<split.length; i++){	
					if(split[i] == null || split[i].isEmpty()){
						lines[count][i] = 0;
					}
					else{
						//System.out.println(Double.parseDouble(split[i]));
						lines[count][i] = Double.parseDouble(split[i]);
					}	
				}
				count++;
			}
			input.close();

		} catch (FileNotFoundException e) {
			System.out.println("No file \""+file+"\" found!");
		}

		// for (int i=0; i<count; i++)
		// {
		// 	// for (int j=0; j<100; j++) 
		// 	// {
		// 	// 	//System.out.print(lines[i][j]+" ");
		// 	// }
		// 	// //System.out.println();
		// }

		// for (int i=0; i<2; i++)
		// {
		// 	System.out.println(Arrays.toString(lines[i]));
		// }

		//Array of CaseTables
		CaseTable[] cases = new CaseTable[CONSTANT.NUM_CASES];
		int caseCount=0;
		for (int i=0; i<100; i++) {
			if (lines[0][i] != 0.0){
				//create a new case table for the entry
				cases[caseCount] = new CaseTable((int)lines[0][i]);
				double [] values = new double[200];

				
				//Create first lookuptable
				for (int j=0; j<98; j++){
					values[j]= lines[j+2][i+1];	
				}
				int rounding= 1;
				if(lines[0][i]==3.0){
					//Case 3 rounds to nearest 0.5kA
					rounding = 2;
				}
				
				LookupTable temp= new LookupTable((int)lines[1][i], values, rounding);
				//System.out.println(Arrays.toString(temp.values));
				cases[caseCount].addLookupTable(temp);

				for (int j=i+2; j<100; j+=2) {
					//System.out.println(lines[0][j]);

					//Check if new case or end of file
					if(lines[0][j]==0.0 && lines[2][j]!=0.0){
						//Create another lookup table for this case
						for (int k=0; k<98; k++){
							values[k]= lines[k+2][j+1];	
						}
						temp= new LookupTable((int)lines[1][j], values, rounding);
						//System.out.println(Arrays.toString(temp.values));
						// System.out.println("case:" + caseCount);
						cases[caseCount].addLookupTable(temp);
					}
					else{
						// System.out.println("break");
						break;
					}
				}
				caseCount++;
			}	
		}

		// try {
		// 	PrintWriter output = new PrintWriter(new File("output.csv"));
		// 	for(int i=0; i < count-1; i++)
		// 	{
		// 		output.println(Arrays.toString(lines[i]));
		// 	}
		// 	output.println(Arrays.toString(lines[count-1]));
		// 	output.close();
		// } catch (FileNotFoundException e) {
		// 	System.out.println("Error creating \"output.txt\"!");
		// }
		// System.out.println("output.txt created!");

		// for (int i=0; i<CONSTANT.NUM_CASES; i++) 
		// {
		// 	cases[i].printTables();
		// }
		return cases;
	}

	//function to get user input and output incident energy for given conditions
	public static void userInput(CaseTable[] cases){

		Scanner reader = new Scanner(System.in);		
		while(true){
			int caseID = 0;
			int index=0;
			boolean flag= true;
			while(flag){
				System.out.print("Enter case ID: ");
				caseID= reader.nextInt();
				flag = false;
				
				switch (caseID){
					case 1: case 2: case 5:
						index = 0;
						break;
					case 3:
						index = 1;
						break;
					case 4:
						index = 2;
						break;
					case 7:
						index = 3;
						break;
					case 8:
						index = 4;
						break;
					case 9:
						index = 5;
						break;
					default:
						System.out.println("Invalid Case Number");
						flag= true;
						break;
				}

			}
			System.out.print("Enter current in amps: ");
			double current= reader.nextDouble();
			System.out.print("Enter voltage in volts: ");
			int voltage= reader.nextInt();
			System.out.print("Enter duration of fault in cycles: ");
			int cycles= reader.nextInt();

			double energy= cases[index].getFlux(voltage, current);
			if (energy==0) System.out.print("Energy invalid");
			else System.out.printf("Energy for %f amp, %d cycle fault on %d V line in case %d = %f \n", current, cycles, voltage, caseID, energy * cycles/60);
			System.out.println();

		}
	}
	
	//function to read in .csv file from Aspen Oneliner and return an array of lines
	//each line is split on commas
	public static String[][] parseInput(String file){
		String nextline;
		String[][] lines = new String[1000000][1];
		int count = 0;
		try {
			Scanner input=  new Scanner(new File(file));
			while(input.hasNextLine()){
				nextline = input.nextLine();
				lines[count]= nextline.split(",");
				count++;
			}
			input.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("No file \""+file+"\" found!");
		}
		inputlines= count;
		return lines;
	}

	//function to get user input for how many percents the line faults were run at
	public static int getNumPercents(){
		Scanner reader = new Scanner(System.in);
		
		
		int percents=0;
		while(true){
			System.out.print("Enter number of Line percents that the fault clearing summary was run at: ");
			while(!reader.hasNext());
			if(reader.hasNextInt()){
				percents= reader.nextInt();
				if(percents>0) break;
			}
			else{
				reader.nextLine();
				System.out.println("Invalid number");
			}
		}
		
		return percents;
	}

	//function to update each line with the incident energy for each case
	public static Line[]  updateEnergies(String[][] lines, int percents, CaseTable[] cases){
		
		Line [] entries= new Line[CONSTANT.MAX_LINES];
		int count=0;
		int currentpercent=0;
		int index=0;
		
		int start = 0;

		while(true){
			if(lines[start].length != 0){
				System.out.println(Arrays.toString(lines[start]));
				if(lines[start][0].equals("BUS1")){
					break;
				}
			}
			start++;
		}

		for(int i=start+2; i<inputlines; i++){	
			//if line not blank
			if(lines[i].length!=0){
				//if first line already imported
				if(entries[count]!= null){
					
					//if the current line represents an already imported line: update the incident energy
					if(entries[count].isSame(lines[i])==1){
						if (Integer.parseInt(lines[i][5])==currentpercent){
							//update energy for line at current percentage of line
							System.out.println("Updating line for " + currentpercent + " percent");

							double current= Double.parseDouble(lines[i-1][8]);
							double duration = Double.parseDouble(lines[i][7])-Double.parseDouble(lines[i-1][7]); 

							entries[count].updateEnergy(current, duration, index, cases);
						}
						else{
							//update currentpercent to the next percentage the fault was run at
							currentpercent=Integer.parseInt(lines[i][5]);
							System.out.println("Updating line for " + currentpercent + " percentage");
							index++;
						}	
					}
					//if the current line has not yet been imported: import line
					else{
						System.out.println("Creating new line");
						count++;
						entries[count]= new Line(lines[i], CONSTANT.NUM_CASES, percents);
						//reset to first instance of fault on this line
						currentpercent=Integer.parseInt(lines[i][5]);
						index=0;
					}
				}
				//if first line not yet imported
				else{
					System.out.println("Creating first line");
					entries[count] = new Line(lines[i], CONSTANT.NUM_CASES, percents);
					//reset to first instance of fault on this line
					currentpercent=Integer.parseInt(lines[i][5]);
					index=0;
				}
			}
		}

		for(int j=0; j<=count; j++){
			entries[j].printLine();
		}

		return entries;
	}

	//function to output incident energies to file
	public static void fileOutput(Line[] entries){
		try {
			PrintWriter output = new PrintWriter(new File("ArcFlashResults.csv"));
			int i=0;

			output.println(",,,Incident Energy");
			output.println("Bus 1, Bus 2, Line Name, Case 1 2 5, Case 3, Case 4, Case 7, Case 8, Case 9");

			while(entries[i] != null){
				output.println(entries[i].outputLine());
				i++;
			}

			output.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error creating \"output.txt\"!");
		}
		System.out.println("ArcFlashResults.csv created!");
	}
}