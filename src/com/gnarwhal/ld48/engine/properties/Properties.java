package com.gnarwhal.ld48.engine.properties;

public class Properties {

	private String name;
	private PropNode head, cur;
	
	public Properties(String name) {
		this.name = new String(name);
	}
	
	public void add(PropNode node) {
		if(head == null) {
			head = node;
			cur = node;
		}
		else {
			cur.next = node;
			cur = cur.next;
		}
	}
	
	private PropNode get(String key) throws UndeclaredPropertyException {
		String[] keys = key.split("\\.");
		PropNode mobile = head;
		while (mobile != null) {
			if(mobile.key.equals(keys[0])) {
				if(keys.length > 1 && mobile instanceof BlockNode)
					return ((BlockNode) mobile).data.get(key.substring(keys[0].length() + 1));
				else
					return mobile;
			}
			mobile = mobile.next;
		}
		throw new UndeclaredPropertyException("Property '" + key + "' in properties '" + name + "' was not found!");
	}

	public String getAsString(String key) throws UndeclaredPropertyException {
		return ((StringNode) get(key)).data;
	}

	public int getAsInt(String key) throws UndeclaredPropertyException {
		return ((IntNode) get(key)).data;
	}

	public int[] getAsIntArray(String key) throws UndeclaredPropertyException {
		PropNode node = get(key);
		if(node instanceof IntNode)
			return new int[] { ((IntNode) node).data };
		return ((IntArrayNode) get(key)).data;
	}

	public double getAsDouble(String key) throws UndeclaredPropertyException {
		PropNode node = get(key);
		if(node instanceof IntNode)
			return (double) ((IntNode) node).data;
		return ((DoubleNode) get(key)).data;
	}
	
	public double[] getAsDoubleArray(String key) throws UndeclaredPropertyException {
		PropNode node = get(key);
		if(node instanceof DoubleNode)
			return new double[] { ((DoubleNode) node).data };
		if(node instanceof IntNode)
			return new double[] { ((IntNode) node).data };
		if(node instanceof IntArrayNode) {
			int[] ints = getAsIntArray(key);
			double[] ret = new double[ints.length];
			for (int i = 0; i < ints.length; ++i)
				ret[i] = ints[i];
			return ret;
		}
		return ((DoubleArrayNode) get(key)).data;
	}
	
	public static class PropNode {
		public String key;
		public PropNode next;
	}
	
	public static class BlockNode extends PropNode {
		public Properties data;
	}
	
	public static class StringNode extends PropNode {
		public String data;
	}
	
	public static class IntNode extends PropNode {
		public int data;
	}
	
	public static class IntArrayNode extends PropNode {
		public int[] data;
	}
	
	public static class DoubleNode extends PropNode {
		public double data;
	}
	
	public static class DoubleArrayNode extends PropNode {
		public double[] data;
	}
}
