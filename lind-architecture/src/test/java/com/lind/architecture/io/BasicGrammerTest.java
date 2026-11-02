package com.lind.architecture.io;

import org.junit.jupiter.api.Test;

public class BasicGrammerTest {

	/**
	 * java16版本中的记录类，语法唐
	 */
	public record Person(String name, int age) {

	}

	// public final class Person {
	// private final String name;
	// private final int age;
	//
	// public Person(String name, int age) {
	// this.name = name;
	// this.age = age;
	// }
	//
	// public String name() { return name; }
	// public int age() { return age; }
	//
	// @Override
	// public boolean equals(Object o) { /* 基于字段比较 */ }
	// @Override
	// public int hashCode() { /* 基于字段计算 */ }
	// @Override
	// public String toString() { /* Person[name=..., age=...] */ }
	// }

	@Test
	public void personCreate() {
		Person person = new Person("Alice", 30);
		System.out.println(person.name());
		System.out.println(person.age());
		System.out.println(person.toString());
	}

}
