package example;

import java.util.ServiceLoader;

public class Test {
	public static void main(String[] args) {
		ServiceLoader<Interface> loader = ServiceLoader.load(Interface.class);
		for (Interface c : loader)
			System.out.println("class " + c.getClass().getName() + " returns " + c.getString());
	}
}
