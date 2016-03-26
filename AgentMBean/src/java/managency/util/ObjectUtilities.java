package managency.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;
import javax.management.ObjectName;

public final class ObjectUtilities {
	private ObjectUtilities() {}

	/**
	 * Returns an object of type <code>type</code> holding the value represented by the string <code>str</code>.
	 */
	public static Object valueOf(String str, String type) throws Exception {
		if(type.equals(String.class.getName())) {
			return str;
		} else if(type.equals(Boolean.TYPE.getName()) || type.equals(Boolean.class.getName())) {
			return Boolean.valueOf(str);
		} else if(type.equals(Byte.TYPE.getName()) || type.equals(Byte.class.getName())) {
			return Byte.valueOf(str);
		} else if(type.equals(Character.TYPE.getName()) || type.equals(Character.class.getName())) {
			return new Character(str.charAt(0));
		} else if(type.equals(Integer.TYPE.getName()) || type.equals(Integer.class.getName())) {
			return Integer.valueOf(str);
		} else if(type.equals(Long.TYPE.getName()) || type.equals(Long.class.getName())) {
			return Long.valueOf(str);
		} else if(type.equals(Short.TYPE.getName()) || type.equals(Short.class.getName())) {
			return Short.valueOf(str);
		} else if(type.equals(Float.TYPE.getName()) || type.equals(Float.class.getName())) {
			return Float.valueOf(str);
		} else if(type.equals(Double.TYPE.getName()) || type.equals(Double.class.getName()) || type.equals(Number.class.getName())) {
			return Double.valueOf(str);
		} else if(type.equals(InetAddress.class.getName())) {
			return InetAddress.getByName(str);
		} else if(type.equals(ObjectName.class.getName())) {
			return ObjectName.getInstance(str);
		} else if(type.charAt(0) == '[') {
			String[] strArray = split(str.substring(1, str.length()-1).trim(), ',');
			type = getComponentType(type);
			Object objArray = Array.newInstance(getClassForType(type), strArray.length);
			for(int i=0; i<strArray.length; i++)
				Array.set(objArray, i, valueOf(strArray[i], type));
			return objArray;
		} else {
			Class cls = Class.forName(type);
			Constructor constructor = cls.getConstructor(new Class[] {String.class});
			return constructor.newInstance(new Object[] {str});
		}
	}
	public static String[] split(String str, char separator) {
		if(str == null)
			return null;
		if(str.length() == 0)
			return new String[0];
		List splitList = new ArrayList();
		int startPos = 0;
		int endPos = str.indexOf(separator);
		while(endPos != -1) {
			splitList.add(str.substring(startPos, endPos).trim());
			startPos = endPos+1;
			endPos = str.indexOf(separator, startPos);
		}
		splitList.add(str.substring(startPos).trim());
		return (String[]) splitList.toArray(new String[splitList.size()]);
	}
	/**
	 * Returns a string representation of an object <code>obj</code> of type <code>type</code>.
	 */
	public static String toString(Object obj, String type) {
		if(obj == null) {
			return "null";
		} else if(type.charAt(0) == '[') {
			final int length = Array.getLength(obj);
			StringBuffer buf = new StringBuffer("{");
			if(length > 0) {
				type = getComponentType(type);
				buf.append(toString(Array.get(obj, 0), type));
				for(int i=1; i<length; i++) {
					buf.append(", ").append(toString(Array.get(obj, i), type));
				}
			}
			return buf.append("}").toString();
		} else {
			return obj.toString();
		}
	}
	private static String getComponentType(String type) {
		switch(type.charAt(1)) {
			case 'L' : return type.substring(2, type.length()-2);
			case 'Z' : return Boolean.TYPE.getName();
			case 'B' : return Byte.TYPE.getName();
			case 'C' : return Character.TYPE.getName();
			case 'D' : return Double.TYPE.getName();
			case 'F' : return Float.TYPE.getName();
			case 'I' : return Integer.TYPE.getName();
			case 'J' : return Long.TYPE.getName();
			case 'S' : return Short.TYPE.getName();
			default : return type.substring(1);
		}
	}
	private static Class getClassForType(String type) throws Exception {
		if(type.equals(Boolean.TYPE.getName())) {
			return Boolean.TYPE;
		} else if(type.equals(Byte.TYPE.getName())) {
			return Byte.TYPE;
		} else if(type.equals(Character.TYPE.getName())) {
			return Character.TYPE;
		} else if(type.equals(Integer.TYPE.getName())) {
			return Integer.TYPE;
		} else if(type.equals(Long.TYPE.getName())) {
			return Long.TYPE;
		} else if(type.equals(Short.TYPE.getName())) {
			return Short.TYPE;
		} else if(type.equals(Float.TYPE.getName())) {
			return Float.TYPE;
		} else if(type.equals(Double.TYPE.getName())) {
			return Double.TYPE;
		} else {
			return Class.forName(type);
		}
	}
}
