package com.unon1100.expiringmap;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by Dean.
 * ExpiringHashMaps use a HashMap to store the references, and use
 *
 * @param <K> Key of map
 * @param <V> Value to be mapped to key
 */
public class ExpiringHashMap <K, V> implements Map<K, V>{
	private final HashMap<K, V> map = new HashMap<>();
	private final HashMap<K, Thread> threads = new HashMap<>();
	private final long expireTime;

	/**
	 * Creates a new ExpiringHashMap
	 *
	 * @param expireTime time in ms until it expires
	 */
	public ExpiringHashMap(long expireTime){
		this.expireTime = expireTime;
	}

	/**
	 * @return the number of keys in the hashmap
	 */
	@Override
	public synchronized int size(){
		return map.size();

	}

	/**
	 * @return if the hashmap is empty
	 */
	@Override
	public synchronized boolean isEmpty(){
		return map.isEmpty();

	}

	/**
	 * @param key the key in which is being searched for
	 * @return whether or not the map has the key registered
	 */
	@Override
	public synchronized boolean containsKey(Object key){
		return map.containsKey(key);

	}

	/**
	 * @param value the value that is being searched for
	 * @return whether or not the map's collection of values contains the value specified
	 */
	@Override
	public synchronized boolean containsValue(Object value){
		return map.containsValue(value);
	}


	/**
	 * @param key the key in which to look up
	 * @return the value associated with the key
	 */
	@Override
	public synchronized V get(Object key){
		return map.get(key);
	}

	/**
	 * puts a value that will expire after the specified time
	 *
	 * @param key   the key that the value will be looked up with
	 * @param value the value that will be associated with the key
	 * @return the value associated with the key before this method was called
	 */
	@Override
	public synchronized V put(K key, V value){
		Thread t = new Thread(){
			@Override
			public void run(){
				try{
					Thread.sleep(expireTime);
					ExpiringHashMap.this.remove(key);
				}catch(InterruptedException ignored){

				}
			}
		};
		Thread previous = threads.put(key, t);
		if(previous != null){
			previous.interrupt();
		}

		t.start();
		return map.put(key, value);

	}

	/**
	 * removes a value early
	 *
	 * @param key the key to be removed from the map
	 * @return the value associated with the key
	 */
	@Override
	public synchronized V remove(Object key){
		Thread prev = threads.remove(key);
		if(prev != null) prev.interrupt();
		return map.remove(key);

	}

	/**
	 * @param m a map that contains the entries that you wish to put into the map
	 */
	@Override
	public synchronized void putAll(Map<? extends K, ? extends V> m){
		for(Entry<? extends K, ? extends V> entry : m.entrySet()){
			this.put(entry.getKey(), entry.getValue());

		}
	}

	/**
	 * Removes all values from the map
	 */
	@Override
	public void clear(){
		Set<Entry<K, V>> entrySet = new HashSet<>(map.entrySet());

		for(Entry<K, V> entry : entrySet){
			this.remove(entry.getKey());
		}

	}

	/**
	 * @return a set containing all of the keys that the map contains
	 */
	@Override
	public synchronized Set<K> keySet(){
		return map.keySet();

	}

	/**
	 * @return returns all of the values that the map contains
	 */
	@Override
	public synchronized Collection<V> values(){
		return map.values();

	}

	/**
	 * @return each key/value association
	 */
	@Override
	public synchronized Set<Entry<K, V>> entrySet(){
		return map.entrySet();

	}

	/**
	 * @param key the key you are looking for
	 * @param defaultValue what is returned if there is no value associated with the key
	 * @return the value associated with the key, unless there isn't one, in which case defaultValue is returned
	 */
	@Override
	public synchronized V getOrDefault(Object key, V defaultValue){
		V val = this.get(key);
		return (val != null) || containsKey(key) ? val : defaultValue;

	}

	/**
	 * @param action the action to be done to each key/value pair
	 */
	@Override
	public synchronized void forEach(BiConsumer<? super K, ? super V> action){
		assert action != null;
		for(Map.Entry<K, V> entry : entrySet()){
			K k;
			V v;
			try{
				k = entry.getKey();
				v = entry.getValue();
			}catch(IllegalStateException ise){
				// this usually means the entry is no longer in the map.
				throw new ConcurrentModificationException(ise);
			}
			action.accept(k, v);
		}

	}

	/**
	 * @param function applies the funtion to each key in the set, replacing the values with what the function returns
	 */
	@Override
	public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> function){
		assert function != null;
		for(Map.Entry<K, V> entry : entrySet()){
			K k;
			V v;
			try{
				k = entry.getKey();
				v = entry.getValue();
			}catch(IllegalStateException ise){
				// this usually means the entry is no longer in the map.
				throw new ConcurrentModificationException(ise);
			}

			// ise thrown from function is not a cme.
			v = function.apply(k, v);

			try{
				entry.setValue(v);
			}catch(IllegalStateException ise){
				// this usually means the entry is no longer in the map.
				throw new ConcurrentModificationException(ise);
			}
		}
	}

	/**
	 * Puts the key/value pair in if the currently value for the key is null or doesn't exist
	 *
	 * @param key the key to be searched
	 * @param value the value to be assigned if the value assigned to the key is null
	 * @return the value that is assigned to the key after this method is run
	 */
	@Override
	public synchronized V putIfAbsent(K key, V value){
		V v = get(key);
		if(v == null){
			v = put(key, value);
		}
		return v;
	}

	/**
	 * removes a key if the provided key/value pair match the one in the map
	 *
	 * @param key the key to be removed
	 * @param value the value that will remove the key if it is the same as the one that is assigned to the key
	 * @return if the value was removed
	 */
	@Override
	public synchronized boolean remove(Object key, Object value){
		Object curValue = get(key);
		if(!Objects.equals(curValue, value) || (curValue == null && !containsKey(key))){
			return false;
		}
		remove(key);
		return true;
	}

	/**
	 * replaces the key's thing if the oldValue parameter matches the value currently associated with the key
	 *
	 * @param key the key to get it's value replaced
	 * @param oldValue if this parameter matches the current value associated with the key, it will be replaced with newValue
	 * @param newValue the value to replace oldValue if oldValue matches the value assigned with the key
	 * @return whether or not the key's value was replaced
	 */
	@Override
	public synchronized boolean replace(K key, V oldValue, V newValue) {
		Object curValue = get(key);
		if (!Objects.equals(curValue, oldValue) ||
				(curValue == null && !containsKey(key))) {
			return false;
		}
		put(key, newValue);
		return true;
	}

	/**
	 * replaces a key that is in the map if the key is currently in the map (note - will not replace null values)
	 *
	 * @param key the key to have it's value replaced
	 * @param value the value which will replace the old one
	 * @return the old value
	 */
	@Override
	public synchronized V replace(K key, V value) {
		V oldValue;
		if (((oldValue = get(key)) != null) || containsKey(key)) {
			oldValue = put(key, value);
		}
		return oldValue;
	}

	/**
	 * runs what is in the mappingFunction with the argument of the key if the value is null or doesn't exist
	 *
	 * @param key The key which will be put into the map
	 * @param mappingFunction the function to be run if the key is absent
	 * @return The old value associated to the key
	 */
	@Override
	public synchronized V computeIfAbsent(K key,
										  Function<? super K, ? extends V> mappingFunction) {
		Objects.requireNonNull(mappingFunction);
		V v;
		if ((v = get(key)) == null) {
			V newValue;
			if ((newValue = mappingFunction.apply(key)) != null) {
				put(key, newValue);
				return newValue;
			}
		}

		return v;
	}

	/**
	 * runs what is in the mappingFunction if the key's value is not null, with the arguments of the key and the old value
	 *
	 * @param key the key to be based on for computing
	 * @param remappingFunction the function to be remapped
	 * @return the new value associated to the key
	 */
	@Override
	public synchronized V computeIfPresent(K key,
										   BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		Objects.requireNonNull(remappingFunction);
		V oldValue;
		if ((oldValue = get(key)) != null) {
			V newValue = remappingFunction.apply(key, oldValue);
			if (newValue != null) {
				put(key, newValue);
				return newValue;
			} else {
				remove(key);
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * runs what is in the mappingFunction with the arguments of the key and the old value
	 *
	 * @param key the key to be based on for computing stuff
	 * @param remappingFunction the function that will remap the key
	 * @return returns the new value associated with the key
	 */
	@Override
	public synchronized V compute(K key,
								  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		Objects.requireNonNull(remappingFunction);
		V oldValue = get(key);

		V newValue = remappingFunction.apply(key, oldValue);
		if (newValue == null) {
			// delete mapping
			if (oldValue != null || containsKey(key)) {
				// something to remove
				remove(key);
				return null;
			} else {
				// nothing to do. Leave things as they were.
				return null;
			}
		} else {
			// add or replace old mapping
			put(key, newValue);
			return newValue;
		}
	}

	/**
	 * Assigns the value parameter to the key if it doesn't exist, otherwise it performs the remappingFunction
	 *
	 * @param key the key to be searched
	 * @param value the value to be set to the key if it doesn't exist
	 * @param remappingFunction the function to perform if there is already a value associated with the key
	 * @return the value that is now associated with the key
	 */
	@Override
	public synchronized V merge(K key, V value,
								BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		Objects.requireNonNull(remappingFunction);
		Objects.requireNonNull(value);
		V oldValue = get(key);
		V newValue = (oldValue == null) ? value :
				remappingFunction.apply(oldValue, value);
		if(newValue == null) {
			remove(key);
		} else {
			put(key, newValue);
		}
		return newValue;
	}
}
