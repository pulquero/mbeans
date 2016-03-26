package managency.util;

public class ByteArrayClassLoader extends ClassLoader {
	public ByteArrayClassLoader(ClassLoader parent) {
		super(parent);
	}
	public Class loadClass(String name, byte[] classData) {
		return defineClass(name, classData, 0, classData.length);
	}
}
