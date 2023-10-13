/*
 * Sibilla:  a Java framework designed to support analysis of Collective
 * Adaptive Systems.
 *
 *             Copyright (C) 2020.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package it.unicam.quasylab.sibilla.core.util.datastructures;


import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author loreti
 *
 */
public class Pair<K,V> implements Map.Entry<K,V> {
	
	private final K key;
	private V value;
	
	public Pair( Entry<K,V> entry ) {
		this(entry.getKey(), entry.getValue());
	}
	
	public Pair( K key, V value ) {
		this.key = key;
		this.value = value;
	}

    public static <K, V> Pair<K,V> of(K first, V second) {
		return new Pair<>(first, second);
    }

	public static <K1,K2,V1,V2> Function<Pair<K1,V1>, Pair<K2,V2>> combine(
			Function<? super K1,? extends K2> f1,
			Function<? super V1,? extends V2> f2
	) {
		return p -> Pair.of(f1.apply(p.key), f2.apply(p.value));
	}

	public <K2, V2> Pair<K2,V2> apply(Function<K, K2> f1, Function<V, V2> f2) {
		return new Pair<>(f1.apply(key), f2.apply(value));
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		V current = this.value;
		this.value = value;
		return current;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair<?,?> other = (Pair<?,?>) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			return other.value == null;
		} else return value.equals(other.value);
	}

	@Override
	public String toString() {
		return "[key=" + key + ", value=" + value + "]";
	}

	public <T> Pair<K,T> apply(Function<V,T> f) {
		return Pair.apply(this,f);
	}
	
	public static <K,V,T> Pair<K,T> apply(Map.Entry<K,V> p, Function<V,T> f) {
		return new Pair<>(p.getKey(),f.apply(p.getValue()));
	}
	
	public Pair<K,V> apply( BiFunction<K, V, V> f ) {
		return new Pair<>(key, f.apply(key,value));
	}

    public <T> Pair<K,T> applyToSecond(Function<V, T> function) {
		return new Pair<>(this.key, function.apply(this.value));
    }

	public <T> Pair<T,V> applyToFirst(Function<K, T> function) {
		return new Pair<>(function.apply(this.key), this.value);
	}



}
