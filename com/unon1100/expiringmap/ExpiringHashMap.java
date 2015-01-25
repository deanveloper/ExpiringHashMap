package com.unon1100.expiringmap;

import java.util.*;

/**
 * Created by Dean! Wow! :oooo
 *
 * @param <K> Key of map
 * @param <V> Value to be mapped to key
 */
public class ExpiringHashMap <K, V> implements Map<K, V>{
	private HashMap<K, V> map = new HashMap<>();
	private HashMap<K, Thread> threads = new HashMap<>();
	private long expireTime;

	/**
	 * @param expireTime time in ms until it expires
	 */
	public ExpiringHashMap(long expireTime){
		this.expireTime = expireTime;
	}

	@Override
	public int size(){
		return map.size();
	}

	@Override
	public boolean isEmpty(){
		return map.size() == 0;
	}

	@Override
	public boolean containsKey(Object key){
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value){
		return map.containsValue(value);
	}

	@Override
	public V get(Object key){
		return map.get(key);
	}

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
		threads.put(key, t);

		t.start();
		return map.put(key, value);
	}

	@Override
	public V remove(Object key){
		threads.remove(key).interrupt();
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m){
		for(Entry<? extends K, ? extends V> entry : m.entrySet()){
			this.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear(){

		Set<Entry<K, V>> entrySet = new HashSet<>(map.entrySet());

		for(Entry<K, V> entry : entrySet){
			this.remove(entry.getKey());
		}
	}

	@Override
	public Set<K> keySet(){
		return map.keySet();
	}

	@Override
	public Collection<V> values(){
		return map.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet(){
		return map.entrySet();
	}
}
