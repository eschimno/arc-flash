/* ArcFunctions.java

Author: Erick Schimnowski

 */

import java.lang.Object;
import java.io.*;
import java.util.*;

public class ArcFunctions
{
	public static int inputlines=0;
	//function to read in a table of ArcPro values that give arc energy flux for currents at given voltages and distances
	public static CaseTable[] createLookups(String file){
		double[][] lines=  new double[100][100];
		int count = 0;
		String nextline;

		try {
			Scanner input=  new Scanner(new File(file));
			while(input.hasNextLine())
			{
				nextline= input.nextLine();
				String[] split= nextline.split(",");
				for(int i=0; i<split.length; i++)
				{	
					if(split[i] == null || split[i].isEmpty())
					{
						lines[count][i] = 0;
					}
					else
					{
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
		for (int i=0; i<100; i++) 
		{
			if (lines[0][i] != 0.0)
			{
				//create a new case table for the entry
				cases[caseCount] = new CaseTable((int)lines[0][i]);
				double [] values = new double[200];

				
				//Create first lookuptable
				for (int j=0; j<98; j++)
				{
					values[j]= lines[j+2][i+1];	
				}
				int rounding= 1;
				if(lines[0][i]==3.0)
				{
					//Case 3 rounds to nearest 0.5kA
					rounding = 2;
				}
				
				LookupTable temp= new LookupTable((int)lines[1][i], values, rounding);
				//System.out.println(Arrays.toString(temp.values));
				cases[caseCount].addLookupTable(temp);

				for (int j=i+2; j<100; j+=2) 
				{
					//System.out.println(lines[0][j]);

					//Check if new case or end of file
					if(lines[0][j]==0.0 && lines[2][j]!=0.0)
					{
						//Create another lookup table for this case
						for (int k=0; k<98; k++)
						{
							values[k]= lines[k+2][j+1];	
						}
						temp= new LookupTable((int)lines[1][j], values, rounding);
						//System.out.println(Arrays.toString(temp.values));
						// System.out.println("case:" + caseCount);
						cases[caseCount].addLookupTable(temp);
					}
					else
					{
						// System.out.println("break");
						break;
					}
				}
				caseCount++;
			}	
		}

		try {
			PrintWriter output = new PrintWriter(new File("output.csv"));
			for(int i=0; i < count-1; i++)
			{
				output.println(Arrays.toString(lines[i]));
			}
			output.println(Arrays.toString(lines[count-1]));
			output.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error creating \"output.txt\"!");
		}
		System.out.println("output.txt created!");

		// for (int i=0; i<CONSTANT.NUM_CASES; i++) 
		// {
		// 	cases[i].printTables();
		// }
		return cases;
	}

	//function to get user input and output incident energy for given conditions
	public static void userInput(CaseTable[] cases){

		Scanner reader = new Scanner(System.in);		
		while(true)
		{
			int caseID = 0;
			int index=0;
			boolean flag= true;
			while(flag)
			{
				System.out.print("Enter case ID: ");
				caseID= reader.nextInt();
				flag = false;
				
				switch (caseID)
				{
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
			while(input.hasNextLine())
			{
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
		while(true)
		{
			System.out.print("Enter number of Line percents that the fault clearing summary was run at: ");
			while(!reader.hasNext());
			if(reader.hasNextInt())
			{
				percents= reader.nextInt();
				if(percents>0) break;
			}
			else
			{
				reader.nextLine();
				System.out.println("Invalid number");
			}
		}
		
		return percents;
	}

	//function to output incident energies to file
	public static void fileOutput(Line[] entries)
	{
		try {
			PrintWriter output = new PrintWriter(new File("ArcFlashResults.csv"));
			int i=0;

			output.println(",,,Incident Energy");
			output.println("Bus 1, Bus 2, Line Name, Case 1 2 5, Case 3, Case 4, Case 7, Case 8, Case 9");

			while(entries[i] != null)
			{
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