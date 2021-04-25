package com.gnarwhal.ld48.engine.properties;

import com.gnarwhal.ld48.engine.properties.Properties.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class PropertyReader {
	
	private static int lineNum;
	private static String path;
	
	public static Properties readProperties(String path) {
		Properties props = null;
		try {
			File file = new File(path);
			Scanner scanner = new Scanner(file);
			PropertyReader.path = path;
			lineNum = 0;
			props = readBlock(file.getName(), scanner).data;
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return props;
	}
	
	private static BlockNode readBlock(String name, Scanner scanner) {
		BlockNode props = new BlockNode();
		props.key = name;
		props.data = new Properties(name);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.replaceAll("\\s", "");
			if(line.equals("}"))
				break;
			else if(line.length() < 2 || !line.substring(0, 2).equals("//")){
				String[] pair = line.split(":");
				if (pair.length != 2)
					throw new ImproperFormattingException("Formatting exception on line " + line + " in file '" + path + "!");
				pair[1] = pair[1].replaceAll("\\s", "");
				if (pair[1].equals("{"))
					props.data.add(readBlock(pair[0], scanner));
				else if (pair[1].matches("(\\d+|0x[\\da-f]+)")) {
					IntNode node = new IntNode();
					node.key  = pair[0];
					node.data = Integer.decode(pair[1]);
					props.data.add(node);
				}
				else if (pair[1].matches("(\\d+|0x[\\d0-9]+)(,(\\d+|0x[\\d0-9]+))+")) {
					String[] data = pair[1].split(",");
					int[] ints = new int[data.length];
					for (int i = 0; i < ints.length; ++i)
						ints[i] = Integer.decode(data[i]);
					IntArrayNode node = new IntArrayNode();
					node.key  = pair[0];
					node.data = ints;
					props.data.add(node);
						
				}
				else if (pair[1].matches("\\d+\\.\\d+")) {
					DoubleNode node = new DoubleNode();
					node.key  = pair[0];
					node.data = Double.parseDouble(pair[1]);
					props.data.add(node);
				}
				else if (pair[1].matches("\\d+\\.\\d+(,\\d+\\.\\d+)+")) {
					String[] data = pair[1].split(",");
					double[] doubles = new double[data.length];
					for (int i = 0; i < doubles.length; ++i)
						doubles[i] = Double.parseDouble(data[i]);
					DoubleArrayNode node = new DoubleArrayNode();
					node.key  = pair[0];
					node.data = doubles;
					props.data.add(node);
						
				}
				else {
					StringNode node = new StringNode();
					node.key  = pair[0];
					node.data = pair[1];
					props.data.add(node);
				}
			}
			++lineNum;
		}
		return props;
	}
}
