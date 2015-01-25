package com.unon1100.expiringmap;

import java.util.*;

/**
 * Created by Dean.
 * ExpiringHashMaps use a HashMap to store the references, and use
 *
 * @param <K> Key of map
 * @param <V> Value to be mapped to key
 */
public class ExpiringHashMap <K, V> implements Map<K, V>{
	private HashMap<K, V> map = new HashMap<>();
	private HashMap<K, Thread> threads = new HashMap<>();
	private long expireTime;

	/**
	 * Creates a new ExpiringHashMap
	 *
	 * @param expireTime time in ms until it expires
	 */
	public ExpiringHashMap(long expireTime){
		this.expireTime = expireTime;
	}

	/**
	 *
	 * @return the number of keys in the hashmap
	 */
	@Override
	public int size(){
		return map.size();
	}

	/**
	 * @return if the hashmap is empty
	 */
	@Override
	public boolean isEmpty(){
		return map.isEmpty();
	}

	/**
	 * @param key the key in which is being searched for
	 * @return whether or not the map has the key registered
	 */
	@Override
	public boolean containsKey(Object key){
		return map.containsKey(key);
	}

	/**
	 * @param value the value that is being searched for
	 * @return whether or not the map's collection of values contains the value specified
	 */
	@Override
	public boolean containsValue(Object value){
		return map.containsValue(value);
	}

	/**
	 * @param key the key in which to look up
	 * @return the value associated with the key
	 */
	@Override
	public V get(Object key){
		return map.get(key);
	}

	/**
	 * puts a value that will expire after the specified time
	 * 
	 * @param key the key that the value will be looked up with
	 * @param value the value that will be associated with the key
	 * @return the value associated with the key before this method was called
	 */
	@Override
	public V put(K key, V value){
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
	public V remove(Object key){
		Thread prev = threads.remove(key);
		if(prev != null) prev.interrupt();
		return map.remove(key);
	}

	/**
	 * @param m a map that contains the entries that you wish to put into the map
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m){
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
	public Set<K> keySet(){
		return map.keySet();
	}

	/**
	 * @return returns all of the values that the map contains
	 */
	@Override
	public Collection<V> values(){
		return map.values();
	}

	/**
	 * @return each key/value association
	 */
	@Override
	public Set<Entry<K, V>> entrySet(){
		return map.entrySet();
	}
}

